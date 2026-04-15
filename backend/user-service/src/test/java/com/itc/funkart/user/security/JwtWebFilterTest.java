package com.itc.funkart.user.security;

import com.itc.funkart.user.dto.user.JwtUserDto;
import com.itc.funkart.user.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtWebFilter}.
 * Verifies token extraction from headers/cookies and ensures the SecurityContext
 * is correctly managed even when validation fails.
 */
@ExtendWith(MockitoExtension.class)
class JwtWebFilterTest {

    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private Claims claims;

    @InjectMocks
    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Filter: Should authenticate when valid Bearer token is provided")
    void doFilterInternal_ValidHeaderToken_SetsAuthentication() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.parseJwtToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("101");
        when(claims.get("name", String.class)).thenReturn("Tester");
        when(claims.get("email", String.class)).thenReturn("test@example.com");

        // Act
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((JwtUserDto) auth.getPrincipal()).email()).isEqualTo("test@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter: Should authenticate when valid cookie token is provided")
    void doFilterInternal_ValidCookieToken_SetsAuthentication() throws Exception {
        // Arrange
        Cookie tokenCookie = new Cookie("token", "cookie.jwt.token");
        when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
        when(jwtService.parseJwtToken("cookie.jwt.token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("102");

        // Act
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter: Should hit catch block and continue chain when token is invalid")
    void doFilterInternal_InvalidToken_HandlesException() throws Exception {
        // Arrange
        String badToken = "garbage.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + badToken);
        when(request.getRequestURI()).thenReturn("/api/v1/users/me");

        // This triggers the 'catch (Exception ex)' block
        when(jwtService.parseJwtToken(badToken)).thenThrow(new RuntimeException("Expired Signature"));

        // Act
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // Assert
        // SecurityContext should NOT be set
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // Critical: The chain MUST still continue
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter: Should skip authentication when no token is present")
    void doFilterInternal_NoToken_ContinuesChain() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        // Act
        jwtWebFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}