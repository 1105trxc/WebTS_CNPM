package com.alotra.controller;

import com.alotra.entity.DonHang;
import com.alotra.entity.KhachHang;
import com.alotra.entity.GioHangCT;
import com.alotra.entity.GioHangCTTopping;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.CartService;
import com.alotra.service.KhachHangService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CartController {
    private final CartService cartService;
    private final KhachHangService khachHangService;

    public CartController(CartService cartService, KhachHangService khachHangService) {
        this.cartService = cartService;
        this.khachHangService = khachHangService;
    }

    private KhachHang currentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof KhachHangUserDetails d) return d.getKhachHang();
        if (principal instanceof UserDetails d) return khachHangService.findByUsername(d.getUsername());
        if (principal instanceof String s && !"anonymousUser".equals(s)) return khachHangService.findByUsername(s);
        return null;
    }

    private boolean isAnonymous(Authentication auth) {
        return auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal()));
    }

    private boolean isAuthenticatedButNotCustomer(Authentication auth, KhachHang kh) {
        return auth != null && auth.isAuthenticated() && kh == null && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        KhachHang kh = currentCustomer();
        List<GioHangCT> items = (kh != null) ? cartService.listItems(kh) : Collections.<GioHangCT>emptyList();
        model.addAttribute("pageTitle", "Giỏ Hàng");
        model.addAttribute("items", items);
        model.addAttribute("total", cartService.calcTotal(items));
        // Toppings catalog and current toppings per item for inline edit
        Map<Integer, List<GioHangCTTopping>> itemToppingsMap = cartService.getToppingsForItems(items);
        model.addAttribute("toppingsCatalog", cartService.listActiveToppings());
        model.addAttribute("itemToppingsMap", itemToppingsMap);
        // Build quantity lookup map: itemId -> (toppingId -> qty)
        Map<Integer, Map<Integer, Integer>> itemTopQtyMap = new HashMap<>();
        for (GioHangCT it : items) {
            List<GioHangCTTopping> tops = itemToppingsMap.getOrDefault(it.getId(), List.of());
            Map<Integer,Integer> qmap = tops.stream().collect(Collectors.toMap(t -> t.getTopping().getId(), GioHangCTTopping::getQuantity));
            itemTopQtyMap.put(it.getId(), qmap);
        }
        model.addAttribute("itemTopQtyMap", itemTopQtyMap);
        return "cart/cart";
    }

    @GetMapping("/cart/add")
    public String addToCartGet(@RequestParam(value = "productId", required = false) Integer productId,
                               @RequestParam(value = "variantId", required = false) Integer variantId,
                               @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                               @RequestParam(value = "note", required = false) String note,
                               RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        KhachHang kh = currentCustomer();
        if (isAnonymous(auth)) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
            return "redirect:/login";
        }
        if (isAuthenticatedButNotCustomer(auth, kh)) {
            ra.addFlashAttribute("error", "Tài khoản hiện tại không thể mua hàng. Vui lòng đăng nhập bằng tài khoản Khách hàng.");
            return "redirect:/";
        }
        try {
            cartService.addItem(kh, productId, variantId, qty, note);
            ra.addFlashAttribute("message", "Đã thêm vào giỏ hàng");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam(value = "productId", required = false) Integer productId,
                            @RequestParam(value = "variantId", required = false) Integer variantId,
                            @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                            @RequestParam(value = "note", required = false) String note,
                            RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        KhachHang kh = currentCustomer();
        if (isAnonymous(auth)) return "redirect:/login";
        if (isAuthenticatedButNotCustomer(auth, kh)) {
            ra.addFlashAttribute("error", "Tài khoản hiện tại không thể mua hàng. Vui lòng đăng nhập bằng tài khoản Khách hàng.");
            return "redirect:/";
        }
        try {
            cartService.addItem(kh, productId, variantId, qty, note);
            ra.addFlashAttribute("message", "Đã thêm vào giỏ hàng");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateQty(@RequestParam("itemId") Integer itemId,
                            @RequestParam("qty") Integer qty) {
        KhachHang kh = currentCustomer();
        if (kh == null) return "redirect:/login";
        cartService.updateQuantity(kh, itemId, qty);
        return "redirect:/cart";
    }

    @GetMapping("/cart/remove/{id}")
    public String remove(@PathVariable Integer id) {
        KhachHang kh = currentCustomer();
        if (kh == null) return "redirect:/login";
        cartService.removeItem(kh, id);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam(value = "itemIds", required = false) List<Integer> itemIds,
                           @RequestParam(value = "all", required = false, defaultValue = "false") boolean checkoutAll,
                           @RequestParam(value = "paymentMethod", defaultValue = "Tiền mặt") String paymentMethod,
                           RedirectAttributes ra) {
        KhachHang kh = currentCustomer();
        if (kh == null) return "redirect:/login";
        try {
            // If caller requests checkout all or itemIds not provided, collect all active cart item ids
            if (checkoutAll || itemIds == null || itemIds.isEmpty()) {
                List<GioHangCT> items = cartService.listItems(kh);
                itemIds = items.stream().map(GioHangCT::getId).collect(Collectors.toList());
            }
            DonHang order = cartService.checkout(kh, itemIds, paymentMethod);
            ra.addFlashAttribute("message", "Đặt hàng thành công. Mã đơn: " + order.getId());
            ra.addFlashAttribute("orderId", order.getId());
            return "redirect:/checkout/success";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/checkout/success")
    public String successPage(@ModelAttribute("orderId") Integer orderId, Model model) {
        model.addAttribute("pageTitle", "Đặt hàng thành công");
        model.addAttribute("orderId", orderId);
        return "checkout/success";
    }

    @PostMapping("/cart/item/{id}/toppings")
    public String updateItemToppings(@PathVariable("id") Integer itemId,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        KhachHang kh = currentCustomer();
        if (kh == null) return "redirect:/login";
        Map<Integer,Integer> toppingQty = extractToppings(request);
        cartService.updateToppings(kh, itemId, toppingQty);
        ra.addFlashAttribute("message", "Đã cập nhật topping cho sản phẩm trong giỏ.");
        return "redirect:/cart";
    }

    private Map<Integer,Integer> extractToppings(HttpServletRequest request) {
        Map<Integer,Integer> map = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("toppings[") && k.endsWith("]")) {
                try {
                    String idStr = k.substring(9, k.length() - 1);
                    Integer tid = Integer.parseInt(idStr);
                    Integer q = Integer.parseInt(v[0]);
                    if (q != null && q > 0) map.put(tid, q);
                } catch (Exception ignored) { }
            }
        });
        return map;
    }
}