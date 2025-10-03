// 📁 com/alotra/entity/Size.java
package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "SizeSanPham")
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSize")
    private Integer id;

    @Column(name = "TenSize", nullable = false, unique = true)
    private String name;

    @Column(name = "TrangThai", nullable = false)
    private Integer status;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}