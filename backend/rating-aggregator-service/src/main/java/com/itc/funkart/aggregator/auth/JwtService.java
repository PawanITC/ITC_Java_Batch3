package com.itc.funkart.aggregator.auth;

import com.itc.funkart.common.constants.auth.JwtClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(JwtConfig jwtConfig) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtClaims.ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
