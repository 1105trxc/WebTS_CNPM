package com.alotra.controller.account;

import com.alotra.entity.DonHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.OrderService;
import com.alotra.repository.DonHangRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    private static final String BANK_CODE = "MSB";
    private static final String ACCOUNT_NUMBER = "0869697790";
    private static final int EXPIRY_MINUTES = 30;

    private final DonHangRepository orderRepo;
    private final OrderService customerOrderService;

    public PaymentController(DonHangRepository orderRepo, OrderService customerOrderService) {
        this.orderRepo = orderRepo;
        this.customerOrderService = customerOrderService;
    }

    // Utility: determine if the stored payment method denotes bank transfer
    private boolean isTransferMethod(Object method) {
        if (method == null) return false;
        String m = String.valueOf(method).trim();
        return m.equalsIgnoreCase("ChuyenKhoan") || m.equalsIgnoreCase("Chuyển khoản") || m.equalsIgnoreCase("Chuyen khoan");
    }

    @GetMapping("/{id}")
    public String showPaymentPage(@PathVariable Integer id,
                                  @AuthenticationPrincipal KhachHangUserDetails principal,
                                  Model model) {
        DonHang order = orderRepo.findById(id).orElse(null);
        if (order == null) return "redirect:/cart?error=Đơn hàng không tồn tại";
        if (principal == null || order.getCustomer() == null || !order.getCustomer().getId().equals(principal.getId())) {
            return "redirect:/account/orders";
        }
        if (!isTransferMethod(order.getPaymentMethod())) {
            return "redirect:/account/orders";
        }
        if ("DaThanhToan".equals(order.getPaymentStatus())) {
            return "redirect:/payment/" + id + "/success";
        }
        // Load order presentation rows
        var header = customerOrderService.getOrder(id);
        var items = customerOrderService.listOrderItems(id);
        java.util.Map<Integer, java.util.List<OrderService.ItemToppingRow>> toppings = new java.util.HashMap<>();
        for (var it : items) toppings.put(it.id, customerOrderService.listOrderItemToppings(it.id));

        String addInfo = "ALOTRA DH " + id;
        String qrUrl = buildVietQrUrl(BANK_CODE, ACCOUNT_NUMBER, order.getTongThanhToan().intValue(), addInfo);
        // Expiry timestamp (UTC) for countdown
        LocalDateTime created = order.getCreatedAt();
        LocalDateTime expiry = (created != null ? created : LocalDateTime.now()).plusMinutes(EXPIRY_MINUTES);
        long expiryEpochMillis = expiry.toInstant(ZoneOffset.UTC).toEpochMilli();

        model.addAttribute("order", header);
        model.addAttribute("items", items);
        model.addAttribute("toppings", toppings);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("addInfo", addInfo);
        model.addAttribute("bankCode", BANK_CODE);
        model.addAttribute("accountNumber", ACCOUNT_NUMBER);
        model.addAttribute("expiryEpochMillis", expiryEpochMillis);
        model.addAttribute("expiryMinutes", EXPIRY_MINUTES);
        model.addAttribute("pageTitle", "Thanh toán đơn #" + id);
        return "payment/transfer";
    }

    @GetMapping("/{id}/status")
    @ResponseBody
    public Map<String, Object> getStatus(@PathVariable Integer id,
                                         @AuthenticationPrincipal KhachHangUserDetails principal) {
        Map<String,Object> m = new HashMap<>();
        var opt = orderRepo.findById(id);
        if (opt.isPresent()) {
            DonHang order = opt.get();
            // Auto-cancel if expired and still unpaid (transfer flow)
            if (isTransferMethod(order.getPaymentMethod())
                    && !"DaThanhToan".equals(order.getPaymentStatus())) {
                LocalDateTime expiry = (order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now()).plusMinutes(EXPIRY_MINUTES);
                if (LocalDateTime.now().isAfter(expiry) && !"DaHuy".equals(order.getStatus())) {
                    order.setStatus("DaHuy");
                    orderRepo.save(order);
                }
            }
        }
        String status = opt.map(DonHang::getPaymentStatus).orElse("NA");
        String orderStatus = opt.map(DonHang::getStatus).orElse("NA");
        m.put("paymentStatus", status);
        m.put("orderStatus", orderStatus);
        return m;
    }

    // Admin-only helper for local testing: mark an order as paid (cash or transfer)
    @PostMapping("/{id}/admin/mark-paid")
    public ResponseEntity<?> adminMarkPaid(@PathVariable Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }
        DonHang order = orderRepo.findById(id).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        order.setPaymentStatus("DaThanhToan");
        order.setPaidAt(LocalDateTime.now());
        orderRepo.save(order);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    // New: success page after paid
    @GetMapping("/{id}/success")
    public String success(@PathVariable Integer id,
                          @AuthenticationPrincipal KhachHangUserDetails principal,
                          Model model) {
        DonHang order = orderRepo.findById(id).orElse(null);
        if (order == null) return "redirect:/account/orders";
        if (principal == null || order.getCustomer() == null || !order.getCustomer().getId().equals(principal.getId())) {
            return "redirect:/account/orders";
        }
        model.addAttribute("orderId", id);
        return "payment/success";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id,
                         @AuthenticationPrincipal KhachHangUserDetails principal,
                         RedirectAttributes ra) {
        DonHang order = orderRepo.findById(id).orElse(null);
        if (order == null) {
            ra.addFlashAttribute("error", "Đơn hàng không tồn tại");
            return "redirect:/account/orders";
        }
        if (principal == null || order.getCustomer() == null || !order.getCustomer().getId().equals(principal.getId())) {
            return "redirect:/account/orders";
        }
        if ("DaThanhToan".equals(order.getPaymentStatus())) {
            return "redirect:/payment/" + id + "/success";
        }
        // Only allow cancel when status is exactly ChoXuLy
        if ("ChoXuLy".equals(order.getStatus())) {
            order.setStatus("DaHuy");
            orderRepo.save(order);
            ra.addFlashAttribute("msg", "Đã hủy đơn #" + id);
        } else {
            ra.addFlashAttribute("error", "Chỉ hủy được đơn đang chờ xử lý.");
        }
        return "redirect:/account/orders";
    }

    private String buildVietQrUrl(String bankCode, String accountNumber, int amount, String addInfo) {
        String info = URLEncoder.encode(addInfo, StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/" + bankCode + "-" + accountNumber + "-print.png?amount=" + amount + "&addInfo=" + info;
    }
}