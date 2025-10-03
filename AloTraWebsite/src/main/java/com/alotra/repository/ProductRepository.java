// 📁 com/alotra/repository/ProductRepository.java
package com.alotra.repository;

import com.alotra.entity.Product;
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
    }

    @Query(value = "SELECT sp.MaSP AS id, sp.TenSP AS name, MIN(b.GiaBan) AS price " +
            "FROM SanPham sp JOIN BienTheSanPham b ON b.MaSP = sp.MaSP " +
            "WHERE sp.TrangThai = 1 " +
            "GROUP BY sp.MaSP, sp.TenSP", nativeQuery = true)
    List<BestSellerProjection> findBestSellersNative();
}