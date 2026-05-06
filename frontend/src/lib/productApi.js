import { api } from "./api";

/**
 * Public product endpoints.
 */
export const productApi = {
    /** GET /lib/v1/products?page=0&size=20&categoryId=&search= */
    getProducts: (params = {}) => {
        const qs = new URLSearchParams(
            Object.fromEntries(Object.entries(params).filter(([, v]) => v != null && v !== ""))
        ).toString();
        return api.get(`/api/v1/products${qs ? `?${qs}` : ""}`);
    },

    /** GET /lib/v1/products/{id} */
    getProduct: (id) => api.get(`/api/v1/products/${id}`),

    /** GET /lib/v1/categories */
    getCategories: () => api.get("/api/v1/categories"),
};