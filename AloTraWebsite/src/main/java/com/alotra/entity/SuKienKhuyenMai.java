package com.alotra.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "SuKienKhuyenMai")
public class SuKienKhuyenMai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKM")
    private Integer id;

    @Column(name = "TenSuKien", nullable = false)
    private String name;

    @Column(name = "MoTa")
    private String description;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "NgayBD", nullable = false)
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "NgayKT", nullable = false)
    private LocalDate endDate;

    @Column(name = "TrangThai", nullable = false)
    private Integer status = 1; // 0: inactive, 1: active

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}