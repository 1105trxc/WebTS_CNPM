package com.alotra.controller.account;

import com.alotra.entity.DonHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.CustomerOrderService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

@Controller
@RequestMapping("/payment")
public class PaymentController {
    private static final String BANK_CODE = "MSB";
    private static final String ACCOUNT_NUMBER = "0869697790";
    private static final int EXPIRY_MINUTES = 30;

    private final DonHangRepository orderRepo;
    private final CustomerOrderService customerOrderService;

    public PaymentController(DonHangRepository orderRepo, CustomerOrderService customerOrderService) {
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
        java.util.Map<Integer, java.util.List<CustomerOrderService.ItemToppingRow>> toppings = new java.util.HashMap<>();
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

    // Webhook endpoint (simulate bank callback). In production, secure with signature/allowlist.
    @PostMapping("/webhook/msb")
    public ResponseEntity<?> msbWebhook(@RequestBody Map<String, Object> payload) {
        // Accept common variants in aggregator payloads
        String account = pickAccount(payload);
        String bankCode = String.valueOf(payload.getOrDefault("bankCode", ""));
        String description = pickDescription(payload);
        BigDecimal amount = parseAmount(payload.get("amount"));
        if (amount == null) amount = parseAmount(payload.get("amt"));

        if (account == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing account"));
        }
        // Compare digits only to tolerate formatting
        String acctDigits = normalizeDigits(account);
        String expectedDigits = normalizeDigits(ACCOUNT_NUMBER);
        if (acctDigits == null || !acctDigits.equals(expectedDigits)) {
            return ResponseEntity.badRequest().body(Map.of("error", "account mismatch"));
        }
        if (description == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "description missing"));
        }
        Integer orderId = extractOrderId(description);
        if (orderId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "cannot extract order id"));
        }
        DonHang order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (!isTransferMethod(order.getPaymentMethod())) {
            return ResponseEntity.badRequest().body(Map.of("error", "not a transfer order"));
        }
        if ("DaThanhToan".equals(order.getPaymentStatus())) {
            return ResponseEntity.ok(Map.of("status", "ALREADY_PAID"));
        }
        if (amount == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount missing"));
        }
        BigDecimal expected = order.getTongThanhToan();
        if (expected == null) expected = BigDecimal.ZERO;
        BigDecimal expVnd = expected.setScale(0, BigDecimal.ROUND_HALF_UP);
        BigDecimal paidVnd = amount.setScale(0, BigDecimal.ROUND_HALF_UP);
        if (paidVnd.compareTo(expVnd) != 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount mismatch", "expected", expVnd, "paid", paidVnd));
        }
        order.setPaymentStatus("DaThanhToan");
        order.setPaidAt(LocalDateTime.now());
        orderRepo.save(order);
        return ResponseEntity.ok(Map.of("status", "OK"));
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

    private Integer extractOrderId(String description) {
        if (description == null) return null;
        Pattern p = Pattern.compile("(?i).*?DH\\s+(\\d+).*"); // case-insensitive
        Matcher m = p.matcher(description);
        if (m.matches()) {
            try { return Integer.valueOf(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String normalizeDigits(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return s.replaceAll("[^0-9]", "");
    }
    private BigDecimal parseAmount(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        String digits = normalizeDigits(v);
        if (digits == null || digits.isBlank()) return null;
        try { return new BigDecimal(digits); } catch (Exception e) { return null; }
    }

    private String pickAccount(Map<String,Object> payload) {
        Object[] keys = { "accountNumber", "toAccount", "toAccountNo", "benAccount", "accountNo" };
        for (Object k : keys) {
            Object val = payload.get(String.valueOf(k));
            if (val != null && !String.valueOf(val).isBlank()) return String.valueOf(val);
        }
        return null;
    }
    private String pickDescription(Map<String,Object> payload) {
        Object[] keys = { "description", "content", "addInfo", "orderInfo", "desc" };
        for (Object k : keys) {
            Object val = payload.get(String.valueOf(k));
            if (val != null && !String.valueOf(val).isBlank()) return String.valueOf(val);
        }
        return null;
    }
}