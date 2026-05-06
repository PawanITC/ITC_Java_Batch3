import React, { createContext, useState, useContext, useEffect } from 'react';
import { authApi } from '@/lib/authApi';  // your Spring Boot /api/v1/auth/me

const checkUserAuth = async () => {
    const currentUser = await authApi.me();  // HttpOnly cookie auth
    setUser(currentUser);
    setIsAuthenticated(true);
};

const logout = async () => {
    await fetch('/api/v1/oauth/github/logout', { credentials: 'include' });
    setUser(null);
    setIsAuthenticated(false);
};

const navigateToLogin = () => {
    window.location.href = '/api/v1/oauth/github';
};