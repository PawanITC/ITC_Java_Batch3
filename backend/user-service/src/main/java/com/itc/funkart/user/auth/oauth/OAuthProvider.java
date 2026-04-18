package com.itc.funkart.user.auth.oauth;

public enum OAuthProvider {
    GITHUB("github");

    private final String value;

    OAuthProvider(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}