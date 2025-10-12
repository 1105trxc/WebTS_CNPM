package com.alotra.controller.admin;

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
        // Eagerly fetch Size to avoid lazy loading issues outside view
        List<ProductVariant> list = variantRepository.findByProductFetchingSize(productOpt.get());
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
        // Include sizes so we can define variants right at creation time
        model.addAttribute("sizes", sizeRepository.findAll());
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
        // Eagerly fetch size to prevent LazyInitializationException in template (v.size.name)
        model.addAttribute("variants", variantRepository.findByProductFetchingSize(p));
        model.addAttribute("sizes", sizeRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Product product,
                       @RequestParam("categoryId") Integer categoryId,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       // New: batch variants right from the create form
                       @RequestParam(value = "variantSizeId", required = false) List<Integer> variantSizeIds,
                       @RequestParam(value = "variantPrice", required = false) List<BigDecimal> variantPrices,
                       @RequestParam(value = "variantStatus", required = false) List<Integer> variantStatuses,
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
        // save product first to get ID
        product = productRepository.save(product);

        // If variants are provided in the same form (creation time), persist them now
        if (variantSizeIds != null && !variantSizeIds.isEmpty()) {
            // Ensure parallel lists are aligned; use min size to avoid IndexOutOfBounds
            int n = variantSizeIds.size();
            int pn = variantPrices != null ? variantPrices.size() : 0;
            int sn = variantStatuses != null ? variantStatuses.size() : 0;
            int limit = Math.min(n, Math.min(pn, Math.max(sn, n))); // status optional, default 1
            // Avoid duplicate sizes within the same product
            Set<Integer> seenSizeIds = new HashSet<>();
            for (int i = 0; i < limit; i++) {
                Integer sizeId = variantSizeIds.get(i);
                BigDecimal price = (variantPrices != null && i < variantPrices.size()) ? variantPrices.get(i) : null;
                Integer status = (variantStatuses != null && i < variantStatuses.size()) ? variantStatuses.get(i) : 1;
                if (sizeId == null || price == null) continue;
                if (price.signum() < 0) continue; // skip negative
                if (!seenSizeIds.add(sizeId)) continue; // skip duplicate rows
                ProductVariant v = new ProductVariant();
                v.setProduct(product);
                SizeSanPham sz = new SizeSanPham();
                sz.setId(sizeId);
                v.setSize(sz);
                v.setPrice(price);
                v.setStatus(status != null ? status : 1);
                variantRepository.save(v);
            }
        }
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