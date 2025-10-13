package com.alotra.controller.admin;

import com.alotra.entity.DanhGia;
import com.alotra.repository.DanhGiaRepository;
import com.alotra.service.EmailService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final DanhGiaRepository reviewRepo;
    private final EmailService emailService; // new

    public AdminReviewController(DanhGiaRepository reviewRepo, EmailService emailService) {
        this.reviewRepo = reviewRepo;
        this.emailService = emailService;
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

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Integer id,
                        @RequestParam("reply") String reply,
                        Authentication auth,
                        RedirectAttributes ra) {
        DanhGia dg = reviewRepo.findById(id).orElse(null);
        if (dg == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đánh giá.");
            return "redirect:/admin/reviews";
        }
        String who = auth != null ? auth.getName() : "admin";
        String content = (reply != null && !reply.isBlank()) ? reply.trim() : null;
        dg.setAdminReply(content);
        dg.setAdminRepliedAt(content != null ? LocalDateTime.now() : null);
        dg.setAdminRepliedBy(content != null ? who : null);
        reviewRepo.save(dg);

        // Send email notification to the reviewer on new/updated reply
        if (content != null) {
            try {
                String to = dg.getCustomer() != null ? dg.getCustomer().getEmail() : null;
                if (to != null && !to.isBlank()) {
                    String subject = "AloTra - Cửa hàng đã phản hồi đánh giá của bạn";
                    String body = "Xin chào,\n\n" +
                            "Cửa hàng đã phản hồi đánh giá của bạn:\n\n" +
                            content + "\n\n" +
                            "Bạn có thể xem chi tiết trong mục Đơn hàng của tôi.";
                    emailService.send(to, subject, body);
                }
            } catch (Exception ignored) { /* avoid failing the UX due to email issues */ }
        }
        ra.addFlashAttribute("message", content == null ? "Đã xóa phản hồi." : "Đã gửi phản hồi.");
        return "redirect:/admin/reviews";
    }
}