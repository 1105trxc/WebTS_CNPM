package com.alotra.service;

import com.alotra.entity.KhachHang;
import com.alotra.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KhachHangService {
    @Autowired
    private KhachHangRepository khachHangRepository;

    public KhachHang findByEmail(String email) {
        return khachHangRepository.findByEmail(email);
    }
    public KhachHang findByUsername(String username) {
        return khachHangRepository.findByUsername(username);
    }
    public KhachHang findByPhone(String phone) {
        return khachHangRepository.findByPhone(phone);
    }
    public KhachHang save(KhachHang khachHang) {
        return khachHangRepository.save(khachHang);
    }
}
