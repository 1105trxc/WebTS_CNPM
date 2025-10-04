// 📁 com/alotra/service/ProductService.java
package com.alotra.service;

import com.alotra.dto.ProductDTO;
import com.alotra.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<ProductDTO> findBestSellers() {
        // Use native query (SanPham + BienTheSanPham); map imageUrl from DB; fallback to placeholder only if empty
        return productRepository.findBestSellersNative().stream()
                .map(row -> new ProductDTO(
                        row.getId(),
                        row.getName(),
                        (row.getImageUrl() != null && !row.getImageUrl().isBlank()) ? row.getImageUrl() : "/images/placeholder.png",
                        row.getPrice()
                ))
                .collect(Collectors.toList());
    }
}