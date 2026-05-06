import { api } from "./api";

export const orderApi = {
    /** GET /lib/v1/orders/{id} — owner-guarded */
    getOrder: (id) => api.get(`/api/v1/orders/${id}`),

    /** GET /lib/v1/orders/history */
    getHistory: () => api.get("/api/v1/orders/history"),

    /** PATCH /lib/v1/orders/{id}/cancel */
    cancelOrder: (id) => api.patch(`/api/v1/orders/${id}/cancel`),
};