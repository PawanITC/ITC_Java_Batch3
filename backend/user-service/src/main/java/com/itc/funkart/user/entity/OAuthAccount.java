package com.itc.funkart.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an external OAuth connection (e.g., GitHub) for a user.
 */
@Entity
@Table(name = "oauth_accounts", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "provider"})})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The actual User object.
     * FetchType.LAZY means we only load the user details if we explicitly ask for them.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The OAuth provider name (e.g., "GitHub", "google").
     */
    @Column(nullable = false)
    private String provider;

    /**
     * The unique identifier provided by the external OAuth service.
     */
    @Column(nullable = false, unique = true)
    private String providerId;
}