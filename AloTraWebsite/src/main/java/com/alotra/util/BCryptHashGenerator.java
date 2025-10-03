package com.alotra.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptHashGenerator {
    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123";
        String hash = new BCryptPasswordEncoder().encode(raw);
        System.out.println("BCrypt hash for '" + raw + "': " + hash);
    }
}
