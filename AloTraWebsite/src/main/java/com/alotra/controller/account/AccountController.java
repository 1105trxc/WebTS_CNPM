package com.alotra.controller.account;

import com.alotra.entity.KhachHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.OrderService;
import com.alotra.service.KhachHangService;
import com.alotra.service.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/account") // Tất cả URL sẽ có tiền tố /account
public class AccountController {

    private final OrderService orderService;
    private final KhachHangService khachHangService;
    private final PasswordEncoder passwordEncoder;
    private final ReviewService reviewService; // new

    public AccountController(OrderService orderService,
                             KhachHangService khachHangService,
                             PasswordEncoder passwordEncoder,
                             ReviewService reviewService) {
        this.orderService = orderService;
        this.khachHangService = khachHangService;
        this.passwordEncoder = passwordEncoder;
        this.reviewService = reviewService;
    }

    // Profile - view form
    @GetMapping("/profile")
    public String showProfilePage(@AuthenticationPrincipal KhachHangUserDetails current,
                                  Model model) {
        model.addAttribute("pageTitle", "Thông Tin Tài Khoản");
        KhachHang kh = khachHangService.findById(current.getId());
        ProfileForm form = new ProfileForm();
        form.fullName = kh.getFullName();
        form.email = kh.getEmail();
        form.phone = kh.getPhone();
        model.addAttribute("form", form);
        model.addAttribute("kh", kh);
        return "account/profile"; // Trỏ đến file /templates/account/profile.html
    }

    // Profile - submit updates
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal KhachHangUserDetails current,
                                @ModelAttribute("form") ProfileForm form,
                                BindingResult result,
                                RedirectAttributes ra,
                                Model model) {
        KhachHang kh = khachHangService.findById(current.getId());
        // Validate email unique (exclude self)
        if (form.email != null && !form.email.equalsIgnoreCase(kh.getEmail())) {
            KhachHang byEmail = khachHangService.findByEmail(form.email);
            if (byEmail != null && !byEmail.getId().equals(kh.getId())) {
                result.rejectValue("email", "dup", "Email đã được sử dụng.");
            }
        }
        // Validate phone unique when present
        if (form.phone != null && !form.phone.isBlank()) {
            KhachHang byPhone = khachHangService.findByPhone(form.phone);
            if (byPhone != null && !byPhone.getId().equals(kh.getId())) {
                result.rejectValue("phone", "dup", "Số điện thoại đã được sử dụng.");
            }
        }
        // Password change validation (optional)
        boolean wantChangePwd = form.newPassword != null && !form.newPassword.isBlank();
        if (wantChangePwd) {
            if (form.confirmPassword == null || !form.newPassword.equals(form.confirmPassword)) {
                result.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp.");
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Thông Tin Tài Khoản");
            model.addAttribute("kh", kh);
            return "account/profile";
        }
        // Apply updates
        kh.setFullName(form.fullName);
        kh.setEmail(form.email);
        kh.setPhone(form.phone);
        if (wantChangePwd) {
            kh.setPasswordHash(passwordEncoder.encode(form.newPassword));
        }
        khachHangService.save(kh);
        ra.addFlashAttribute("message", "Cập nhật thông tin thành công.");
        return "redirect:/account/profile";
    }

    // Orders list
    @GetMapping("/orders")
    public String showOrdersPage(@AuthenticationPrincipal KhachHangUserDetails current,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "code", required = false) String code,
                                 @RequestParam(value = "from", required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
                                 @RequestParam(value = "to", required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
                                 Model model) {
        model.addAttribute("pageTitle", "Lịch Sử Đơn Hàng");
        Integer orderId = null;
        if (code != null && !code.isBlank()) {
            try { orderId = Integer.valueOf(code.trim()); } catch (NumberFormatException ignored) {}
        }
        LocalDateTime fromDt = null, toDt = null;
        if (from != null || to != null) {
            fromDt = (from != null) ? from.atStartOfDay() : null;
            toDt = (to != null) ? to.atTime(23,59,59) : LocalDateTime.now();
            if (fromDt != null && toDt != null && toDt.isBefore(fromDt)) {
                LocalDateTime tmp = fromDt; fromDt = toDt; toDt = tmp;
            }
        }
        List<OrderService.OrderRow> list = orderService.listOrdersByCustomer(current.getId(), status, orderId, fromDt, toDt);
        model.addAttribute("items", list);
        model.addAttribute("status", status);
        model.addAttribute("code", code);
        model.addAttribute("from", from != null ? from.toString() : "");
        model.addAttribute("to", to != null ? to.toString() : "");
        return "account/orders"; // Trỏ đến file /templates/account/orders.html
    }

    // Order detail (ensure it belongs to logged-in user)
    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Integer id,
                              @AuthenticationPrincipal KhachHangUserDetails current,
                              RedirectAttributes ra,
                              Model model) {
        var order = orderService.getOrderOfCustomer(id, current.getId());
        if (order == null) {
            ra.addFlashAttribute("error", "Không tìm thấy đơn hàng.");
            return "redirect:/account/orders";
        }
        var items = orderService.listOrderItems(id);
        // Build toppings map for each line item
        Map<Integer, List<OrderService.ItemToppingRow>> toppings = new HashMap<>();
        for (var it : items) {
            toppings.put(it.id, orderService.listOrderItemToppings(it.id));
        }
        // Reviews map: lineId -> review (if any) for current user
        List<Integer> lineIds = items.stream().map(it -> it.id).collect(Collectors.toList());
        Map<Integer, com.alotra.entity.DanhGia> reviewsByLine = reviewService.findExistingByCustomerAndLines(current.getId(), lineIds);
        boolean eligibleForReview = reviewService.isOrderEligibleForReview(order.status, order.paymentStatus);
        // Compute edit-allowed per line
        Map<Integer, Boolean> reviewEditableByLine = new HashMap<>();
        reviewsByLine.forEach((lineId, rv) -> reviewEditableByLine.put(lineId, reviewService.canEdit(rv)));
        model.addAttribute("pageTitle", "Chi tiết đơn #" + id);
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("toppings", toppings);
        model.addAttribute("reviewsByLine", reviewsByLine);
        model.addAttribute("eligibleForReview", eligibleForReview);
        model.addAttribute("reviewEditableByLine", reviewEditableByLine);
        model.addAttribute("editWindowMinutes", com.alotra.service.ReviewService.EDIT_WINDOW.toMinutes());
        return "account/order-detail";
    }

    // Small profile form (DTO)
    public static class ProfileForm {
        @NotBlank
        public String fullName;
        @NotBlank
        @Email
        public String email;
        public String phone;
        public String newPassword;
        public String confirmPassword;

        // getters/setters (Thymeleaf can access public fields directly, but add for safety)
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}