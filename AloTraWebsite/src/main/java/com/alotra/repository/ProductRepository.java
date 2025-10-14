// 📁 com/alotra/repository/ProductRepository.java
package com.alotra.repository;

import com.alotra.entity.Product;
import com.alotra.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    interface BestSellerProjection {
        Integer getId();
        String getName();
        BigDecimal getPrice();
        String getImageUrl();
        // New: total sold quantity (only paid orders)
        Long getSoldQty();
    }

    // Top 5 best sellers by total quantity sold (paid orders only). Includes products with zero sales.
    @Query(value = "SELECT TOP 5 sp.MaSP AS id, sp.TenSP AS name, sp.UrlAnh AS imageUrl, " +
            " MIN(b.GiaBan) AS price, COALESCE(SUM(CASE WHEN dh.PaymentStatus='DaThanhToan' THEN ct.SoLuong ELSE 0 END),0) AS soldQty " +
            "FROM SanPham sp " +
            "LEFT JOIN BienTheSanPham b ON b.MaSP = sp.MaSP " +
            "LEFT JOIN CTDonHang ct ON ct.MaBT = b.MaBT " +
            "LEFT JOIN DonHang dh ON dh.MaDH = ct.MaDH " +
            "WHERE sp.TrangThai = 1 AND sp.DeletedAt IS NULL " +
            "GROUP BY sp.MaSP, sp.TenSP, sp.UrlAnh " +
            "ORDER BY soldQty DESC, sp.MaSP DESC", nativeQuery = true)
    List<BestSellerProjection> findBestSellersNative();

    // New: list products by category (null = all)
    @Query(value = "SELECT sp.MaSP AS id, sp.TenSP AS name, sp.UrlAnh AS imageUrl, MIN(b.GiaBan) AS price " +
            "FROM SanPham sp LEFT JOIN BienTheSanPham b ON b.MaSP = sp.MaSP " +
            "WHERE sp.TrangThai = 1 AND sp.DeletedAt IS NULL " +
            "AND (:categoryId IS NULL OR sp.MaDM = :categoryId) " +
            "GROUP BY sp.MaSP, sp.TenSP, sp.UrlAnh " +
            "ORDER BY sp.MaSP DESC", nativeQuery = true)
    List<BestSellerProjection> findListByCategoryNative(@Param("categoryId") Integer categoryId);

    List<Product> findByDeletedAtIsNull();
    List<Product> findByDeletedAtIsNotNull();

    // New: count active products by category (for delete guard)
    long countByCategoryAndDeletedAtIsNull(Category category);

    // New: filter active products by category id
    List<Product> findByCategory_IdAndDeletedAtIsNull(Integer categoryId);

    // New: keyword search by product or category name (case-insensitive)
    @Query(value = "SELECT TOP 20 sp.MaSP AS id, sp.TenSP AS name, sp.UrlAnh AS imageUrl, MIN(b.GiaBan) AS price " +
            "FROM SanPham sp " +
            "LEFT JOIN BienTheSanPham b ON b.MaSP = sp.MaSP " +
            "LEFT JOIN DanhMucSanPham dm ON dm.MaDM = sp.MaDM " +
            "WHERE sp.TrangThai = 1 AND sp.DeletedAt IS NULL " +
            "AND (LOWER(sp.TenSP) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(dm.TenDM) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
            "GROUP BY sp.MaSP, sp.TenSP, sp.UrlAnh " +
            "ORDER BY sp.MaSP DESC", nativeQuery = true)
    List<BestSellerProjection> searchByKeywordNative(@Param("kw") String keyword);

    // New: find active product by name (case-insensitive) for duplicate check
    Product findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    // === Admin: search with optional filters (keyword/category/status) among non-deleted products ===
    @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL " +
            "AND (:kw IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:status IS NULL OR p.status = :status) " +
            "ORDER BY p.id DESC")
    List<Product> adminSearch(@Param("kw") String kw,
                              @Param("categoryId") Integer categoryId,
                              @Param("status") Integer status);
}