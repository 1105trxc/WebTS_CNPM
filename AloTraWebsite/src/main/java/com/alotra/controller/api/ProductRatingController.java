package com.alotra.controller.api;

import com.alotra.repository.DanhGiaRepository;
import com.alotra.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductRatingController {
    private final ReviewService reviewService;

    public ProductRatingController(ReviewService reviewService) { this.reviewService = reviewService; }

    @GetMapping("/{id}/rating")
    public ResponseEntity<?> rating(@PathVariable("id") Integer productId) {
        DanhGiaRepository.ProductRatingStats s = reviewService.statsForProduct(productId);
        double avg = s != null && s.getAvg() != null ? s.getAvg() : 0.0;
        long cnt = s != null && s.getCnt() != null ? s.getCnt() : 0L;
        return ResponseEntity.ok(Map.of("avg", Math.round(avg * 10.0)/10.0, "count", cnt));
    }
}
