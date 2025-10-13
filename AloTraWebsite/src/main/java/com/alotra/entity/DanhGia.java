package com.alotra.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "DanhGia",
       uniqueConstraints = @UniqueConstraint(name = "UQ_DanhGia_OnePerLine", columnNames = {"MaKH", "MaCT"}))
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDG")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaKH", referencedColumnName = "MaKH", nullable = false)
    private KhachHang customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaCT", referencedColumnName = "MaCT", nullable = false)
    private CTDonHang orderLine;

    @Column(name = "SoSao", nullable = false)
    private Integer stars; // 1..5

    @Column(name = "BinhLuan")
    private String comment;

    @Column(name = "NgayDG", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "TraLoiAdmin")
    private String adminReply;

    @Column(name = "TraLoiLuc")
    private LocalDateTime adminRepliedAt;

    @Column(name = "TraLoiBoi")
    private String adminRepliedBy;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public KhachHang getCustomer() { return customer; }
    public void setCustomer(KhachHang customer) { this.customer = customer; }
    public CTDonHang getOrderLine() { return orderLine; }
    public void setOrderLine(CTDonHang orderLine) { this.orderLine = orderLine; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public LocalDateTime getAdminRepliedAt() { return adminRepliedAt; }
    public void setAdminRepliedAt(LocalDateTime adminRepliedAt) { this.adminRepliedAt = adminRepliedAt; }
    public String getAdminRepliedBy() { return adminRepliedBy; }
    public void setAdminRepliedBy(String adminRepliedBy) { this.adminRepliedBy = adminRepliedBy; }
}