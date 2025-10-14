package com.alotra.repository;

import com.alotra.entity.SuKienKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface SuKienKhuyenMaiRepository extends JpaRepository<SuKienKhuyenMai, Integer> {
    List<SuKienKhuyenMai> findTop8ByStatusOrderByStartDateDesc(Integer status);
    List<SuKienKhuyenMai> findTop8ByStatusAndDeletedAtIsNullOrderByStartDateDesc(Integer status);

    @Modifying
    @Query("UPDATE SuKienKhuyenMai s SET s.views = COALESCE(s.views,0) + 1 WHERE s.id = :id")
    int incrementViews(@Param("id") Integer id);

    List<SuKienKhuyenMai> findByDeletedAtIsNull();
    List<SuKienKhuyenMai> findByDeletedAtIsNotNull();
}