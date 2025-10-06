package com.alotra.controller;

import com.alotra.entity.SuKienKhuyenMai;
import com.alotra.entity.KhuyenMaiSanPham;
import com.alotra.repository.SuKienKhuyenMaiRepository;
import com.alotra.repository.KhuyenMaiSanPhamRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/promotions")
public class PromotionController {
    private final SuKienKhuyenMaiRepository promoRepo;
    private final KhuyenMaiSanPhamRepository linkRepo;

    public PromotionController(SuKienKhuyenMaiRepository promoRepo, KhuyenMaiSanPhamRepository linkRepo) {
        this.promoRepo = promoRepo;
        this.linkRepo = linkRepo;
    }

    @GetMapping
    public String list(Model model) {
        List<SuKienKhuyenMai> items = promoRepo.findTop8ByStatusOrderByStartDateDesc(1);
        model.addAttribute("pageTitle", "Tin tức & Khuyến mãi");
        model.addAttribute("items", items);
        return "promotions/list";
    }

    @GetMapping("/{id}")
    @Transactional
    public String detail(@PathVariable Integer id, Model model) {
        SuKienKhuyenMai p = promoRepo.findById(id).orElse(null);
        if (p == null) return "redirect:/promotions";
        // increment views and sync local object
        promoRepo.incrementViews(id);
        p.setViews((p.getViews() == null ? 0 : p.getViews()) + 1);
        List<KhuyenMaiSanPham> assigned = linkRepo.findByPromotion(p);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String period = (p.getStartDate() != null ? df.format(p.getStartDate()) : "?") +
                " - " + (p.getEndDate() != null ? df.format(p.getEndDate()) : "?");
        model.addAttribute("item", p);
        model.addAttribute("period", period);
        model.addAttribute("assigned", assigned);
        model.addAttribute("pageTitle", p.getName());
        return "promotions/detail";
    }
}
