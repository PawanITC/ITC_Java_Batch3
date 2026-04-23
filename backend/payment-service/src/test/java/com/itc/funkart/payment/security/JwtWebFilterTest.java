package com.itc.funkart.payment.security;

import com.itc.funkart.payment.auth.claims.JwtClaims;
import com.itc.funkart.payment.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

/**
 * <h2>JwtWebFilterTest</h2>
 * <p>
 * Tests the security interceptor logic. Ensures that tokens from various
 * request sources (Headers, Cookies) are correctly translated into
 * Spring Security Authentication objects.
 * </p>
 */
class JwtWebFilterTest {

    private JwtService jwtService;
    private JwtWebFilter jwtWebFilter;
    private FilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        jwtWebFilter = new JwtWebFilter(jwtService);
        filterChain = mock(FilterChain.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should extract Bearer token and set SecurityContext")
    void shouldHandleBearerToken() throws Exception {
        String token = "mock-token";
        request.addHeader("Authorization", "Bearer " + token);

        // Using the builder instead of internal 'impl' classes
        Claims claims = Jwts.claims()
                .subject("1")
                .add(JwtClaims.ROLE, "ROLE_USER")
                .add(JwtClaims.NAME, "Abbas")
                .build();

        when(jwtService.parseJwtToken(token)).thenReturn(claims);

        jwtWebFilter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should fallback to Cookie if Authorization header is missing")
    void shouldHandleCookieToken() throws Exception {
        // Arrange
        Cookie cookie = new Cookie("token", "cookie-token");
        request.setCookies(cookie);

        // Using the builder to create the interface implementation
        Claims claims = Jwts.claims()
                .subject("1")
                .add(JwtClaims.ROLE, "ROLE_USER")
                .build();

        when(jwtService.parseJwtToken("cookie-token")).thenReturn(claims);

        // Act
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Authentication should be set from Cookie");
        verify(jwtService).parseJwtToken("cookie-token");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain if token is invalid")
    void shouldContinueChainOnFailure() throws Exception {
        request.addHeader("Authorization", "Bearer invalid");
        when(jwtService.parseJwtToken(anyString())).thenThrow(new RuntimeException("Invalid"));

        jwtWebFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip authentication if Authorization header format is wrong")
    void shouldIgnoreInvalidHeaderFormat() throws Exception {
        // Header exists but doesn't start with "Bearer "
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtWebFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).parseJwtToken(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should ignore cookies with different names")
    void shouldIgnoreIrrelevantCookies() throws Exception {
        Cookie otherCookie = new Cookie("sessionID", "12345");
        request.setCookies(otherCookie);

        jwtWebFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).parseJwtToken(any());
    }

    @Test
    @DisplayName("Should discard token if mandatory claims (sub/role) are missing")
    void shouldRejectTokenMissingClaims() throws Exception {
        String token = "valid-sig-bad-data";
        request.addHeader("Authorization", "Bearer " + token);

        // Valid signature, but the payload is missing the ROLE
        Claims claims = Jwts.claims()
                .subject("1")
                // Missing JwtClaims.ROLE
                .build();

        when(jwtService.parseJwtToken(token)).thenReturn(claims);

        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // Should NOT set authentication if claims are incomplete
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}