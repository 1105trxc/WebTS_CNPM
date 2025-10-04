package com.alotra.repository;

import com.alotra.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {
    NhanVien findByUsername(String username);
    NhanVien findByEmail(String email);
    NhanVien findByPhone(String phone);

    @Query("SELECT n FROM NhanVien n WHERE n.deletedAt IS NULL AND " +
            "(:kw IS NULL OR LOWER(n.username) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(n.email) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(n.fullName) LIKE LOWER(CONCAT('%',:kw,'%')) OR n.phone LIKE CONCAT('%',:kw,'%')) AND " +
            "(:role IS NULL OR n.role = :role) AND " +
            "(:status IS NULL OR n.status = :status)")
    List<NhanVien> search(@Param("kw") String kw, @Param("role") Integer role, @Param("status") Integer status);

    List<NhanVien> findByDeletedAtIsNull();
    List<NhanVien> findByDeletedAtIsNotNull();
}