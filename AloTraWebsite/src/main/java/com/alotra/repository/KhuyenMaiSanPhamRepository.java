package com.alotra.repository;

import com.alotra.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhuyenMaiSanPhamRepository extends JpaRepository<KhuyenMaiSanPham, KhuyenMaiSanPhamId> {
    List<KhuyenMaiSanPham> findByPromotion(SuKienKhuyenMai promotion);
    long deleteByPromotion(SuKienKhuyenMai promotion);
    boolean existsByPromotionAndProduct(SuKienKhuyenMai promotion, Product product);
}
