package com.alotra.repository;

import com.alotra.entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {
    Optional<DanhGia> findByCustomer_IdAndOrderLine_Id(Integer customerId, Integer orderLineId);
    List<DanhGia> findByCustomer_IdAndOrderLine_IdIn(Integer customerId, List<Integer> orderLineIds);

    @Query("SELECT dg FROM DanhGia dg WHERE dg.orderLine.id = :lineId")
    DanhGia findByOrderLineId(@Param("lineId") Integer lineId);

    @Query("SELECT dg FROM DanhGia dg WHERE dg.customer.id = :customerId AND dg.orderLine.id = :lineId")
    DanhGia findByCustomerIdAndLineId(@Param("customerId") Integer customerId, @Param("lineId") Integer lineId);

    interface ProductRatingStats { Double getAvg(); Long getCnt(); }

    // Average and count by Product (via order line -> variant -> product)
    @Query(value = "SELECT AVG(CAST(dg.SoSao AS FLOAT)) AS avg, COUNT(*) AS cnt\n" +
            "FROM DanhGia dg\n" +
            "JOIN CTDonHang ct ON ct.MaCT = dg.MaCT\n" +
            "JOIN BienTheSanPham bt ON bt.MaBT = ct.MaBT\n" +
            "WHERE bt.MaSP = :productId", nativeQuery = true)
    ProductRatingStats findStatsByProductId(@Param("productId") Integer productId);

    // List reviews for a product (newest first)
    @Query("SELECT dg FROM DanhGia dg " +
            "JOIN dg.orderLine ol " +
            "JOIN ol.variant v " +
            "JOIN v.product p " +
            "WHERE p.id = :productId " +
            "ORDER BY dg.createdAt DESC")
    List<DanhGia> findByProductIdOrderByCreatedAtDesc(@Param("productId") Integer productId);

    // List newest reviews with product and user data for admin page (fetch joins to avoid N+1/lazy issues)
    @Query("SELECT DISTINCT dg FROM DanhGia dg " +
            "LEFT JOIN FETCH dg.customer c " +
            "LEFT JOIN FETCH dg.orderLine ol " +
            "LEFT JOIN FETCH ol.variant v " +
            "LEFT JOIN FETCH v.product p " +
            "LEFT JOIN FETCH v.size s " +
            "ORDER BY dg.createdAt DESC")
    List<DanhGia> findAllOrderByCreatedAtDesc();
}