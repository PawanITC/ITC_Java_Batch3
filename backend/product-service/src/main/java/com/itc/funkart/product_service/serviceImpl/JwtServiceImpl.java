package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.config.JwtConfig;
import com.itc.funkart.product_service.constants.JwtClaims;
import com.itc.funkart.product_service.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Implementation of the {@link JwtService} providing cryptographic operations for JWTs.
 * <p>
 * This service handles the decoding of the shared secret key and uses it to verify
 * the integrity of tokens issued by the FunKart Auth Authority. It is designed to be
 * stateless and performant for use within the security filter chain.
 * </p>
 */
@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final SecretKey key;

    /**
     * Initializes the service and prepares the HMAC-SHA signing key.
     *
     * @param jwtConfig Configuration properties containing the Base64 secret.
     */
    public JwtServiceImpl(JwtConfig jwtConfig) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getSecret());
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode JWT secret. Ensure it is a valid Base64 string.");
            throw e;
        }
    }

    /**
     * Parses a raw JWT string and validates its signature and issuer.
     * <p>
     * Utilizes the JJWT 0.12.x fluent API to build a parser that strictly enforces
     * the {@link JwtClaims#ISSUER} contract.
     * </p>
     *
     * @param token The raw Bearer or Cookie token string.
     * @return {@link Claims} payload if validation is successful.
     * @throws io.jsonwebtoken.JwtException if the token is invalid or the issuer does not match.
     */
    @Override
    public Claims parseJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtClaims.ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if a token is cryptographically valid and currently active.
     *
     * @param token The raw JWT string to evaluate.
     * @return {@code true} if valid and not expired; {@code false} otherwise.
     */
    @Override
    public boolean validateToken(String token) {
        try {
            Claims claims = parseJwtToken(token);
            boolean isExpired = claims.getExpiration().before(new Date());

            if (isExpired) {
                log.debug("JWT token is expired");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}