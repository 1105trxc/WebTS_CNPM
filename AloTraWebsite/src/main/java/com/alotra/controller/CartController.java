package com.alotra.controller;

import com.alotra.entity.GioHangCT;
import com.alotra.entity.KhachHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(@AuthenticationPrincipal KhachHangUserDetails principal, Model model,
                           @RequestParam(value = "msg", required = false) String msg,
                           @RequestParam(value = "error", required = false) String error) {
        KhachHang kh = principal.getKhachHang();
        List<GioHangCT> items = cartService.listItems(kh);
        model.addAttribute("pageTitle", "Giỏ hàng");
        model.addAttribute("items", items);
        // toppings per item and catalogs for UI
        model.addAttribute("itemToppingsMap", cartService.getToppingsForItems(items));
        model.addAttribute("toppingsCatalog", cartService.listActiveToppings());
        // Build a simple map: itemId -> (toppingId -> qty) for default values
        Map<Integer, Map<Integer,Integer>> qtyMap = new HashMap<>();
        cartService.getToppingsForItems(items).forEach((itemId, list) -> {
            Map<Integer,Integer> inner = new HashMap<>();
            list.forEach(t -> inner.put(t.getTopping().getId(), t.getQuantity()));
            qtyMap.put(itemId, inner);
        });
        model.addAttribute("itemTopQtyMap", qtyMap);
        model.addAttribute("total", cartService.calcTotal(items));
        if (msg != null) model.addAttribute("message", msg);
        if (error != null) model.addAttribute("error", error);
        return "cart/cart";
    }

    // Add by productId or variantId
    @GetMapping("/add")
    public String addFromProduct(@AuthenticationPrincipal KhachHangUserDetails principal,
                                 @RequestParam(value = "productId", required = false) Integer productId,
                                 @RequestParam(value = "variantId", required = false) Integer variantId,
                                 @RequestParam(value = "qty", required = false, defaultValue = "1") Integer qty,
                                 RedirectAttributes ra) {
        KhachHang kh = principal.getKhachHang();
        cartService.addItem(kh, productId, variantId, qty, null);
        ra.addFlashAttribute("msg", "Đã thêm vào giỏ hàng");
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQty(@AuthenticationPrincipal KhachHangUserDetails principal,
                            @RequestParam("itemId") Integer itemId,
                            @RequestParam("qty") Integer qty,
                            RedirectAttributes ra) {
        try {
            cartService.updateQuantity(principal.getKhachHang(), itemId, qty);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @GetMapping("/remove/{id}")
    public String remove(@AuthenticationPrincipal KhachHangUserDetails principal,
                         @PathVariable Integer id,
                         RedirectAttributes ra) {
        try {
            cartService.removeItem(principal.getKhachHang(), id);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/item/{id}/toppings")
    public String updateItemToppings(@AuthenticationPrincipal KhachHangUserDetails principal,
                                     @PathVariable("id") Integer itemId,
                                     @RequestParam MultiValueMap<String, String> params,
                                     RedirectAttributes ra) {
        try {
            Map<Integer,Integer> map = new HashMap<>();
            for (String key : params.keySet()) {
                if (key.startsWith("toppings[") && key.endsWith("]")) {
                    String idStr = key.substring(9, key.length() - 1);
                    try {
                        Integer tid = Integer.valueOf(idStr);
                        Integer q = Integer.valueOf(params.getFirst(key));
                        if (q != null && q > 0) map.put(tid, q);
                    } catch (NumberFormatException ignored) {}
                }
            }
            cartService.updateToppings(principal.getKhachHang(), itemId, map);
            ra.addFlashAttribute("msg", "Đã cập nhật topping.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal KhachHangUserDetails principal,
                           @RequestParam(value = "itemIds", required = false) List<Integer> itemIds,
                           @RequestParam(value = "paymentMethod", required = false, defaultValue = "TienMat") String paymentMethod,
                           RedirectAttributes ra) {
        try {
            var order = cartService.checkout(principal.getKhachHang(), itemIds, paymentMethod);
            if ("ChuyenKhoan".equalsIgnoreCase(paymentMethod)) {
                // Redirect to payment page with QR for bank transfer
                return "redirect:/payment/" + order.getId();
            }
            // Cash-on-delivery: placed immediately, pay at handover
            ra.addFlashAttribute("msg", "Đặt hàng thành công. Mã đơn: " + order.getId());
            return "redirect:/account/orders";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }
}