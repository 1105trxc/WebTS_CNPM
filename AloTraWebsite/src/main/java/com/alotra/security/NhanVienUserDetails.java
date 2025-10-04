package com.alotra.security;

import com.alotra.entity.NhanVien;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class NhanVienUserDetails implements UserDetails {
    private final NhanVien nv;

    public NhanVienUserDetails(NhanVien nv) { this.nv = nv; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // VaiTro: 1 -> ADMIN (chủ cửa hàng), 2 -> VENDOR (nhân viên)
        if (nv.getRole() != null && nv.getRole() == 1) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_VENDOR"));
    }

    @Override
    public String getPassword() { return nv.getPasswordHash(); }

    @Override
    public String getUsername() { return nv.getUsername(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return nv.getStatus() == null || nv.getStatus() == 1; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return nv.getStatus() == null || nv.getStatus() == 1; }

    // Expose for templates similar to KhachHangUserDetails
    public Integer getId() { return nv.getId(); }
    public String getFullName() { return nv.getFullName(); }
    public String getEmail() { return nv.getEmail(); }
    public String getAvatarUrl() { return null; }

    public RoleView getRole() {
        boolean isAdmin = getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new RoleView(isAdmin ? "Quản trị viên" : "Nhân viên");
    }

    public static class RoleView {
        private final String name;
        public RoleView(String name) { this.name = name; }
        public String getName() { return name; }
    }
}
