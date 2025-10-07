package com.alotra.controller.account;

import com.alotra.entity.KhachHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.ReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountReviewController {
    private final ReviewService reviewService;

    public AccountReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private KhachHang currentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = auth != null ? auth.getPrincipal() : null;
        if (p instanceof KhachHangUserDetails kh) return kh.getKhachHang();
        return null;
    }

    @PostMapping("/orders/{orderId}/review")
    public String create(@PathVariable Integer orderId,
                         @RequestParam("lineId") Integer lineId,
                         @RequestParam("stars") Integer stars,
                         @RequestParam(value = "comment", required = false) String comment,
                         RedirectAttributes ra) {
        try {
            KhachHang kh = currentCustomer();
            if (kh == null) throw new IllegalStateException("Bạn cần đăng nhập");
            reviewService.submitReview(kh, lineId, stars != null ? stars : 5, comment);
            ra.addFlashAttribute("msg", "Đã gửi đánh giá.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/orders/" + orderId;
    }

    @PostMapping("/reviews/{id}/edit")
    public String edit(@PathVariable Integer id,
                       @RequestParam("orderId") Integer orderId,
                       @RequestParam("stars") Integer stars,
                       @RequestParam(value = "comment", required = false) String comment,
                       RedirectAttributes ra) {
        try {
            KhachHang kh = currentCustomer();
            if (kh == null) throw new IllegalStateException("Bạn cần đăng nhập");
            reviewService.updateIfAllowed(kh, id, stars, comment);
            ra.addFlashAttribute("msg", "Đã cập nhật đánh giá.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/orders/" + orderId;
    }

    @PostMapping("/reviews/{id}/delete")
    public String delete(@PathVariable Integer id,
                         @RequestParam("orderId") Integer orderId,
                         RedirectAttributes ra) {
        try {
            KhachHang kh = currentCustomer();
            if (kh == null) throw new IllegalStateException("Bạn cần đăng nhập");
            reviewService.deleteIfAllowed(kh, id);
            ra.addFlashAttribute("msg", "Đã xóa đánh giá.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/orders/" + orderId;
    }
}
