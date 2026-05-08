package com.itc.funkart.config;

import com.itc.funkart.common.constants.auth.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
@Slf4j
public class JwtService {

    private final SecretKey key;

    public JwtService(JwtConfig jwtConfig) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            log.error("CRITICAL: JWT Secret is not valid Base64. Check your environment variables.");
            throw e;
        }
    }

    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtClaims.ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
