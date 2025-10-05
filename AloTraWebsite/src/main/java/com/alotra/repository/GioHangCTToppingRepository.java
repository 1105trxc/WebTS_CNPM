package com.alotra.repository;

import com.alotra.entity.GioHangCTTopping;
import com.alotra.entity.GioHangCTToppingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GioHangCTToppingRepository extends JpaRepository<GioHangCTTopping, GioHangCTToppingId> {
}
