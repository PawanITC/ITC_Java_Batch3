import { api } from "./api";

// ── Users ─────────────────────────────────────────────────
export const adminUserApi = {
    /** GET /api/v1/admin/users */
    getAllUsers: () => api.get("/api/v1/admin/users"),

    /** PATCH /api/v1/admin/users/{userId}/role  body: { role } */
    updateRole: (userId, role) =>
        api.patch(`/api/v1/admin/users/${userId}/role`, { role }),

    /** PATCH /api/v1/admin/users/{userId}/status — toggles active/inactive */
    toggleStatus: (userId) =>
        api.patch(`/api/v1/admin/users/${userId}/status`),
};

// ── Products ──────────────────────────────────────────────
export const adminProductApi = {
    /** POST /api/v1/admin/products */
    createProduct: (data) => api.post("/api/v1/admin/products", data),

    /** PUT /api/v1/admin/products/{id} */
    updateProduct: (id, data) => api.put(`/api/v1/admin/products/${id}`, data),

    /** DELETE /api/v1/admin/products/{id} */
    deleteProduct: (id) => api.delete(`/api/v1/admin/products/${id}`),
};

// ── Categories ────────────────────────────────────────────
export const adminCategoryApi = {
    /** GET /api/v1/categories (public — reused here for convenience) */
    getAllCategories: () => api.get("/api/v1/categories"),

    /** POST /api/v1/admin/categories */
    createCategory: (data) => api.post("/api/v1/admin/categories", data),

    /** DELETE /api/v1/admin/categories/{id} */
    deleteCategory: (id) => api.delete(`/api/v1/admin/categories/${id}`),
};

// ── Orders ────────────────────────────────────────────────
export const adminOrderApi = {
    /**
     * GET /api/v1/admin/orders
     * @param {object} params  { status?, page?, size?, sort? }
     */
    getAllOrders: (params = {}) => {
        const qs = new URLSearchParams(
            Object.fromEntries(Object.entries(params).filter(([, v]) => v != null))
        ).toString();
        return api.get(`/api/v1/admin/orders${qs ? `?${qs}` : ""}`);
    },

    /** GET /api/v1/admin/orders/{id} — no ownership check (admin privilege) */
    getOrder: (id) => api.get(`/api/v1/admin/orders/${id}`),

    /** PATCH /api/v1/admin/orders/{id}/status?newStatus=SHIPPED */
    updateStatus: (id, newStatus) =>
        api.patch(`/api/v1/admin/orders/${id}/status?newStatus=${newStatus}`),
};