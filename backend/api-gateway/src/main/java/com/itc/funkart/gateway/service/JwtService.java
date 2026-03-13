package com.itc.funkart.gateway.service;
import com.itc.funkart.gateway.config.JwtConfig;
import com.itc.funkart.gateway.dto.JwtUserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final Key key;  // use same key for signing and parsing

    public JwtService(JwtConfig jwtConfig) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtConfig = jwtConfig;
    }

    public String generateJwtToken(JwtUserDto user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationMs());
        return Jwts.builder()
                .setSubject(user.id().toString())   // store user id
                .claim("name", user.name())
                .claim("email", user.email())
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
