package com.alotra.service;

import com.alotra.entity.CTDonHang;
import com.alotra.entity.DanhGia;
import com.alotra.entity.DonHang;
import com.alotra.entity.KhachHang;
import com.alotra.repository.CTDonHangRepository;
import com.alotra.repository.DanhGiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    public static final Duration EDIT_WINDOW = Duration.ofMinutes(15);

    private final DanhGiaRepository reviewRepo;
    private final CTDonHangRepository orderLineRepo;

    public ReviewService(DanhGiaRepository reviewRepo, CTDonHangRepository orderLineRepo) {
        this.reviewRepo = reviewRepo;
        this.orderLineRepo = orderLineRepo;
    }

    public Map<Integer, DanhGia> findByCustomerAndLineIds(Integer customerId, List<Integer> lineIds) {
        if (customerId == null || lineIds == null || lineIds.isEmpty()) return Map.of();
        return reviewRepo.findByCustomer_IdAndOrderLine_IdIn(customerId, lineIds).stream()
                .collect(Collectors.toMap(d -> d.getOrderLine().getId(), d -> d));
    }

    // Backwards-compatible name used by AccountController
    public Map<Integer, DanhGia> findExistingByCustomerAndLines(Integer customerId, List<Integer> lineIds) {
        return findByCustomerAndLineIds(customerId, lineIds);
    }

    public boolean canEdit(DanhGia r) {
        if (r == null || r.getCreatedAt() == null) return false;
        return Duration.between(r.getCreatedAt(), LocalDateTime.now()).compareTo(EDIT_WINDOW) <= 0;
    }

    public boolean isOrderEligibleForReview(String orderStatus, String paymentStatus) {
        return "DaGiao".equals(orderStatus) && "DaThanhToan".equals(paymentStatus);
    }

    @Transactional
    public void submitReview(KhachHang customer, Integer orderLineId, int stars, String comment) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        CTDonHang line = orderLineRepo.findById(orderLineId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng đơn hàng"));
        DonHang order = line.getOrder();
        if (order == null || order.getCustomer() == null || !Objects.equals(order.getCustomer().getId(), customer.getId())) {
            throw new SecurityException("Bạn không có quyền đánh giá dòng đơn này");
        }
        if (!isOrderEligibleForReview(order.getStatus(), order.getPaymentStatus())) {
            throw new IllegalStateException("Chỉ đánh giá được khi đơn đã giao và đã thanh toán");
        }
        // Ensure not already reviewed
        reviewRepo.findByCustomer_IdAndOrderLine_Id(customer.getId(), orderLineId).ifPresent(r -> {
            throw new IllegalStateException("Bạn đã đánh giá dòng này rồi");
        });
        DanhGia rv = new DanhGia();
        rv.setCustomer(customer);
        rv.setOrderLine(line);
        rv.setStars(stars);
        rv.setComment(comment);
        rv.setCreatedAt(LocalDateTime.now());
        reviewRepo.save(rv);
    }
    
    @Transactional
    public DanhGia updateIfAllowed(KhachHang kh, Integer reviewId, int stars, String comment) {
        DanhGia r = reviewRepo.findById(reviewId).orElseThrow();
        if (!Objects.equals(r.getCustomer().getId(), kh.getId())) {
            throw new SecurityException("Không thể sửa đánh giá của người khác");
        }
        if (!canEdit(r)) throw new IllegalStateException("Hết thời gian cho phép chỉnh sửa");
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Số sao phải từ 1..5");
        r.setStars(stars);
        r.setComment((comment != null && !comment.isBlank()) ? comment.trim() : null);
        return reviewRepo.save(r);
    }

    @Transactional
    public void deleteIfAllowed(KhachHang kh, Integer reviewId) {
        DanhGia r = reviewRepo.findById(reviewId).orElseThrow();
        if (!Objects.equals(r.getCustomer().getId(), kh.getId())) {
            throw new SecurityException("Không thể xóa đánh giá của người khác");
        }
        if (!canEdit(r)) throw new IllegalStateException("Hết thời gian cho phép xóa");
        reviewRepo.delete(r);
    }

    public DanhGiaRepository.ProductRatingStats statsForProduct(Integer productId) {
        return reviewRepo.findStatsByProductId(productId);
    }

    // New: list reviews for product with optional limit
    public java.util.List<DanhGia> listByProduct(Integer productId, Integer limit) {
        java.util.List<DanhGia> list = reviewRepo.findByProductIdOrderByCreatedAtDesc(productId);
        if (limit != null && limit > 0 && list.size() > limit) return list.subList(0, limit);
        return list;
    }
}