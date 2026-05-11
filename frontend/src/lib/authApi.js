import { api } from "./api";

/**
 * Fetches the currently authenticated user from the Spring Boot backend.
 * The JWT is sent automatically via HttpOnly cookie.
 * Returns { id, name, email, role } — unwrapped from ApiResponse<UserProfileDto>.
 */
export const authApi = {
    // Backend returns ApiResponse<UserProfileDto> ({ success, data, message }).
    // Unwrap .data so callers get the flat { id, name, email, role } object directly.
    me: () => api.get("/api/v1/users/me").then(res => res?.data ?? res),
};