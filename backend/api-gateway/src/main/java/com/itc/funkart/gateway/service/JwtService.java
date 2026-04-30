package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.auth.jwt.JwtClaims;
import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(AppConfig appConfig) {
        String rawSecret = appConfig.jwt().secret();
        if (rawSecret == null) {
            throw new RuntimeException("JWT Secret is missing from configuration!");
        }

        byte[] keyBytes = java.util.Base64.getDecoder().decode(rawSecret.trim());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(JwtClaims.ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("JWT expired", e);

        } catch (JwtException e) {
            throw new JwtAuthenticationException("Invalid JWT", e);
        }
    }

    public void validateClaims(Claims claims) {

        // Expiration validation (critical missing piece before)
        Date expiration = claims.getExpiration();

        if (expiration == null) {
            throw new JwtAuthenticationException("Missing expiration claim");
        }

        if (expiration.before(new Date())) {
            throw new JwtAuthenticationException("JWT token is expired");
        }
    }

    public String generateAccessToken(Long userId, String role) {
        return Jwts.builder()
                .issuer(JwtClaims.ISSUER)
                .subject(String.valueOf(userId))
                .claim(JwtClaims.ROLE, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(key)
                .compact();
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }
}
