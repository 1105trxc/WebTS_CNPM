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
        // 1) Try staff first (NhanVien): allow username or email
        NhanVien nv = nvRepo.findByUsername(username);
        if (nv == null) nv = nvRepo.findByEmail(username);
        if (nv != null) {
            return new NhanVienUserDetails(nv);
        }
        // 2) Fallback to customers (KhachHang): allow username or email
        KhachHang kh = khRepo.findByUsername(username);
        if (kh == null) kh = khRepo.findByEmail(username);
        if (kh != null) {
            return new KhachHangUserDetails(kh);
        }
        throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + username);
    }
}
