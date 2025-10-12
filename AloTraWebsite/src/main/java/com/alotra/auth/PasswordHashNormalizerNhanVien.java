package com.alotra.auth;

import com.alotra.entity.NhanVien;
import com.alotra.repository.NhanVienRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHashNormalizerNhanVien implements ApplicationRunner {
    private final NhanVienRepository repo;
    private final PasswordEncoder encoder;

    public PasswordHashNormalizerNhanVien(NhanVienRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        repo.findAll().forEach(nv -> {
            String hash = nv.getPasswordHash();
            if (hash == null) return;
            if (!hash.startsWith("$2a$") && !hash.startsWith("$2b$") && !hash.startsWith("$2y$")) {
                nv.setPasswordHash(encoder.encode(hash));
                repo.save(nv);
            }
        });
    }
}
