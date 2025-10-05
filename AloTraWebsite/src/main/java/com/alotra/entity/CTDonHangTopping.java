package com.alotra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "CTDonHang_Topping")
public class CTDonHangTopping {
    @EmbeddedId
    private CTDonHangToppingId id = new CTDonHangToppingId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ctId")
    @JoinColumn(name = "MaCT")
    private CTDonHang orderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("toppingId")
    @JoinColumn(name = "MaTopping")
    private Topping topping;

    @Column(name = "SoLuong", nullable = false)
    private Integer quantity = 1;

    @Column(name = "DonGia", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "ThanhTien", nullable = false)
    private BigDecimal lineTotal;

    public CTDonHangToppingId getId() { return id; }
    public void setId(CTDonHangToppingId id) { this.id = id; }
    public CTDonHang getOrderLine() { return orderLine; }
    public void setOrderLine(CTDonHang orderLine) { this.orderLine = orderLine; }
    public Topping getTopping() { return topping; }
    public void setTopping(Topping topping) { this.topping = topping; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
