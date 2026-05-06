package com.itc.funkart.payment.security;

import com.itc.funkart.common.constants.auth.JwtClaims;
import com.itc.funkart.common.dto.user.JwtUserDto;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <h2>JwtWebFilterTest</h2>
 * <p>
 * Validates the security interceptor logic, ensuring tokens are extracted from
 * multiple sources and correctly mapped to the {@link JwtUserDto} principal.
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
    @DisplayName("Fast Pass: Should bypass JWT logic for Stripe Webhooks")
    void shouldBypassForWebhooks() throws Exception {
        // GIVEN: A request to the webhook endpoint
        request.setServletPath("/payments/webhook");
        request.addHeader("Authorization", "Bearer some-token");

        // WHEN
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // THEN: SecurityContext remains empty, and jwtService is never called
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).parseJwtToken(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Extraction: Should set JwtUserDto principal from Bearer Token")
    void shouldHandleBearerToken() throws Exception {
        // GIVEN
        String token = "valid-jwt";
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = Jwts.claims()
                .subject("42")
                .add(JwtClaims.ROLE, "ROLE_PAYMENT_ADMIN")
                .add(JwtClaims.NAME, "Abbas")
                .add(JwtClaims.EMAIL, "abbas@funkart.com")
                .build();

        when(jwtService.parseJwtToken(token)).thenReturn(claims);

        // WHEN
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // THEN
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);

        JwtUserDto principal = (JwtUserDto) auth.getPrincipal();
        assertEquals(42L, principal.id());
        assertEquals("ROLE_PAYMENT_ADMIN", principal.role());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Extraction: Should fallback to 'token' Cookie")
    void shouldHandleCookieToken() throws Exception {
        // GIVEN
        Cookie authCookie = new Cookie("token", "cookie-jwt");
        request.setCookies(authCookie);

        Claims claims = Jwts.claims()
                .subject("1")
                .add(JwtClaims.ROLE, "ROLE_USER")
                .build();

        when(jwtService.parseJwtToken("cookie-jwt")).thenReturn(claims);

        // WHEN
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).parseJwtToken("cookie-jwt");
    }

    @Test
    @DisplayName("Validation: Should ignore token if subject is missing")
    void shouldIgnoreTokenMissingSubject() throws Exception {
        // GIVEN
        request.addHeader("Authorization", "Bearer token-missing-sub");

        // Claims has Role but NO Subject (sub)
        Claims claims = Jwts.claims()
                .add(JwtClaims.ROLE, "ROLE_USER")
                .build();

        when(jwtService.parseJwtToken(anyString())).thenReturn(claims);

        // WHEN
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // THEN: Auth should not be set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Resilience: Should continue chain even if JWT service throws exception")
    void shouldContinueChainOnServiceError() throws Exception {
        // GIVEN
        request.addHeader("Authorization", "Bearer expired-token");
        when(jwtService.parseJwtToken(anyString())).thenThrow(new RuntimeException("Token Expired"));

        // WHEN
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // THEN: Chain continues, but user is unauthenticated (anonymous)
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Cleanup: Should clear context for requests with no auth")
    void shouldKeepEmptyContextForNoAuth() throws Exception {
        // WHEN
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).parseJwtToken(any());
    }
}