/*
package com.alotra.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.alotra.entity.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);
    List<Branch> findByStatus(String status);
    List<Branch> findByNameContainingIgnoreCaseAndStatusOrAddressContainingIgnoreCaseAndStatus(String name, String status1, String address, String status2);
    @Query("SELECT b FROM Branch b WHERE " +
            "(:keyword IS NULL OR b.name LIKE %:keyword% OR b.address LIKE %:keyword%) AND " +
            "(:status IS NULL OR b.status = :status)")
     List<Branch> searchAndFilter(String keyword, String status);
}
*/