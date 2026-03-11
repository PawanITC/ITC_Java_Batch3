package com.itc.funkart.dto.github;
// -----------------------
// DTO for GitHub API
// -----------------------
public record AccessTokenResponse(String access_token, String scope, String token_type) {}

