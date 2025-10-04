package com.alotra.service;

import com.alotra.entity.KhachHang;
import com.alotra.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    // Added for admin Users module
    public List<KhachHang> findAll() { return khachHangRepository.findAll(); }
    public KhachHang findById(Integer id) { return khachHangRepository.findById(id).orElse(null); }

    // New search with optional filters
    public List<KhachHang> search(String kw, Integer status) {
        if (kw != null && kw.isBlank()) kw = null;
        return khachHangRepository.search(kw, status);
    }
}