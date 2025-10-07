// 📁 com/alotra/service/ProductService.java
package com.alotra.service;

import com.alotra.dto.ProductDTO;
import com.alotra.repository.ProductRepository;
import com.alotra.repository.KhuyenMaiSanPhamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private KhuyenMaiSanPhamRepository promoRepo;

    public List<ProductDTO> findBestSellers() {
        // Use native query (SanPham + BienTheSanPham); map imageUrl from DB; fallback to placeholder only if empty
        return productRepository.findBestSellersNative().stream()
                .map(row -> {
                    BigDecimal minBase = row.getPrice();
                    Integer percent = row.getId() != null ? promoRepo.findActiveMaxDiscountPercentForProduct(row.getId()) : null;
                    BigDecimal finalPrice = applyPercent(minBase, percent);
                    ProductDTO dto = new ProductDTO(
                            row.getId(),
                            row.getName(),
                            (row.getImageUrl() != null && !row.getImageUrl().isBlank()) ? row.getImageUrl() : "/images/placeholder.png",
                            finalPrice
                    );
                    dto.setOriginalPrice(minBase);
                    dto.setDiscountPercent(percent);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // New: list products by category (null => all)
    public List<ProductDTO> listByCategory(Integer categoryId) {
        return productRepository.findListByCategoryNative(categoryId).stream()
                .map(row -> {
                    BigDecimal minBase = row.getPrice();
                    Integer percent = row.getId() != null ? promoRepo.findActiveMaxDiscountPercentForProduct(row.getId()) : null;
                    BigDecimal finalPrice = applyPercent(minBase, percent);
                    ProductDTO dto = new ProductDTO(
                            row.getId(),
                            row.getName(),
                            (row.getImageUrl() != null && !row.getImageUrl().isBlank()) ? row.getImageUrl() : "/images/placeholder.png",
                            finalPrice
                    );
                    dto.setOriginalPrice(minBase);
                    dto.setDiscountPercent(percent);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal applyPercent(BigDecimal base, Integer percent) {
        if (base == null) return null;
        if (percent == null || percent <= 0) return base;
        BigDecimal p = BigDecimal.valueOf(100 - Math.min(100, percent)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return base.multiply(p).setScale(0, RoundingMode.HALF_UP); // round to VNĐ
    }
}