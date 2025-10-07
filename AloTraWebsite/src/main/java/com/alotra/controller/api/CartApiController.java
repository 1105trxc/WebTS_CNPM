package com.alotra.controller.api;

import com.alotra.entity.KhachHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {
    private final CartService cartService;

    public CartApiController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AddRequest req, Authentication auth) {
        KhachHang kh = getCurrentCustomer(auth);
        if (kh == null) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED"));
        }
        Integer qty = (req.quantity == null || req.quantity <= 0) ? 1 : req.quantity;
        if (req.productId == null && req.variantId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing productId or variantId"));
        }
        if (req.productId != null) {
            cartService.addItem(kh, req.productId, null, qty, null);
        } else {
            cartService.addItemWithOptions(kh, req.variantId, qty, null, null);
        }
        int count = cartService.getItemCount(kh);
        return ResponseEntity.ok(Map.of("ok", true, "count", count));
    }

    @GetMapping("/count")
    public Map<String, Object> count(Authentication auth) {
        KhachHang kh = getCurrentCustomer(auth);
        int count = (kh == null) ? 0 : cartService.getItemCount(kh);
        return Map.of("count", count);
    }

    private KhachHang getCurrentCustomer(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof KhachHangUserDetails khd) {
            return khd.getKhachHang();
        }
        return null;
    }

    public static class AddRequest {
        public Integer productId; // optional if variantId provided
        public Integer variantId; // optional if productId provided
        public Integer quantity;  // default 1
    }
}
