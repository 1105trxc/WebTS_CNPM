/*package com.alotra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@Entity
@Table(name = "Users")
public class User implements UserDetails { // << BỔ SUNG QUAN TRỌNG

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Column(name = "Email", unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Column(name = "Phone", unique = true)
    private String phone;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "Họ tên không được để trống")
    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "AvatarUrl")
    private String avatarUrl;

    @Column(name = "Gender")
    private String gender;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "IdCardNumber", unique = true)
    private String idCardNumber;

    @Column(name = "EmailVerifiedAt")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "Status", nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "RoleId", nullable = false)
    private Role role;

    @Transient
    private String rawPassword;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    // --- CÁC PHƯƠNG THỨC TIỆN ÍCH ---

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Address getDefaultAddress() {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        return addresses.stream()
                        .filter(Address::isDefault)
                        .findFirst()
                        .orElse(addresses.get(0));
    }

    // === PHẦN BỔ SUNG CHO SPRING SECURITY (UserDetails) ===

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Trả về vai trò của người dùng với tiền tố ROLE_
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role.getCode()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash; // Cung cấp mật khẩu đã mã hóa
    }

    @Override
    public String getUsername() {
        return this.email; // Dùng email làm username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.status.equals("BANNED"); // Tài khoản không bị khóa
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status.equals("ACTIVE"); // Tài khoản được kích hoạt
    }
}*/

/*
// User entity commented out because it does not exist in the current database schema.
package com.alotra.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...fields...
}
*/