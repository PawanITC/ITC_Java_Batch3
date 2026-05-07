import { api } from "./api";

/**
 * Fetches the currently authenticated user from the Spring Boot backend.
 * The JWT is sent automatically via HttpOnly cookie.
 * Returns something like: { id, email, name, roles: ["ROLE_USER"] } or { role: "ROLE_ADMIN" }
 */
export const authApi = {
    me: () => api.get("/api/v1/users/me"),
};