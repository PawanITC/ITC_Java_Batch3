import { api } from "./api";

export const paymentApi = {
    /**
     * Fetch the latest payment intent for the current user.
     * Called after checkout to get the clientSecret for Stripe Elements.
     */
    getLatestIntent: () => api.get("/api/v1/payments/my-latest"),

    /**
     * Explicitly create a payment intent.
     * @param {number} orderId
     * @param {number} amount
     * @param {string} currency
     */
    createIntent: (orderId, amount, currency = "usd") =>
        api.post("/api/v1/payments/create-intent", { orderId, amount, currency }),

    /**
     * Confirm the payment after Stripe returns a paymentMethod.
     * @param {string} paymentIntentId
     * @param {string} paymentMethodId
     */
    confirmPayment: (paymentIntentId, paymentMethodId) =>
        api.post("/api/v1/payments/confirm", {
            paymentIntentId,
            paymentMethodId,
            returnUrl: `${window.location.origin}/payment-success`,
        }),
};