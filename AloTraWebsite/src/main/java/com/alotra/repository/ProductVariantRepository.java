package com.alotra.repository;

import com.alotra.entity.Product;
import com.alotra.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProduct(Product product);
    void deleteByProduct(Product product);

    // New: pick the cheapest variant for a product (used when adding by productId only)
    Optional<ProductVariant> findTopByProductOrderByPriceAsc(Product product);

    // New: fetch size eagerly to avoid LazyInitializationException in views
    @Query("select v from ProductVariant v left join fetch v.size where v.product = :product")
    List<ProductVariant> findByProductFetchingSize(@Param("product") Product product);
}