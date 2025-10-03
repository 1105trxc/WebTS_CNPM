package com.alotra.bootstrap;

import com.alotra.entity.KhachHang;
import com.alotra.repository.KhachHangRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensure an admin account exists for initial access.
 * Username: boss, Password: 123
 */
@Component
public class CreateAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CreateAdminInitializer.class);

    private final KhachHangRepository repo;
    private final PasswordEncoder encoder;

    public CreateAdminInitializer(KhachHangRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // If a user with username 'boss' already exists, do nothing
        KhachHang existing = repo.findByUsername("boss");
        if (existing != null) {
            log.info("Admin account 'boss' already exists (id={}).", existing.getId());
            return;
        }
        // Also guard by email in case someone created it with email.
        if (repo.findByEmail("boss@alotra.com") != null) {
            log.info("An account with email boss@alotra.com already exists; skipping admin seed.");
            return;
        }
        KhachHang admin = new KhachHang();
        admin.setUsername("boss");
        admin.setFullName("AloTra Administrator");
        admin.setEmail("boss@alotra.com");
        admin.setPhone("0900000000");
        admin.setStatus(1);
        admin.setPasswordHash(encoder.encode("123"));
        repo.save(admin);
        log.warn("Seeded default admin account: username='boss', password='123'. Please change the password after first login.");
    }
}
