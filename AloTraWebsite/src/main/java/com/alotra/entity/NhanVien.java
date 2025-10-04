package com.alotra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "NhanVien")
public class NhanVien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaNV")
    private Integer id;

    @NotBlank
    @Column(name = "Username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "MatKhauHash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Email
    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(name = "TenNV", nullable = false)
    private String fullName;

    // 1: Chủ cửa hàng, 2: Nhân viên
    @Column(name = "VaiTro", nullable = false)
    private Integer role; 

    @Column(name = "SoDienThoai", unique = true)
    private String phone;

    // 0: Khóa, 1: Hoạt động
    @Column(name = "TrangThai", nullable = false)
    private Integer status = 1;

    @Transient
    private String plainPassword; // for form input only

    @Transient
    private String confirmPassword; // for form input only

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getPlainPassword() { return plainPassword; }
    public void setPlainPassword(String plainPassword) { this.plainPassword = plainPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
