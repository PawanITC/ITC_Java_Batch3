package com.itc.funkart.user.dto.github;
// -----------------------
// DTO for GitHub API
// -----------------------
public record GithubUser(
        Long id,
        String login,
        String name,
        String email
) {}
