package com.alotra.controller;

import com.alotra.entity.GioHangCT;
import com.alotra.entity.KhachHang;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {
    private final CartService cartService;

    public CheckoutController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/confirm")
    public String confirm(@AuthenticationPrincipal KhachHangUserDetails principal,
                          @RequestParam(value = "itemIds", required = false) List<Integer> itemIds,
                          @RequestParam(value = "paymentMethod", defaultValue = "TienMat") String paymentMethod,
                          Model model,
                          RedirectAttributes ra) {
        if (itemIds == null || itemIds.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 sản phẩm để đặt hàng");
            return "redirect:/cart";
        }
        KhachHang kh = principal.getKhachHang();
        // Load user's cart items and filter by selected IDs
        List<GioHangCT> all = cartService.listItems(kh);
        List<GioHangCT> sel = all.stream().filter(it -> itemIds.contains(it.getId())).toList();
        if (sel.isEmpty()) {
            ra.addFlashAttribute("error", "Không có sản phẩm hợp lệ để đặt hàng");
            return "redirect:/cart";
        }
        model.addAttribute("pageTitle", "Xác nhận đặt hàng");
        model.addAttribute("items", sel);
        model.addAttribute("itemToppingsMap", cartService.getToppingsForItems(sel));
        model.addAttribute("total", cartService.calcTotal(sel));
        model.addAttribute("paymentMethod", paymentMethod);
        model.addAttribute("itemIds", itemIds);
        return "checkout/confirm";
    }

    @PostMapping("/place")
    public String place(@AuthenticationPrincipal KhachHangUserDetails principal,
                        @RequestParam("itemIds") List<Integer> itemIds,
                        @RequestParam("paymentMethod") String paymentMethod,
                        @RequestParam(value = "receivingMethod", defaultValue = "Ship") String receivingMethod,
                        @RequestParam(value = "note", required = false) String note,
                        @RequestParam(value = "shipName", required = false) String shipName,
                        @RequestParam(value = "shipPhone", required = false) String shipPhone,
                        @RequestParam(value = "shipAddress", required = false) String shipAddress,
                        RedirectAttributes ra) {
        try {
            var order = cartService.checkoutWithOptions(
                    principal.getKhachHang(), itemIds, paymentMethod, note, receivingMethod, shipName, shipPhone, shipAddress
            );
            if ("ChuyenKhoan".equalsIgnoreCase(paymentMethod)) {
                return "redirect:/payment/" + order.getId();
            }
            ra.addFlashAttribute("msg", "Đặt hàng thành công. Mã đơn: " + order.getId());
            return "redirect:/account/orders";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
