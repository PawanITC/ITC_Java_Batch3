package com.itc.funkart.user.dto.github;

import lombok.Builder;

/**
 * Data carrier for the GitHub user profile information.
 * * @param id    GitHub's internal, immutable unique numeric identifier.
 *
 * @param login The user's GitHub username (handle).
 * @param name  The user's public-facing display name (can be {@code null}).
 * @param email The user's primary public email (can be {@code null} based on privacy settings).
 */
@Builder
public record GithubUser(
        Long id,
        String login,
        String name,
        String email
) {}