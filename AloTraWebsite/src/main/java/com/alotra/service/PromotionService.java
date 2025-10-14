package com.alotra.service;

import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PromotionService {
    private final SuKienKhuyenMaiRepository promotionRepo;
    private final KhuyenMaiSanPhamRepository linkRepo;
    private final ProductRepository productRepo;

    public PromotionService(SuKienKhuyenMaiRepository promotionRepo,
                            KhuyenMaiSanPhamRepository linkRepo,
                            ProductRepository productRepo) {
        this.promotionRepo = promotionRepo;
        this.linkRepo = linkRepo;
        this.productRepo = productRepo;
    }

    public List<SuKienKhuyenMai> findAll() { return promotionRepo.findAll(); }
    public Optional<SuKienKhuyenMai> findById(Integer id) { return promotionRepo.findById(id); }
    public SuKienKhuyenMai save(SuKienKhuyenMai p) { return promotionRepo.save(p); }
    public List<SuKienKhuyenMai> findActive() { return promotionRepo.findByDeletedAtIsNull(); }

    @Transactional
    public void deleteById(Integer id) {
        promotionRepo.findById(id).ifPresent(p -> {
            linkRepo.deleteByPromotion(p); // remove assignments first
            promotionRepo.delete(p);
        });
    }

    public List<KhuyenMaiSanPham> listAssignments(Integer promotionId) {
        SuKienKhuyenMai p = promotionRepo.findById(promotionId).orElseThrow();
        return linkRepo.findByPromotion(p);
    }

    @Transactional
    public void assignProduct(Integer promotionId, Integer productId, Integer percent) {
        if (percent == null || percent < 1 || percent > 100) {
            throw new IllegalArgumentException("Phần trăm giảm phải trong khoảng 1..100");
        }
        SuKienKhuyenMai promo = promotionRepo.findById(promotionId).orElseThrow();
        Product product = productRepo.findById(productId).orElseThrow();
        if (linkRepo.existsByPromotionAndProduct(promo, product)) return;
        KhuyenMaiSanPham link = new KhuyenMaiSanPham();
        link.setPromotion(promo);
        link.setProduct(product);
        link.setId(new KhuyenMaiSanPhamId(promotionId, productId));
        link.setDiscountPercent(percent);
        linkRepo.save(link);
    }

    @Transactional
    public void unassignProduct(Integer promotionId, Integer productId) {
        KhuyenMaiSanPhamId id = new KhuyenMaiSanPhamId(promotionId, productId);
        linkRepo.findById(id).ifPresent(linkRepo::delete);
    }

    public List<Product> listUnassignedProducts(Integer promotionId) {
        SuKienKhuyenMai promo = promotionRepo.findById(promotionId).orElseThrow();
        Set<Integer> assigned = new HashSet<>();
        linkRepo.findByPromotion(promo).forEach(l -> assigned.add(l.getProduct().getId()));
        List<Product> all = productRepo.findAll();
        all.removeIf(p -> assigned.contains(p.getId()));
        return all;
    }
}