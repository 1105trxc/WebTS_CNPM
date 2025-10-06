package com.alotra.controller.payment;

import com.alotra.entity.DonHang;
import com.alotra.repository.DonHangRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/payment/webhook")
public class PaymentAggregatorWebhookController {

    private final DonHangRepository orderRepo;

    @Value("${payment.webhook.secret:}")
    private String webhookSecret;

    @Value("${payment.settle.account:}")
    private String settleAccount;

    @Value("${payment.webhook.success-keywords:SUCCESS,COMPLETED,PAID}")
    private String successKeywords;

    public PaymentAggregatorWebhookController(DonHangRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    @PostMapping("/aggregator")
    public ResponseEntity<?> handle(@RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
                                    @RequestBody Map<String, Object> payload) {
        // 0) Secret check
        if (!StringUtils.hasText(webhookSecret) || !webhookSecret.equals(secret)) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid secret"));
        }

        // 1) Normalize input
        String account = pick(payload, "accountNumber", "toAccount", "toAccountNo", "benAccount", "accountNo");
        String description = pick(payload, "description", "content", "addInfo", "orderInfo", "desc");
        String orderCode = safeStr(payload.get("orderCode"));
        String status = safeStr(payload.get("status"));
        BigDecimal amount = parseAmount(payload.get("amount"));
        if (amount == null) amount = parseAmount(payload.get("amt"));

        // 2) Validate receiving account (digits only compare)
        if (!matchDigits(account, settleAccount)) {
            return ResponseEntity.badRequest().body(Map.of("error", "account mismatch"));
        }

        // 3) Determine order id from orderCode first, fallback to description
        Integer orderId = extractOrderId(orderCode);
        if (orderId == null) orderId = extractOrderId(description);
        if (orderId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "order id not found in orderCode/description"));
        }

        Optional<DonHang> opt = orderRepo.findById(orderId);
        if (opt.isEmpty()) return ResponseEntity.ok(Map.of("status", "IGNORED_NOT_FOUND"));
        DonHang dh = opt.get();

        // Only proceed for bank transfer orders
        if (!isTransferMethod(dh.getPaymentMethod())) {
            return ResponseEntity.ok(Map.of("status", "IGNORED_NOT_TRANSFER"));
        }

        if ("DaThanhToan".equals(dh.getPaymentStatus())) {
            return ResponseEntity.ok(Map.of("status", "ALREADY_PAID"));
        }

        // 4) Check success status and amount match
        if (!isSuccess(status)) {
            return ResponseEntity.ok(Map.of("status", "IGNORED_NOT_SUCCESS"));
        }
        if (amount == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount missing"));
        }
        BigDecimal expected = dh.getTongThanhToan() == null ? BigDecimal.ZERO : dh.getTongThanhToan();
        if (amount.setScale(0, BigDecimal.ROUND_HALF_UP).compareTo(expected.setScale(0, BigDecimal.ROUND_HALF_UP)) != 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount mismatch", "expected", expected, "paid", amount));
        }

        // 5) Mark as paid
        dh.setPaymentStatus("DaThanhToan");
        dh.setPaidAt(LocalDateTime.now());
        orderRepo.save(dh);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private boolean isTransferMethod(Object method) {
        if (method == null) return false;
        String m = String.valueOf(method).trim();
        return m.equalsIgnoreCase("ChuyenKhoan") || m.equalsIgnoreCase("Chuyển khoản") || m.equalsIgnoreCase("Chuyen khoan");
    }

    private String safeStr(Object o) { return o == null ? null : String.valueOf(o); }

    private String pick(Map<String, Object> p, String... keys) {
        for (String k : keys) {
            Object v = p.get(k);
            if (v != null && !String.valueOf(v).isBlank()) return String.valueOf(v);
        }
        return null;
    }

    private BigDecimal parseAmount(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        String digits = normalizeDigits(String.valueOf(v));
        if (digits.isBlank()) return null;
        try { return new BigDecimal(digits); } catch (Exception e) { return null; }
    }

    private String normalizeDigits(String s) { return s == null ? "" : s.replaceAll("[^0-9]", ""); }

    private boolean matchDigits(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) return false;
        return normalizeDigits(a).equals(normalizeDigits(b));
    }

    private Integer extractOrderId(String s) {
        if (!StringUtils.hasText(s)) return null;
        // First try common numeric id within the string
        var m = java.util.regex.Pattern.compile("(\\d{3,})").matcher(s);
        if (m.find()) {
            try { return Integer.valueOf(m.group(1)); } catch (Exception ignored) {}
        }
        return null;
    }

    private boolean isSuccess(String status) {
        if (!StringUtils.hasText(status)) return false;
        String s = status.trim().toUpperCase(Locale.ROOT);
        for (String kw : successKeywords.split(",")) {
            if (s.contains(kw.trim().toUpperCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
