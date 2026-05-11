import { api } from "./api";

export const orderApi = {
    /** GET /api/v1/orders/{id} — owner-guarded */
    getOrder: (id) => api.get(`/api/v1/orders/${id}`),

    /** GET /api/v1/orders/history */
    getHistory: () => api.get("/api/v1/orders/history"),

    /** PATCH /api/v1/orders/{id}/cancel */
    cancelOrder: (id) => api.patch(`/api/v1/orders/${id}/cancel`),
};