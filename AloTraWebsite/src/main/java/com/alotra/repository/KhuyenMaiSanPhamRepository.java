package com.alotra.repository;

import com.alotra.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface KhuyenMaiSanPhamRepository extends JpaRepository<KhuyenMaiSanPham, KhuyenMaiSanPhamId> {
    List<KhuyenMaiSanPham> findByPromotion(SuKienKhuyenMai promotion);
    long deleteByPromotion(SuKienKhuyenMai promotion);
    boolean existsByPromotionAndProduct(SuKienKhuyenMai promotion, Product product);

    // Max discount percent for a product across active promotions (status=1, today within range)
    @Query(value = "SELECT MAX(k.PhanTramGiam) FROM KhuyenMaiSanPham k\n" +
            "JOIN SuKienKhuyenMai s ON s.MaKM = k.MaKM\n" +
            "WHERE k.MaSP = :productId AND s.TrangThai = 1\n" +
            "AND CONVERT(date, SYSUTCDATETIME()) BETWEEN s.NgayBD AND s.NgayKT", nativeQuery = true)
    Integer findActiveMaxDiscountPercentForProduct(@Param("productId") Integer productId);
}