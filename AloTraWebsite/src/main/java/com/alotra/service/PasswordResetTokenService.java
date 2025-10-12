package com.alotra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class PasswordResetTokenService {
    private final String secret;
    private final long expireMinutes;

    public PasswordResetTokenService(
            @Value("${password.reset.secret:change-me}") String secret,
            @Value("${password.reset.expire-minutes:15}") long expireMinutes) {
        this.secret = secret;
        this.expireMinutes = expireMinutes;
    }

    public String generateToken(String email) {
        long exp = Instant.now().getEpochSecond() + expireMinutes * 60;
        String emailB64 = base64Url(email.getBytes(StandardCharsets.UTF_8));
        String payload = emailB64 + "." + exp;
        String sig = hmac(payload);
        return payload + "." + sig;
    }

    public String validateAndExtractEmail(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        String emailB64 = parts[0];
        String expStr = parts[1];
        String sig = parts[2];
        long now = Instant.now().getEpochSecond();
        long exp;
        try { exp = Long.parseLong(expStr); } catch (NumberFormatException e) { return null; }
        if (exp < now) return null;
        String expectedSig = hmac(emailB64 + "." + expStr);
        if (!constantTimeEquals(sig, expectedSig)) return null;
        try {
            byte[] emailBytes = Base64.getUrlDecoder().decode(emailB64);
            return new String(emailBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64Url(raw);
        } catch (Exception e) {
            throw new RuntimeException("HMAC error", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) {
            res |= a.charAt(i) ^ b.charAt(i);
        }
        return res == 0;
    }
}
