package com.itc.funkart.service;

import com.itc.funkart.config.JwtConfig;
import com.itc.funkart.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final Key key;  // use same key for signing and parsing

    public JwtService(JwtConfig jwtConfig , @Value("${JWT_SECRET}") String jwtSecret) {
        this.jwtConfig = jwtConfig;

        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateJwtToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());
        return Jwts.builder()
                .setSubject(user.getId().toString())   // store user id
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    // Parse JWT token
    public Claims parseJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)  // <- use same key
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseJwtToken(token).getSubject());
    }
}