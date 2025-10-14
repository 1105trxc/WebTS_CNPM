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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/promotions")
public class AdminPromotionController {
    private final PromotionService promotionService;
    private final CloudinaryService cloudinaryService;

    public AdminPromotionController(PromotionService promotionService, CloudinaryService cloudinaryService) {
        this.promotionService = promotionService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Sự kiện khuyến mãi");
        model.addAttribute("currentPage", "promotions");
        // Show only active (not in trash)
        model.addAttribute("items", promotionService.findActive());
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
                       Model model,
                       RedirectAttributes ra) {
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
        boolean isNew = (item.getId() == null);
        promotionService.save(item);
        ra.addFlashAttribute("message", isNew ? "Đã thêm sự kiện khuyến mãi." : "Đã cập nhật sự kiện khuyến mãi.");
        return "redirect:/admin/promotions";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        // Prevent delete when products are still applied
        var promoOpt = promotionService.findById(id);
        if (promoOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy sự kiện khuyến mãi.");
            return "redirect:/admin/promotions";
        }
        SuKienKhuyenMai p = promoOpt.get();
        if (promotionService.listAssignments(id).size() > 0) {
            ra.addFlashAttribute("error", "Sự kiện đang áp dụng sản phẩm, không thể xóa. Vui lòng gỡ áp dụng trước.");
            return "redirect:/admin/promotions";
        }
        // Soft-delete: move to trash
        p.setDeletedAt(java.time.LocalDateTime.now());
        promotionService.save(p);
        ra.addFlashAttribute("message", "Đã chuyển sự kiện vào thùng rác.");
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
                             @RequestParam("percent") Integer percent,
                             RedirectAttributes ra) {
        try {
            promotionService.assignProduct(id, productId, percent);
            ra.addFlashAttribute("message", "Đã áp dụng sản phẩm vào sự kiện.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/promotions/" + id + "/products";
    }

    @GetMapping("/{id}/products/{productId}/remove")
    public String removeProduct(@PathVariable Integer id, @PathVariable Integer productId, RedirectAttributes ra) {
        promotionService.unassignProduct(id, productId);
        ra.addFlashAttribute("message", "Đã gỡ sản phẩm khỏi sự kiện.");
        return "redirect:/admin/promotions/" + id + "/products";
    }
}