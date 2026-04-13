package com.itc.funkart.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Core User entity representing a registered user in the Funkart system.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("JpaDataSourceORMInspection")
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
}