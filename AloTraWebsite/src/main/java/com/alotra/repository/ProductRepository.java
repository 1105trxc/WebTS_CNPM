// 📁 com/alotra/repository/ProductRepository.java
package com.alotra.repository;

import com.alotra.entity.Product;
import com.alotra.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    }

    @Query(value = "SELECT sp.MaSP AS id, sp.TenSP AS name, sp.UrlAnh AS imageUrl, MIN(b.GiaBan) AS price " +
            "FROM SanPham sp LEFT JOIN BienTheSanPham b ON b.MaSP = sp.MaSP " +
            "WHERE sp.TrangThai = 1 AND sp.DeletedAt IS NULL " +
            "GROUP BY sp.MaSP, sp.TenSP, sp.UrlAnh " +
            "ORDER BY sp.MaSP DESC", nativeQuery = true)
    List<BestSellerProjection> findBestSellersNative();

    List<Product> findByDeletedAtIsNull();
    List<Product> findByDeletedAtIsNotNull();

    // New: count active products by category (for delete guard)
    long countByCategoryAndDeletedAtIsNull(Category category);
}