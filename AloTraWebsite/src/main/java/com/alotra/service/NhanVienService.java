package com.alotra.service;

import com.alotra.entity.NhanVien;
import com.alotra.repository.NhanVienRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NhanVienService {
    private final NhanVienRepository repo;
    private final PasswordEncoder encoder;

    public NhanVienService(NhanVienRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public List<NhanVien> findAll() { return repo.findAll(); }
    public Optional<NhanVien> findById(Integer id) { return repo.findById(id); }

    // New: list active (not soft-deleted) employees
    public List<NhanVien> findActive() { return repo.findByDeletedAtIsNull(); }

    public NhanVien saveHandlingPassword(NhanVien nv) {
        boolean isNew = nv.getId() == null;
        String pwd = nv.getPlainPassword();
        if (isNew) {
            if (pwd == null || pwd.isBlank()) {
                throw new IllegalArgumentException("Mật khẩu không được để trống cho nhân viên mới");
            }
            nv.setPasswordHash(encoder.encode(pwd));
        } else {
            if (pwd != null && !pwd.isBlank()) {
                nv.setPasswordHash(encoder.encode(pwd));
            } else {
                // Preserve current hash from DB when not changing password
                NhanVien current = repo.findById(nv.getId()).orElseThrow();
                nv.setPasswordHash(current.getPasswordHash());
            }
        }
        return repo.save(nv);
    }

    public void deleteById(Integer id) { repo.deleteById(id); }

    public NhanVien findByUsername(String u){ return repo.findByUsername(u);}    
    public NhanVien findByEmail(String e){ return repo.findByEmail(e);}    
    public NhanVien findByPhone(String p){ return repo.findByPhone(p);}   

    // Search with optional filters
    public List<NhanVien> search(String kw, Integer role, Integer status) {
        if (kw != null && kw.isBlank()) kw = null;
        return repo.search(kw, role, status);
    }

    // Reset password to a random temporary one; returns plaintext temp password
    public String resetPassword(Integer id) {
        NhanVien nv = repo.findById(id).orElseThrow();
        String temp = generateTempPassword(10);
        nv.setPasswordHash(encoder.encode(temp));
        repo.save(nv);
        return temp;
    }

    private String generateTempPassword(int len) {
        final String dict = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%"; // avoid confusing chars
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(dict.charAt(r.nextInt(dict.length())));
        return sb.toString();
    }

    // Soft delete to trash: mark DeletedAt and lock account
    public void softDeleteToTrash(Integer id) {
        repo.findById(id).ifPresent(nv -> {
            nv.setDeletedAt(LocalDateTime.now());
            nv.setStatus(0);
            repo.save(nv);
        });
    }

    public void restoreFromTrash(Integer id) {
        repo.findById(id).ifPresent(nv -> {
            nv.setDeletedAt(null);
            repo.save(nv);
        });
    }
}