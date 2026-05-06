import { useQuery } from "@tanstack/react-query";
import { authApi } from "../lib/authApi";

export const CURRENT_USER_KEY = ["currentUser"];

/**
 * Fetches the authenticated user from your Spring Boot backend.
 * Determines role from `roles` array or `role` string field.
 */
export function useCurrentUser() {
    return useQuery({
        queryKey: CURRENT_USER_KEY,
        queryFn: authApi.me,
        retry: false,
        staleTime: 5 * 60 * 1000,
    });
}

/** Helper — true if user has ROLE_ADMIN */
export function isAdmin(user) {
    if (!user) return false;
    if (Array.isArray(user.roles)) return user.roles.includes("ROLE_ADMIN");
    return user.role === "ROLE_ADMIN";
}