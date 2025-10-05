package com.alotra.controller;

import com.alotra.entity.*;
import com.alotra.repository.ProductRepository;
import com.alotra.repository.ProductVariantRepository;
import com.alotra.repository.ToppingRepository;
import com.alotra.security.KhachHangUserDetails;
import com.alotra.service.CartService;
import com.alotra.service.KhachHangService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository productRepo;
    private final ProductVariantRepository variantRepo;
    private final ToppingRepository toppingRepo;
    private final CartService cartService;
    private final KhachHangService khService;

    public ProductController(ProductRepository productRepo,
                             ProductVariantRepository variantRepo,
                             ToppingRepository toppingRepo,
                             CartService cartService,
                             KhachHangService khService) {
        this.productRepo = productRepo;
        this.variantRepo = variantRepo;
        this.toppingRepo = toppingRepo;
        this.cartService = cartService;
        this.khService = khService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Product p = productRepo.findById(id).orElseThrow();
        List<ProductVariant> variants = variantRepo.findByProduct(p);
        variants.removeIf(v -> v.getStatus() == null || v.getStatus() == 0);
        variants.sort(Comparator.comparing(ProductVariant::getPrice));
        List<Topping> toppings = toppingRepo.findByDeletedAtIsNull();
        toppings.removeIf(t -> t.getStatus() != null && t.getStatus() == 0);

        model.addAttribute("pageTitle", p.getName());
        model.addAttribute("product", p);
        model.addAttribute("variants", variants);
        model.addAttribute("toppings", toppings);
        return "products/product_detail";
    }

    @PostMapping("/{id}/add-to-cart")
    public String addToCart(@PathVariable Integer id,
                            @RequestParam("variantId") Integer variantId,
                            @RequestParam(value = "qty", defaultValue = "1") Integer qty,
                            @RequestParam(value = "sugar", defaultValue = "Bình thường") String sugar,
                            @RequestParam(value = "tea", defaultValue = "Bình thường") String tea,
                            @RequestParam(value = "ice", defaultValue = "Bình thường") String ice,
                            HttpServletRequest request,
                            @SessionAttribute(name = "SPRING_SECURITY_CONTEXT", required = false) Object ignored,
                            @org.springframework.security.core.annotation.AuthenticationPrincipal KhachHangUserDetails principal,
                            RedirectAttributes ra) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        KhachHang kh = null;
        if (principal != null) kh = principal.getKhachHang();
        if (kh == null && auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            kh = khService.findByUsername(auth.getName());
        }
        if (kh == null) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng");
            return "redirect:/login";
        }
        Map<Integer, Integer> toppingQty = extractToppings(request);
        String note = String.format("Đường: %s; Trà: %s; Đá: %s", sugar, tea, ice);
        try {
            cartService.addItemWithOptions(kh, variantId, qty, toppingQty, note);
            ra.addFlashAttribute("message", "Đã thêm vào giỏ hàng");
            return "redirect:/cart";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/products/" + id;
        }
    }

    private Map<Integer, Integer> extractToppings(HttpServletRequest request) {
        Map<Integer, Integer> map = new HashMap<>();
        // Expect inputs named like: toppings[12] = 2
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
