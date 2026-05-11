/**
 * Base API client — all requests go through here.
 * Credentials: "include" ensures the HttpOnly JWT cookie is sent on every request.
 * On 401, redirects to /login automatically.
 */

const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {}),
        },
    });

    if (response.status === 401) {
        window.location.href = "/login";
        return;
    }

    if (!response.ok) {
        let errorMessage = `Request failed: ${response.status}`;
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorData.error || errorMessage;
        } catch (_) {}
        throw new Error(errorMessage);
    }

    // Some endpoints return empty body (e.g. DELETE)
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

export const api = {
    get: (path, options) => request(path, { method: "GET", ...options }),
    post: (path, body, options) =>
        request(path, { method: "POST", body: JSON.stringify(body), ...options }),
    patch: (path, body, options) =>
        request(path, { method: "PATCH", body: JSON.stringify(body), ...options }),
    delete: (path, options) => request(path, { method: "DELETE", ...options }),
    put: (path, body, options) =>
        request(path, { method: "PUT", body: JSON.stringify(body), ...options }),
};