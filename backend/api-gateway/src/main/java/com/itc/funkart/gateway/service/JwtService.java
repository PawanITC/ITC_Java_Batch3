package com.itc.funkart.gateway.service;

import com.itc.funkart.gateway.config.AppConfig;
import com.itc.funkart.gateway.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private static final String ISSUER = "funkart-gateway";

    public JwtService(AppConfig appConfig) {
        this.key = Keys.hmacShaKeyFor(
                appConfig.jwt().secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
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
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(key)
                .compact();
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }
}
