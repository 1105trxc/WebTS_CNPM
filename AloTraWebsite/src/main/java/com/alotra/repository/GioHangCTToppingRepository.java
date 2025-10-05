package com.alotra.repository;

import com.alotra.entity.GioHangCT;
import com.alotra.entity.GioHangCTTopping;
import com.alotra.entity.GioHangCTToppingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GioHangCTToppingRepository extends JpaRepository<GioHangCTTopping, GioHangCTToppingId> {
    List<GioHangCTTopping> findByCartItem(GioHangCT cartItem);
}