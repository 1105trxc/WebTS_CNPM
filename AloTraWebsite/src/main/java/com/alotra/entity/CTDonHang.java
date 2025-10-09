package com.alotra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "CTDonHang")
public class CTDonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCT")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDH", nullable = false)
    private DonHang order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaBT", nullable = false)
    private ProductVariant variant;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity;

    @Column(name = "DonGia", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "GiamGiaDong", nullable = false)
    private BigDecimal lineDiscount = BigDecimal.ZERO;

    @Column(name = "ThanhTien", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "GhiChu")
    private String note;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public DonHang getOrder() { return order; }
    public void setOrder(DonHang order) { this.order = order; }
    public ProductVariant getVariant() { return variant; }
    public void setVariant(ProductVariant variant) { this.variant = variant; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineDiscount() { return lineDiscount; }
    public void setLineDiscount(BigDecimal lineDiscount) { this.lineDiscount = lineDiscount; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}