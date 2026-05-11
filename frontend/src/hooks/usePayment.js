import { useState } from "react";
import { paymentApi } from "@/lib/paymentApi";

/**
 * Handles the full payment confirmation flow:
 * 1. Receive clientSecret + paymentIntentId (from checkout response or my-latest)
 * 2. Use Stripe Elements to collect card info
 * 3. Call backend confirm endpoint
 */
export function usePayment() {
    const [status, setStatus] = useState("idle"); // idle | processing | success | error
    const [errorMessage, setErrorMessage] = useState(null);

    /**
     * @param {object} stripe   - Stripe instance from loadStripe()
     * @param {object} elements - Stripe Elements instance
     * @param {string} paymentIntentId - e.g. "pi_..."
     */
    const confirmPayment = async (stripe, elements, paymentIntentId) => {
        setStatus("processing");
        setErrorMessage(null);

        // 1. Create payment method from card element
        const cardElement = elements.getElement("card");
        const { paymentMethod, error: pmError } = await stripe.createPaymentMethod({
            type: "card",
            card: cardElement,
        });

        if (pmError) {
            setStatus("error");
            setErrorMessage(pmError.message);
            return { success: false, error: pmError.message };
        }

        // 2. Confirm on backend
        const result = await paymentApi.confirmPayment(paymentIntentId, paymentMethod.id);

        // Backend returns PROCESSING — this is expected. Don't poll.
        setStatus("success");
        return { success: true, data: result.data };
    };

    const reset = () => {
        setStatus("idle");
        setErrorMessage(null);
    };

    return { status, errorMessage, confirmPayment, reset };
}