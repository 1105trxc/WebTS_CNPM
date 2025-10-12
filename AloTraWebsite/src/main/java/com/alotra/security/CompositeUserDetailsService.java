package com.alotra.security;

import com.alotra.entity.KhachHang;
import com.alotra.entity.NhanVien;
import com.alotra.repository.KhachHangRepository;
import com.alotra.repository.NhanVienRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
@Primary
public class CompositeUserDetailsService implements UserDetailsService {
    private final NhanVienRepository nvRepo;
    private final KhachHangRepository khRepo;

    public CompositeUserDetailsService(NhanVienRepository nvRepo, KhachHangRepository khRepo) {
        this.nvRepo = nvRepo;
        this.khRepo = khRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String u = username != null ? username.trim() : "";
        // 0) Special bootstrap admin: "boss" (password "123"): take precedence to avoid DB conflicts
        if ("boss".equalsIgnoreCase(u) || "boss@alotra.com".equalsIgnoreCase(u)) {
            KhachHang ghost = new KhachHang();
            ghost.setUsername("boss");
            ghost.setEmail("boss@alotra.com");
            ghost.setFullName("AloTra Boss");
            ghost.setStatus(1); // active
            // Use local BCrypt to avoid depending on SecurityConfig's PasswordEncoder bean
            String hash = new BCryptPasswordEncoder().encode("123");
            ghost.setPasswordHash(hash);
            return new KhachHangUserDetails(ghost);
        }
        // 1) Try staff first (NhanVien): allow username or email
        NhanVien nv = nvRepo.findByUsername(u);
        if (nv == null) nv = nvRepo.findByEmail(u);
        if (nv != null) {
            return new NhanVienUserDetails(nv);
        }
        // 2) Fallback to customers (KhachHang): allow username or email
        KhachHang kh = khRepo.findByUsername(u);
        if (kh == null) kh = khRepo.findByEmail(u);
        if (kh != null) {
            return new KhachHangUserDetails(kh);
        }
        throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + username);
    }
}