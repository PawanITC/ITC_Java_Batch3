package com.itc.funkart.payment.service;

import com.itc.funkart.payment.config.JwtConfig;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Service for generating and validating JWT tokens.
 * User-Service owns JWT generation (unlike API Gateway which only validates).
 */
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey key;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a new JWT token from user claims
     */
    public String generateJwtToken(JwtUserDto user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(user.id().toString())
                .issuer("funkart-user-service")
                .claim("name", user.name())
                .claim("email", user.email())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate JWT claims
     */
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract user ID from token
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseJwtToken(token).getSubject());
    }
}