// 📁 com/alotra/entity/ProductVariant.java
package com.alotra.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "BienTheSanPham")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaBT")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSize", nullable = false)
    private SizeSanPham size;

    @Column(name = "GiaBan", nullable = false)
    private BigDecimal price;

    @Column(name = "TrangThai", nullable = false)
    private Integer status;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public SizeSanPham getSize() { return size; }
    public void setSize(SizeSanPham size) { this.size = size; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}