package com.alotra.repository;

import com.alotra.entity.Topping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToppingRepository extends JpaRepository<Topping, Integer> {
    List<Topping> findByDeletedAtIsNull();
    List<Topping> findByDeletedAtIsNotNull();
    Topping findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}