package com.alotra.repository;

import com.alotra.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    KhachHang findByEmail(String email);
    KhachHang findByUsername(String username);
    KhachHang findByPhone(String phone);
}
