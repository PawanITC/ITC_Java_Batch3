package com.itc.funkart.user.auth;

import com.itc.funkart.user.dto.security.UserPrincipalDto;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PrincipalFactory — Unit Tests</h2>
 *
 * <p>Validates the two identity-mapping paths:
 * <ul>
 *   <li>{@code create(User)} — domain entity → {@link UserPrincipalDto}</li>
 *   <li>{@code fromClaims(Claims)} — JWT claims → {@link UserPrincipalDto}</li>
 * </ul>
 *
 * <p>Also exercises the internal role normalization and whitelist gate
 * ({@code ROLE_USER}, {@code ROLE_MODERATOR}, {@code ROLE_ADMIN} are allowed;
 * everything else must throw).
 *
 * <p>No Spring context is required — {@link PrincipalFactory} is a plain
 * {@code @Component} with no injected dependencies.
 */
class PrincipalFactoryTest {

    private PrincipalFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PrincipalFactory();
    }

    // -------------------------------------------------------------------------
    // create(User)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create(User)")
    class CreateFromUserTests {

        @Test
        @DisplayName("Returns null when user is null")
        void nullUser_returnsNull() {
            assertNull(factory.create(null));
        }

        @Test
        @DisplayName("Maps userId from User.id")
        void mapsUserId() {
            User user = User.builder().id(7L).name("Alice")
                    .email("alice@example.com").role(Role.ROLE_USER).build();
            assertEquals(7L, factory.create(user).userId());
        }

        @Test
        @DisplayName("Maps name from User.name")
        void mapsName() {
            User user = User.builder().id(1L).name("Alice")
                    .email("alice@example.com").role(Role.ROLE_USER).build();
            assertEquals("Alice", factory.create(user).name());
        }

        @Test
        @DisplayName("Maps email from User.email")
        void mapsEmail() {
            User user = User.builder().id(1L).name("Alice")
                    .email("alice@example.com").role(Role.ROLE_USER).build();
            assertEquals("alice@example.com", factory.create(user).email());
        }

        @Test
        @DisplayName("Role is ROLE_USER for standard users")
        void roleUser() {
            User user = User.builder().id(1L).name("Alice")
                    .email("alice@example.com").role(Role.ROLE_USER).build();
            assertEquals("ROLE_USER", factory.create(user).role());
        }

        @Test
        @DisplayName("Role is ROLE_ADMIN for admin users")
        void roleAdmin() {
            User user = User.builder().id(1L).name("Alice")
                    .email("alice@example.com").role(Role.ROLE_ADMIN).build();
            assertEquals("ROLE_ADMIN", factory.create(user).role());
        }

        @Test
        @DisplayName("Role is ROLE_MODERATOR for moderator users")
        void roleModerator() {
            User user = User.builder().id(1L).name("Alice")
                    .email("alice@example.com").role(Role.ROLE_MODERATOR).build();
            assertEquals("ROLE_MODERATOR", factory.create(user).role());
        }

        @Test
        @DisplayName("Defaults to ROLE_USER when User.role is null")
        void nullRole_defaultsToRoleUser() {
            User user = User.builder().id(1L).name("Alice")
                    .email("alice@example.com").role(null).build();
            assertEquals("ROLE_USER", factory.create(user).role());
        }
    }

    // -------------------------------------------------------------------------
    // fromClaims(Claims)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("fromClaims(Claims)")
    class FromClaimsTests {

        /** Builds minimal in-memory Claims without signing. */
        private Claims claims(String subject, Map<String, Object> extra) {
            var builder = Jwts.claims().subject(subject);
            extra.forEach(builder::add);
            return builder.build();
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException when claims is null")
        void nullClaims_throws() {
            assertThrows(JwtAuthenticationException.class,
                    () -> factory.fromClaims(null));
        }

        @Test
        @DisplayName("Maps userId from subject claim")
        void mapsUserIdFromSubject() {
            Claims c = claims("42", Map.of("name", "Alice",
                    "email", "alice@example.com", "role", "ROLE_USER"));
            assertEquals(42L, factory.fromClaims(c).userId());
        }

        @Test
        @DisplayName("Maps name from name claim")
        void mapsNameClaim() {
            Claims c = claims("1", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", "ROLE_USER"));
            assertEquals("Bob", factory.fromClaims(c).name());
        }

        @Test
        @DisplayName("Maps email from email claim")
        void mapsEmailClaim() {
            Claims c = claims("1", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", "ROLE_USER"));
            assertEquals("bob@example.com", factory.fromClaims(c).email());
        }

        @Test
        @DisplayName("Maps role from role claim")
        void mapsRoleClaim() {
            Claims c = claims("1", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", "ROLE_ADMIN"));
            assertEquals("ROLE_ADMIN", factory.fromClaims(c).role());
        }

        @Test
        @DisplayName("Throws when subject is not a numeric string")
        void nonNumericSubject_throws() {
            Claims c = claims("not-a-number", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", "ROLE_USER"));
            assertThrows(JwtAuthenticationException.class, () -> factory.fromClaims(c));
        }

        @Test
        @DisplayName("Throws when subject is null")
        void nullSubject_throws() {
            // Jwts.claims() without a subject sets it to null
            Claims c = Jwts.claims()
                    .add("name", "Bob")
                    .add("email", "bob@example.com")
                    .add("role", "ROLE_USER")
                    .build();
            assertThrows(JwtAuthenticationException.class, () -> factory.fromClaims(c));
        }

        @Test
        @DisplayName("Throws when name claim is absent")
        void missingName_throws() {
            Claims c = claims("1", Map.of(
                    "email", "bob@example.com", "role", "ROLE_USER"));
            assertThrows(JwtAuthenticationException.class, () -> factory.fromClaims(c));
        }

        @Test
        @DisplayName("Throws when email claim is absent")
        void missingEmail_throws() {
            Claims c = claims("1", Map.of(
                    "name", "Bob", "role", "ROLE_USER"));
            assertThrows(JwtAuthenticationException.class, () -> factory.fromClaims(c));
        }

        @Test
        @DisplayName("Prepends ROLE_ prefix when role claim lacks it")
        void addsPrefixWhenMissing() {
            Claims c = claims("1", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", "USER"));
            assertEquals("ROLE_USER", factory.fromClaims(c).role());
        }

        @Test
        @DisplayName("Defaults role to ROLE_USER when role claim is blank")
        void blankRole_defaultsToRoleUser() {
            Claims c = claims("1", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", ""));
            assertEquals("ROLE_USER", factory.fromClaims(c).role());
        }

        @Test
        @DisplayName("Throws JwtAuthenticationException for a role not in the whitelist")
        void unauthorisedRole_throws() {
            Claims c = claims("1", Map.of("name", "Bob",
                    "email", "bob@example.com", "role", "ROLE_SUPERUSER"));
            assertThrows(JwtAuthenticationException.class, () -> factory.fromClaims(c));
        }
    }
}