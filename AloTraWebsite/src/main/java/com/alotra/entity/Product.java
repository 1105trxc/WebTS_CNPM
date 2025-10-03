// 📁 com/alotra/entity/Product.java
package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "SanPham")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSP")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDM", nullable = false)
    private Category category; // This should map to DanhMucSanPham entity

    @Column(name = "TenSP", nullable = false)
    private String name;

    @Column(name = "MoTa")
    private String description;

    @Column(name = "TrangThai", nullable = false)
    private Integer status;

    @Column(name = "UrlAnh")
    private String imageUrl;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}