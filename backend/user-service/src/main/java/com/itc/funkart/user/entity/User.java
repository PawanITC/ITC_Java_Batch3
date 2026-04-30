package com.itc.funkart.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core User entity representing a registered user in the Funkart system.
 * <p>
 * Holds authentication credentials and authorization roles.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique email address used for identification and login. */
    @Column(unique = true, nullable = false)
    private String email;

    /** The Bcrypt hashed password. Will be {@code null} for pure OAuth users. */
    private String password;

    /** The display name of the user. */
    private String name;

    /** The security clearance level of the user.
     * Defaults to ROLE_USER to follow the Principle of Least Privilege.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    /**
     * One User can have multiple OAuth providers (GitHub, Google, etc.)
     * 'mappedBy' points to the field name in the OAuthAccount class.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OAuthAccount> oauthAccounts = new ArrayList<>();

    @Column(updatable = false)
    private Instant createdAt;


    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}