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
@Table(name = "oauth_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** * Foreign reference to the {@link User#getId()}.
     * Stored as a plain Long to maintain service flexibility.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The OAuth provider name (e.g., "GitHub", "google"). */
    @Column(nullable = false)
    private String provider;

    /** The unique identifier provided by the external OAuth service. */
    @Column(nullable = false, unique = true)
    private String providerId;
}