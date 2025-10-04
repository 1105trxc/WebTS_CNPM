package com.alotra.repository;

import com.alotra.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
    KhachHang findByEmail(String email);
    KhachHang findByUsername(String username);
    KhachHang findByPhone(String phone);

    @Query("SELECT k FROM KhachHang k WHERE " +
            "(:kw IS NULL OR LOWER(k.username) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(k.email) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(k.fullName) LIKE LOWER(CONCAT('%',:kw,'%')) OR k.phone LIKE CONCAT('%',:kw,'%')) AND " +
            "(:status IS NULL OR k.status = :status)")
    List<KhachHang> search(@Param("kw") String kw, @Param("status") Integer status);
}