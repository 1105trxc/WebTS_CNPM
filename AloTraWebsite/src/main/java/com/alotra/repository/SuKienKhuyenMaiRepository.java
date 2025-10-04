package com.alotra.repository;

import com.alotra.entity.SuKienKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuKienKhuyenMaiRepository extends JpaRepository<SuKienKhuyenMai, Integer> {
}
