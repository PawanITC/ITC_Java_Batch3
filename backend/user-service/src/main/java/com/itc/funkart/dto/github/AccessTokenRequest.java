package com.itc.funkart.dto.github;
// -----------------------
// DTO for GitHub API
// -----------------------
public record AccessTokenRequest(String client_id, String client_secret, String code, String redirect_uri) {}

