package com.alotra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DonHang")
public class DonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDH")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", nullable = false)
    private KhachHang customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaNV")
    private NhanVien employee; // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKM")
    private SuKienKhuyenMai promotion; // nullable

    @Column(name = "NgayLap", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "TrangThaiDonHang", nullable = false)
    private String status = "ChoXuLy";

    @Column(name = "PaymentStatus", nullable = false)
    private String paymentStatus = "ChuaThanhToan";

    @Column(name = "PaymentMethod")
    private String paymentMethod;

    @Column(name = "PaidAt")
    private LocalDateTime paidAt;

    @Column(name = "TongHang", nullable = false)
    private BigDecimal tongHang = BigDecimal.ZERO;
    @Column(name = "GiamGiaDon", nullable = false)
    private BigDecimal giamGiaDon = BigDecimal.ZERO;
    @Column(name = "PhiVanChuyen", nullable = false)
    private BigDecimal phiVanChuyen = BigDecimal.ZERO;
    @Column(name = "TongThanhToan", nullable = false)
    private BigDecimal tongThanhToan = BigDecimal.ZERO;

    @Column(name = "GhiChu")
    private String note;

    // New: receiving method (Ship/Pickup)
    @Column(name = "PhuongThucNhanHang")
    private String receivingMethod;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public KhachHang getCustomer() { return customer; }
    public void setCustomer(KhachHang customer) { this.customer = customer; }
    public NhanVien getEmployee() { return employee; }
    public void setEmployee(NhanVien employee) { this.employee = employee; }
    public SuKienKhuyenMai getPromotion() { return promotion; }
    public void setPromotion(SuKienKhuyenMai promotion) { this.promotion = promotion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public BigDecimal getTongHang() { return tongHang; }
    public void setTongHang(BigDecimal tongHang) { this.tongHang = tongHang; }
    public BigDecimal getGiamGiaDon() { return giamGiaDon; }
    public void setGiamGiaDon(BigDecimal giamGiaDon) { this.giamGiaDon = giamGiaDon; }
    public BigDecimal getPhiVanChuyen() { return phiVanChuyen; }
    public void setPhiVanChuyen(BigDecimal phiVanChuyen) { this.phiVanChuyen = phiVanChuyen; }
    public BigDecimal getTongThanhToan() { return tongThanhToan; }
    public void setTongThanhToan(BigDecimal tongThanhToan) { this.tongThanhToan = tongThanhToan; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getReceivingMethod() { return receivingMethod; }
    public void setReceivingMethod(String receivingMethod) { this.receivingMethod = receivingMethod; }
}