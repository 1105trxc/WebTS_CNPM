package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "KhuyenMaiSanPham")
public class KhuyenMaiSanPham {
    @EmbeddedId
    private KhuyenMaiSanPhamId id = new KhuyenMaiSanPhamId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("promotionId")
    @JoinColumn(name = "MaKM")
    private SuKienKhuyenMai promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "MaSP")
    private Product product;

    @Column(name = "PhanTramGiam", nullable = false)
    private Integer discountPercent;

    public KhuyenMaiSanPhamId getId() { return id; }
    public void setId(KhuyenMaiSanPhamId id) { this.id = id; }
    public SuKienKhuyenMai getPromotion() { return promotion; }
    public void setPromotion(SuKienKhuyenMai promotion) { this.promotion = promotion; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
}
