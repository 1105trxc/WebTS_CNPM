package com.alotra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "GioHangCT_Topping")
public class GioHangCTTopping {
    @EmbeddedId
    private GioHangCTToppingId id = new GioHangCTToppingId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ctghId")
    @JoinColumn(name = "MaCTGH")
    private GioHangCT cartItem;

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

    public GioHangCTToppingId getId() { return id; }
    public void setId(GioHangCTToppingId id) { this.id = id; }
    public GioHangCT getCartItem() { return cartItem; }
    public void setCartItem(GioHangCT cartItem) { this.cartItem = cartItem; }
    public Topping getTopping() { return topping; }
    public void setTopping(Topping topping) { this.topping = topping; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
