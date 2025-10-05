package com.alotra.repository;

import com.alotra.entity.GioHang;
import com.alotra.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GioHangRepository extends JpaRepository<GioHang, Integer> {
    Optional<GioHang> findFirstByCustomerAndStatus(KhachHang customer, String status);
}
