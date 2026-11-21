package com.gallery.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long accessMinutes;
    private final long deviceAccessMinutes;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.access-token-minutes}") long accessMinutes,
            @Value("${app.security.jwt.device-access-token-minutes}") long deviceAccessMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            // pad deterministically for dev only; production must supply strong secret
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            for (int i = bytes.length; i < 32; i++) padded[i] = (byte) (i * 31);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
        this.deviceAccessMinutes = deviceAccessMinutes;
    }

    public String generateAdminAccessToken(Long adminId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .claims(Map.of("typ", "ADMIN", "aid", adminId, "role", role))
                .signWith(key)
                .compact();
    }

    public String generateDeviceAccessToken(Long deviceId, String deviceUuid) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(deviceUuid)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(deviceAccessMinutes, ChronoUnit.MINUTES)))
                .claims(Map.of("typ", "DEVICE", "did", deviceId))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
