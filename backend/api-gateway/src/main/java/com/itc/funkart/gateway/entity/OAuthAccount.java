package com.itc.funkart.gateway.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "oauth_accounts")
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // store user-service user ID as plain Long

    @Column(nullable = false)
    private String provider; // e.g., "github", "google"

    @Column(nullable = false, unique = true)
    private String providerId; // OAuth provider ID

    public OAuthAccount() {}

    public OAuthAccount(Long userId, String provider, String providerId) {
        this.userId = userId;
        this.provider = provider;
        this.providerId = providerId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
}