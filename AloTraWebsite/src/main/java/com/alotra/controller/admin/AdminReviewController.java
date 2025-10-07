package com.alotra.controller.admin;

import com.alotra.entity.DanhGia;
import com.alotra.repository.DanhGiaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final DanhGiaRepository reviewRepo;

    public AdminReviewController(DanhGiaRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<DanhGia> items = reviewRepo.findAllOrderByCreatedAtDesc();
        model.addAttribute("items", items);
        model.addAttribute("pageTitle", "Đánh giá sản phẩm");
        model.addAttribute("currentPage", "reviews");
        return "admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        reviewRepo.findById(id).ifPresent(reviewRepo::delete);
        ra.addFlashAttribute("message", "Đã xóa đánh giá #" + id);
        return "redirect:/admin/reviews";
    }
}
