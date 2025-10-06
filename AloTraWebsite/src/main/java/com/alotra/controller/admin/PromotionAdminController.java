package com.alotra.controller.admin;

import com.alotra.entity.KhuyenMaiSanPham;
import com.alotra.entity.SuKienKhuyenMai;
import com.alotra.entity.Product;
import com.alotra.service.PromotionService;
import com.alotra.service.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/promotions")
public class PromotionAdminController {
    private final PromotionService promotionService;
    private final CloudinaryService cloudinaryService;

    public PromotionAdminController(PromotionService promotionService, CloudinaryService cloudinaryService) {
        this.promotionService = promotionService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Sự kiện khuyến mãi");
        model.addAttribute("currentPage", "promotions");
        model.addAttribute("items", promotionService.findAll());
        return "admin/promotion-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Thêm sự kiện");
        model.addAttribute("currentPage", "promotions");
        model.addAttribute("item", new SuKienKhuyenMai());
        return "admin/promotion-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        SuKienKhuyenMai item = promotionService.findById(id).orElseThrow();
        model.addAttribute("pageTitle", "Sửa sự kiện");
        model.addAttribute("currentPage", "promotions");
        model.addAttribute("item", item);
        return "admin/promotion-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("item") @Valid SuKienKhuyenMai item,
                       BindingResult result,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       Model model) {
        // basic date validation
        LocalDate s = item.getStartDate();
        LocalDate e = item.getEndDate();
        if (s != null && e != null && e.isBefore(s)) {
            result.rejectValue("endDate", "date.invalid", "Ngày kết thúc phải >= ngày bắt đầu");
        }
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", item.getId() == null ? "Thêm sự kiện" : "Sửa sự kiện");
            model.addAttribute("currentPage", "promotions");
            return "admin/promotion-form";
        }
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String url = cloudinaryService.uploadFile(imageFile);
                item.setImageUrl(url);
            } else if (item.getId() != null) {
                // preserve existing image if not uploading new
                promotionService.findById(item.getId()).ifPresent(old -> item.setImageUrl(old.getImageUrl()));
            }
        } catch (Exception ex) {
            result.rejectValue("imageUrl", "upload.error", "Lỗi tải ảnh: " + ex.getMessage());
            model.addAttribute("pageTitle", item.getId() == null ? "Thêm sự kiện" : "Sửa sự kiện");
            model.addAttribute("currentPage", "promotions");
            return "admin/promotion-form";
        }
        promotionService.save(item);
        return "redirect:/admin/promotions";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        promotionService.deleteById(id);
        return "redirect:/admin/promotions";
    }

    // Assign products to a promotion
    @GetMapping("/{id}/products")
    public String manageProducts(@PathVariable Integer id, Model model) {
        SuKienKhuyenMai promo = promotionService.findById(id).orElseThrow();
        List<KhuyenMaiSanPham> assigned = promotionService.listAssignments(id);
        List<Product> unassigned = promotionService.listUnassignedProducts(id);
        model.addAttribute("pageTitle", "Áp sản phẩm - " + promo.getName());
        model.addAttribute("currentPage", "promotions");
        model.addAttribute("promo", promo);
        model.addAttribute("assigned", assigned);
        model.addAttribute("unassigned", unassigned);
        return "admin/promotion-products";
    }

    @PostMapping("/{id}/products")
    public String addProduct(@PathVariable Integer id,
                             @RequestParam("productId") Integer productId,
                             @RequestParam("percent") Integer percent) {
        promotionService.assignProduct(id, productId, percent);
        return "redirect:/admin/promotions/" + id + "/products";
    }

    @GetMapping("/{id}/products/{productId}/remove")
    public String removeProduct(@PathVariable Integer id, @PathVariable Integer productId) {
        promotionService.unassignProduct(id, productId);
        return "redirect:/admin/promotions/" + id + "/products";
    }
}