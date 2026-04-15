package com.itc.funkart.user.service;

import com.itc.funkart.user.config.JwtConfig;
import com.itc.funkart.user.dto.user.JwtUserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Service responsible for the lifecycle of JSON Web Tokens (JWT).
 * Unlike the API Gateway which only validates, the User-Service owns the
 * generation and signing of tokens using {@link JwtConfig}.
 */
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey key;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // Decode the Base64 secret from config to create the signing key
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Encodes user identity and metadata into a signed JWT string.
     * * @param user The {@link JwtUserDto} containing {@code id}, {@code name}, and {@code email}.
     * @return A compact, URL-safe JWT string.
     */
    public String generateJwtToken(JwtUserDto user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(user.id().toString())
                .issuer("funkart-user-service")
                .claim("name", user.name())
                .claim("email", user.email())
                .claim("role", user.role())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Parses the provided token and verifies its signature against the internal {@code SecretKey}.
     * * @param token The JWT string to parse.
     * @return The {@link Claims} payload if the signature is valid.
     * @throws io.jsonwebtoken.JwtException if the token is tampered with or malformed.
     */
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates the token's structural integrity and checks if the {@code exp} claim is still in the future.
     * * @param token The JWT string.
     * @return {@code true} if the token is currently valid; {@code false} otherwise.
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
     * Convenience method to retrieve the primary user identifier from a valid token.
     * * @param token The JWT string.
     * @return The user ID as a {@link Long}.
     */
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseJwtToken(token).getSubject());
    }
}