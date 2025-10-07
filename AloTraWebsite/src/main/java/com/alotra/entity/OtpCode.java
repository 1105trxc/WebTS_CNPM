package com.alotra.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OtpCodes")
public class OtpCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "KhachHangId", referencedColumnName = "MaKH")
    private KhachHang customer;

    @Column(name = "Code", nullable = false, length = 10)
    private String code;

    @Column(name = "Type", nullable = false, length = 30)
    private String type; // REGISTER, RESET_PASSWORD, ...

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "UsedAt")
    private LocalDateTime usedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KhachHang getCustomer() { return customer; }
    public void setCustomer(KhachHang customer) { this.customer = customer; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    @Transient
    public boolean isExpired() { return expiresAt != null && LocalDateTime.now().isAfter(expiresAt); }

    @Transient
    public boolean isUsed() { return usedAt != null; }
}