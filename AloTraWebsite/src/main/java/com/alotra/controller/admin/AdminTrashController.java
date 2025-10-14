package com.alotra.controller.admin;

import com.alotra.entity.Category;
import com.alotra.entity.NhanVien;
import com.alotra.entity.Product;
import com.alotra.entity.Topping;
import com.alotra.entity.SuKienKhuyenMai;
import com.alotra.repository.CategoryRepository;
import com.alotra.repository.NhanVienRepository;
import com.alotra.repository.ProductRepository;
import com.alotra.repository.ToppingRepository;
import com.alotra.repository.SuKienKhuyenMaiRepository;
import com.alotra.repository.KhuyenMaiSanPhamRepository;
import com.alotra.service.NhanVienService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/trash")
public class AdminTrashController {
    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;
    private final ToppingRepository toppingRepo;
    private final NhanVienRepository nhanVienRepo;
    private final NhanVienService nhanVienService;
    private final SuKienKhuyenMaiRepository promotionRepo;
    private final KhuyenMaiSanPhamRepository promoLinkRepo;

    public AdminTrashController(CategoryRepository categoryRepo,
                                ProductRepository productRepo,
                                ToppingRepository toppingRepo,
                                NhanVienRepository nhanVienRepo,
                                NhanVienService nhanVienService,
                                SuKienKhuyenMaiRepository promotionRepo,
                                KhuyenMaiSanPhamRepository promoLinkRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
        this.toppingRepo = toppingRepo;
        this.nhanVienRepo = nhanVienRepo;
        this.nhanVienService = nhanVienService;
        this.promotionRepo = promotionRepo;
        this.promoLinkRepo = promoLinkRepo;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "Thùng rác");
        model.addAttribute("currentPage", "trash");
        model.addAttribute("categories", categoryRepo.findByDeletedAtIsNotNull());
        model.addAttribute("products", productRepo.findByDeletedAtIsNotNull());
        model.addAttribute("toppings", toppingRepo.findByDeletedAtIsNotNull());
        model.addAttribute("employees", nhanVienRepo.findByDeletedAtIsNotNull());
        model.addAttribute("promotions", promotionRepo.findByDeletedAtIsNotNull());
        return "admin/trash";
    }

    // Restore
    @GetMapping("/categories/{id}/restore")
    public String restoreCategory(@PathVariable Integer id, RedirectAttributes ra) {
        categoryRepo.findById(id).ifPresent(c -> { c.setDeletedAt(null); categoryRepo.save(c); });
        ra.addFlashAttribute("message", "Đã khôi phục danh mục.");
        return "redirect:/admin/trash";
    }

    @GetMapping("/products/{id}/restore")
    public String restoreProduct(@PathVariable Integer id, RedirectAttributes ra) {
        productRepo.findById(id).ifPresent(p -> { p.setDeletedAt(null); productRepo.save(p); });
        ra.addFlashAttribute("message", "Đã khôi phục sản phẩm.");
        return "redirect:/admin/trash";
    }

    @GetMapping("/toppings/{id}/restore")
    public String restoreTopping(@PathVariable Integer id, RedirectAttributes ra) {
        toppingRepo.findById(id).ifPresent(t -> { t.setDeletedAt(null); toppingRepo.save(t); });
        ra.addFlashAttribute("message", "Đã khôi phục topping.");
        return "redirect:/admin/trash";
    }

    @GetMapping("/employees/{id}/restore")
    public String restoreEmployee(@PathVariable Integer id, RedirectAttributes ra) {
        nhanVienService.restoreFromTrash(id);
        ra.addFlashAttribute("message", "Đã khôi phục nhân viên.");
        return "redirect:/admin/trash";
    }

    @GetMapping("/promotions/{id}/restore")
    public String restorePromotion(@PathVariable Integer id, RedirectAttributes ra) {
        promotionRepo.findById(id).ifPresent(p -> { p.setDeletedAt(null); promotionRepo.save(p); });
        ra.addFlashAttribute("message", "Đã khôi phục sự kiện khuyến mãi.");
        return "redirect:/admin/trash";
    }

    // Hard delete (may violate FK) => show friendly error
    @GetMapping("/categories/{id}/delete")
    public String hardDeleteCategory(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            categoryRepo.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa vĩnh viễn danh mục.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì còn dữ liệu tham chiếu (sản phẩm).");
        }
        return "redirect:/admin/trash";
    }

    @GetMapping("/products/{id}/delete")
    public String hardDeleteProduct(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            productRepo.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa vĩnh viễn sản phẩm.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì còn dữ liệu tham chiếu (đơn hàng/khuyến mãi/biến thể).");
        }
        return "redirect:/admin/trash";
    }

    @GetMapping("/toppings/{id}/delete")
    public String hardDeleteTopping(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            toppingRepo.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa vĩnh viễn topping.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì còn dữ liệu tham chiếu.");
        }
        return "redirect:/admin/trash";
    }

    @GetMapping("/employees/{id}/delete")
    public String hardDeleteEmployee(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            nhanVienService.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa vĩnh viễn nhân viên.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì nhân viên đã tham gia xử lý đơn hàng.");
        }
        return "redirect:/admin/trash";
    }

    @GetMapping("/promotions/{id}/delete")
    public String hardDeletePromotion(@PathVariable Integer id, RedirectAttributes ra) {
        var opt = promotionRepo.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy sự kiện khuyến mãi.");
            return "redirect:/admin/trash";
        }
        SuKienKhuyenMai promo = opt.get();
        // Block hard delete when still applied to products
        if (!promoLinkRepo.findByPromotion(promo).isEmpty()) {
            ra.addFlashAttribute("error", "Sự kiện đang áp dụng sản phẩm, không thể xóa.");
            return "redirect:/admin/trash";
        }
        try {
            promotionRepo.deleteById(id);
            ra.addFlashAttribute("message", "Đã xóa vĩnh viễn sự kiện khuyến mãi.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì còn dữ liệu tham chiếu (áp dụng sản phẩm/đơn hàng).");
        }
        return "redirect:/admin/trash";
    }
}