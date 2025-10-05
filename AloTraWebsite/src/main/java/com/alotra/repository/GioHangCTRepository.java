package com.alotra.repository;

import com.alotra.entity.GioHang;
import com.alotra.entity.GioHangCT;
import com.alotra.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GioHangCTRepository extends JpaRepository<GioHangCT, Integer> {
    List<GioHangCT> findByCart(GioHang cart);
    Optional<GioHangCT> findByCartAndVariant(GioHang cart, ProductVariant variant);
}
