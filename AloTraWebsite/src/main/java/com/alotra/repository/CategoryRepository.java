package com.alotra.repository;

import com.alotra.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByDeletedAtIsNull();
    List<Category> findByDeletedAtIsNotNull();
    // New: check duplicate active name (case-insensitive)
    Category findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}