package com.itc.funkart.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // hashed password for traditional login

    private String name;

    // One user can have multiple OAuth accounts (GitHub, Google, etc.)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OAuthAccount> oauthAccounts = new HashSet<>();

    public User() {}

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<OAuthAccount> getOauthAccounts() { return oauthAccounts; }
    public void addOAuthAccount(OAuthAccount account) {
        oauthAccounts.add(account);
        account.setUser(this);
    }
    public void removeOAuthAccount(OAuthAccount account) {
        oauthAccounts.remove(account);
        account.setUser(null);
    }
}