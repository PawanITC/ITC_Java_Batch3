import { api } from "./api";

export const cartApi = {
    getCart: () => api.get("/api/v1/cart/my-cart"),

    addItem: (productId, quantity) =>
        api.post("/api/v1/cart/items", { productId, quantity }),

    updateItem: (productId, quantityChange) =>
        api.patch(`/api/v1/cart/items/${productId}`, { quantityChange }),

    removeItem: (productId) =>
        api.delete(`/api/v1/cart/items/${productId}`),

    checkout: () =>
        api.post("/api/v1/cart/checkout"),
};