package com.alotra.security;

import com.alotra.entity.KhachHang;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class KhachHangUserDetails implements UserDetails {
    private final KhachHang khachHang;

    public KhachHangUserDetails(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Assign admin role for the seeded 'boss' account; others are regular users
        if ("boss".equalsIgnoreCase(khachHang.getUsername()) || "boss@alotra.com".equalsIgnoreCase(khachHang.getEmail())) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return khachHang.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return khachHang.getUsername();
    }

    // --- Expose properties for templates ---
    public Integer getId() {
        return khachHang.getId();
    }

    public String getFullName() {
        return khachHang.getFullName();
    }

    // Return null so Thymeleaf Elvis operator in template can fall back to default avatar
    public String getAvatarUrl() {
        return null; // You can map to a real field later if available
    }

    public RoleView getRole() {
        boolean isAdmin = getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new RoleView(isAdmin ? "Quản trị viên" : "Khách hàng");
    }

    public String getEmail() {
        return khachHang.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return khachHang.getStatus() == null || khachHang.getStatus() == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return khachHang.getStatus() == null || khachHang.getStatus() == 1;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    // Simple role view to satisfy template access currentUser.role.name
    public static class RoleView {
        private final String name;
        public RoleView(String name) { this.name = name; }
        public String getName() { return name; }
    }
}