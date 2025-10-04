// 📁 com/alotra/entity/Size.java
package com.alotra.entity;

// This class was a duplicate mapping of table SizeSanPham, which caused
// Hibernate to try ALTER COLUMN (length/type) at startup. We convert it
// to a simple POJO (no @Entity) so only SizeSanPham is used by JPA.

// import jakarta.persistence.*; // no longer needed

public class Size {
    private Integer id;
    private String name;
    private Integer status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}