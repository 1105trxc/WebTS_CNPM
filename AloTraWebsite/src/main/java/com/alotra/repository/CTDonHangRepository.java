package com.alotra.repository;

import com.alotra.entity.CTDonHang;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CTDonHangRepository extends JpaRepository<CTDonHang, Integer> {
    // Count order lines that belong to variants of the specified product
    long countByVariant_Product_Id(Integer productId);
}