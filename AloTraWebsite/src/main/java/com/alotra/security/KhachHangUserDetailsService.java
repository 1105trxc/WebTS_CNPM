package com.alotra.security;

import com.alotra.entity.KhachHang;
import com.alotra.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class KhachHangUserDetailsService implements UserDetailsService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        KhachHang kh = khachHangRepository.findByUsername(username);
        if (kh == null) {
            // Also allow login by email
            kh = khachHangRepository.findByEmail(username);
        }
        if (kh == null) {
            throw new UsernameNotFoundException("Không tìm thấy tài khoản: " + username);
        }
        return new KhachHangUserDetails(kh);
    }
}
