package com.itc.funkart.user.auth;

import com.itc.funkart.user.dto.security.UserPrincipalDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * <h2>JwtWebFilter — Unit Tests</h2>
 *
 * <p>Validates the three-stage pipeline inside {@link JwtAuthWebFilter}:
 * <ol>
 *   <li>Token extraction from Authorization header or cookie</li>
 *   <li>Claim parsing via {@link JwtService}</li>
 *   <li>Principal reconstruction via {@link PrincipalFactory} → sets {@link UserPrincipalDto}</li>
 * </ol>
 *
 * <p><b>Key fix from old tests:</b>
 * <ul>
 *   <li>The filter constructor is {@code JwtWebFilter(JwtService, PrincipalFactory)} —
 *       both must be injected. The old tests only injected {@code JwtService}.</li>
 *   <li>The SecurityContext principal is a {@link UserPrincipalDto} —
 *       NOT a {@code JwtUserDto}. The old tests asserted against the wrong type.</li>
 *   <li>When validation fails the filter <b>continues the chain</b> (stateless, never
 *       blocks) — this invariant is explicitly tested.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthWebFilterTest {

    @Mock private JwtService       jwtService;
    @Mock private PrincipalFactory principalFactory;
    @Mock private HttpServletRequest  request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain         filterChain;

    private JwtAuthWebFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthWebFilter(jwtService, principalFactory);
    }

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Claims buildClaims(String subject) {
        return Jwts.claims().subject(subject).build();
    }

    private UserPrincipalDto buildPrincipal(Long id, String email) {
        return UserPrincipalDto.builder()
                .userId(id).name("Alice").email(email).role("ROLE_USER").build();
    }

    // -------------------------------------------------------------------------
    // Authorization Bearer header
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Authorization Bearer header")
    class BearerHeaderTests {

        @Test
        @DisplayName("Populates SecurityContext with UserPrincipalDto when token is valid")
        void validBearerToken_setsAuthentication() throws Exception {
            String token = "valid.jwt.token";
            Claims claims = buildClaims("1");
            UserPrincipalDto principal = buildPrincipal(1L, "alice@example.com");

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.parseJwtToken(token)).thenReturn(claims);
            when(principalFactory.fromClaims(claims)).thenReturn(principal);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipalDto.class);
            assertThat(((UserPrincipalDto) auth.getPrincipal()).email())
                    .isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("Filter chain always continues after valid token")
        void validBearerToken_continuesChain() throws Exception {
            String token = "valid.jwt.token";
            Claims claims = buildClaims("1");
            UserPrincipalDto principal = buildPrincipal(1L, "alice@example.com");

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.parseJwtToken(token)).thenReturn(claims);
            when(principalFactory.fromClaims(claims)).thenReturn(principal);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Role from principal is set as granted authority")
        void setsGrantedAuthority() throws Exception {
            String token = "valid.jwt.token";
            Claims claims = buildClaims("1");
            UserPrincipalDto principal = UserPrincipalDto.builder()
                    .userId(1L).name("Alice").email("alice@example.com")
                    .role("ROLE_ADMIN").build();

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.parseJwtToken(token)).thenReturn(claims);
            when(principalFactory.fromClaims(claims)).thenReturn(principal);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
    }

    // -------------------------------------------------------------------------
    // Cookie extraction
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Cookie token extraction")
    class CookieExtractionTests {

        @Test
        @DisplayName("Populates SecurityContext when valid token is in cookie")
        void validCookieToken_setsAuthentication() throws Exception {
            String token = "cookie.jwt.token";
            Cookie tokenCookie = new Cookie("token", token);
            Claims claims = buildClaims("2");
            UserPrincipalDto principal = buildPrincipal(2L, "bob@example.com");

            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
            when(jwtService.parseJwtToken(token)).thenReturn(claims);
            when(principalFactory.fromClaims(claims)).thenReturn(principal);

            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(((UserPrincipalDto) auth.getPrincipal()).email())
                    .isEqualTo("bob@example.com");
        }

        @Test
        @DisplayName("Filter chain continues after cookie token authentication")
        void validCookieToken_continuesChain() throws Exception {
            String token = "cookie.jwt.token";
            Cookie tokenCookie = new Cookie("token", token);
            Claims claims = buildClaims("2");
            UserPrincipalDto principal = buildPrincipal(2L, "bob@example.com");

            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{tokenCookie});
            when(jwtService.parseJwtToken(token)).thenReturn(claims);
            when(principalFactory.fromClaims(claims)).thenReturn(principal);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // -------------------------------------------------------------------------
    // No token present
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No token present")
    class NoTokenTests {

        @Test
        @DisplayName("SecurityContext remains empty when no token is provided")
        void noToken_contextIsEmpty() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Filter chain still continues with no token")
        void noToken_continuesChain() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }
    }

    // -------------------------------------------------------------------------
    // Invalid / expired token
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Invalid or expired token")
    class InvalidTokenTests {

        @Test
        @DisplayName("SecurityContext remains empty when token parsing fails")
        void invalidToken_contextIsEmpty() throws Exception {
            String badToken = "garbage.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + badToken);
            when(request.getRequestURI()).thenReturn("/api/v1/users/me");
            when(jwtService.parseJwtToken(badToken))
                    .thenThrow(new RuntimeException("Expired signature"));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Filter chain still continues even when token is invalid (stateless — never blocks)")
        void invalidToken_continuesChain() throws Exception {
            String badToken = "garbage.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + badToken);
            when(request.getRequestURI()).thenReturn("/api/v1/users/me");
            when(jwtService.parseJwtToken(badToken))
                    .thenThrow(new RuntimeException("Expired signature"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("SecurityContext stays empty when PrincipalFactory returns null")
        void nullPrincipal_contextIsEmpty() throws Exception {
            String token = "valid.jwt.token";
            Claims claims = buildClaims("1");

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(request.getRequestURI()).thenReturn("/api/v1/users/me");
            when(jwtService.parseJwtToken(token)).thenReturn(claims);
            when(principalFactory.fromClaims(claims)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}