package com.alotra.controller;

import com.alotra.entity.Category;
import com.alotra.entity.Product;
import com.alotra.entity.ProductVariant;
import com.alotra.entity.SizeSanPham;
import com.alotra.repository.CategoryRepository;
import com.alotra.repository.ProductRepository;
import com.alotra.repository.ProductVariantRepository;
import com.alotra.repository.SizeSanPhamRepository;
import com.alotra.service.CloudinaryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SizeSanPhamRepository sizeRepository;
    private final ProductVariantRepository variantRepository;
    private final CloudinaryService cloudinaryService;

    public AdminProductController(ProductRepository productRepository,
                                  CategoryRepository categoryRepository,
                                  SizeSanPhamRepository sizeRepository,
                                  ProductVariantRepository variantRepository,
                                  CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.sizeRepository = sizeRepository;
        this.variantRepository = variantRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public String list(Model model) {
        List<Product> items = productRepository.findByDeletedAtIsNull();
        model.addAttribute("pageTitle", "Sản phẩm");
        model.addAttribute("currentPage", "products");
        model.addAttribute("items", items);
        return "admin/products";
    }

    // Quick view: JSON variants for a product
    @GetMapping("/{id}/variants/json")
    @ResponseBody
    public ResponseEntity<?> getVariantsJson(@PathVariable Integer id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) return ResponseEntity.notFound().build();
        List<ProductVariant> list = variantRepository.findByProduct(productOpt.get());
        List<Map<String, Object>> data = new ArrayList<>();
        for (ProductVariant v : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId());
            m.put("size", v.getSize() != null ? v.getSize().getName() : null);
            m.put("price", v.getPrice());
            m.put("status", v.getStatus());
            data.add(m);
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("pageTitle", "Thêm sản phẩm");
        model.addAttribute("currentPage", "products");
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findByDeletedAtIsNull());
        return "admin/product-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
            return "redirect:/admin/products";
        }
        Product p = opt.get();
        model.addAttribute("pageTitle", "Sửa sản phẩm");
        model.addAttribute("currentPage", "products");
        model.addAttribute("product", p);
        model.addAttribute("categories", categoryRepository.findByDeletedAtIsNull());
        model.addAttribute("variants", variantRepository.findByProduct(p));
        model.addAttribute("sizes", sizeRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       @RequestParam("categoryId") Integer categoryId,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       RedirectAttributes ra) {
        // attach category
        Category cat = new Category();
        cat.setId(categoryId);
        product.setCategory(cat);
        // default active if not provided
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        // if image uploaded => upload to cloudinary
        if (imageFile != null && !imageFile.isEmpty()) {
            String url = cloudinaryService.uploadFile(imageFile);
            product.setImageUrl(url);
        }
        // save
        productRepository.save(product);
        ra.addFlashAttribute("message", "Lưu sản phẩm thành công.");
        return "redirect:/admin/products";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        productRepository.findById(id).ifPresent(p -> {
            p.setDeletedAt(LocalDateTime.now());
            p.setStatus(0);
            productRepository.save(p);
        });
        ra.addFlashAttribute("message", "Đã chuyển sản phẩm vào thùng rác.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/variants")
    public String addVariant(@PathVariable Integer id,
                             @RequestParam("sizeId") Integer sizeId,
                             @RequestParam("price") BigDecimal price,
                             @RequestParam(value = "status", defaultValue = "1") Integer status,
                             RedirectAttributes ra) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
            return "redirect:/admin/products";
        }
        Product p = productOpt.get();
        SizeSanPham size = new SizeSanPham();
        size.setId(sizeId);
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setSize(size);
        v.setPrice(price);
        v.setStatus(status);
        variantRepository.save(v);
        return "redirect:/admin/products/edit/" + id;
    }

    @GetMapping("/{id}/variants/{variantId}/delete")
    public String deleteVariant(@PathVariable Integer id, @PathVariable Integer variantId, RedirectAttributes ra) {
        try {
            variantRepository.deleteById(variantId);
            ra.addFlashAttribute("message", "Đã xóa biến thể.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa biến thể vì đang được tham chiếu trong đơn hàng/giỏ hàng.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Không thể xóa biến thể: " + ex.getMessage());
        }
        return "redirect:/admin/products/edit/" + id;
    }
}