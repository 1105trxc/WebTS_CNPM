package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "DanhMucSanPham")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDM")
    private Integer id;

    @Column(name = "TenDM", nullable = false, unique = true)
    private String name;

    @Column(name = "MoTa")
    private String description;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}