package com.alotra.controller.api;

import com.alotra.dto.ProductDTO;
import com.alotra.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogApiController {
    private final ProductService productService;

    public CatalogApiController(ProductService productService) {
        this.productService = productService;
    }

    // GET /api/catalog/products?categoryId=123 | null for all
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> list(@RequestParam(required = false) Integer categoryId) {
        return ResponseEntity.ok(productService.listByCategory(categoryId));
    }
}
