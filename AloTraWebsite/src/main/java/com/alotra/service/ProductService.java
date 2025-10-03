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
        // Lấy sản phẩm theo schema mới bằng native query (SanPham + BienTheSanPham)
        return productRepository.findBestSellersNative().stream()
                .map(row -> new ProductDTO(
                        row.getId(),
                        row.getName(),
                        "/images/placeholder.png", // Schema mới chưa có bảng media -> dùng ảnh mặc định
                        row.getPrice()
                ))
                .collect(Collectors.toList());
    }
}