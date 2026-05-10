import { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { Loader2, AlertTriangle } from "lucide-react";
import { loadStripe } from "@stripe/stripe-js";
import { useCart } from "../context/CartContext";
import { paymentApi } from "../lib/paymentApi";
import { usePayment } from "../hooks/usePayment";
import OrderSummary from "../components/checkout/OrderSummary";
import StripeCardForm from "../components/checkout/StripeCardForm";
import { Button } from "@/components/ui/button";

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);

export default function CheckoutPage() {
    const { cart, fetchCart } = useCart();
    const navigate = useNavigate();
    const { state } = useLocation();

    // resumeOrder: set when navigating here from a PENDING order's "Complete Payment" button.
    // In this case we reuse the existing payment intent — no new checkout() call needed.
    const resumeOrder = state?.resumeOrder ?? null;

    // For the order summary: prefer the resume order's items, then live cart, then snapshot.
    const displayCart = resumeOrder
        ? { items: resumeOrder.items ?? [], totalAmount: resumeOrder.totalAmount ?? 0 }
        : (cart ?? state?.cartSnapshot);

    const [stripeInstance, setStripeInstance] = useState(null);
    const [elements, setElements] = useState(null);
    const [paymentIntentId, setPaymentIntentId] = useState(null);

    const [initError, setInitError] = useState(null);
    const [initializing, setInitializing] = useState(true);

    const { status, errorMessage, confirmPayment } = usePayment();

    // Only fetch live cart when we have no other summary source
    useEffect(() => {
        if (!resumeOrder && !state?.cartSnapshot && !cart) fetchCart();
    }, [resumeOrder, state, cart, fetchCart]);

    useEffect(() => {
        let cancelled = false;

        async function init() {
            setInitializing(true);
            setInitError(null);

            try {
                const stripe = await stripePromise;
                if (cancelled) return;

                let clientSecret = null;
                let intentId = null;

                // Poll until payment intent is ready.
                // Backend returns 204 (null) while Kafka processes the order — keep waiting.
                // Backend returns 200 + clientSecret when ready.
                const MAX_ATTEMPTS = 15;   // 15 × 1500ms = ~22s max
                const DELAY_MS     = 1500;

                for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                    if (cancelled) return;

                    try {
                        const res  = await paymentApi.getLatestIntent();
                        const data = res?.data ?? res;

                        if (data?.clientSecret) {
                            clientSecret = data.clientSecret;
                            intentId     = data.paymentIntentId;
                            break;
                        }
                        // null means 204 — still being prepared, fall through to wait
                    } catch (_) {
                        // real error (network/5xx) — still retry silently
                    }

                    await new Promise(r => setTimeout(r, DELAY_MS));
                }

                if (!clientSecret) {
                    setInitError(
                        "Payment is still being prepared. Please wait a moment and try again."
                    );
                    setInitializing(false);
                    return;
                }

                const els = stripe.elements({ clientSecret });

                if (cancelled) return;

                setStripeInstance(stripe);
                setElements(els);
                setPaymentIntentId(intentId);
                setInitializing(false);
            } catch (err) {
                setInitError("Failed to initialize Stripe checkout.");
                setInitializing(false);
            }
        }

        init();
        return () => {
            cancelled = true;
        };
    }, []);

    const handlePay = async (stripe, els) => {
        // Snapshot cart NOW before fetchCart() on PaymentSuccess clears it
        const cartSnapshot = displayCart;
        const result = await confirmPayment(stripe, els, paymentIntentId);

        if (result.success) {
            navigate("/payment-success", {
                state: {
                    orderData: result.data,
                    cartSnapshot,
                }
            });
        } else {
            navigate("/payment-failure", {
                state: { error: result.error }
            });
        }
    };

    if (initializing) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
                <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                <p className="text-muted-foreground text-sm">
                    Setting up payment…
                </p>
            </div>
        );
    }

    if (initError) {
        return (
            <div className="max-w-lg mx-auto px-4 py-20 text-center space-y-4">
                <AlertTriangle className="w-12 h-12 text-destructive mx-auto" />
                <h2 className="text-xl font-semibold">Payment Setup Failed</h2>
                <p className="text-muted-foreground text-sm">{initError}</p>

                <Button onClick={() => navigate("/cart")}>
                    Back to Cart
                </Button>
            </div>
        );
    }

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
            <h1 className="text-2xl md:text-3xl font-bold mb-8">
                Checkout
            </h1>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div className="bg-card rounded-xl border p-6">
                    <h2 className="font-semibold text-lg mb-6">
                        Payment Details
                    </h2>

                    <StripeCardForm
                        stripe={stripeInstance}
                        elements={elements}
                        onSubmit={handlePay}
                        loading={status === "processing"}
                        error={errorMessage ?? null}
                    />
                </div>

                <div>
                    <OrderSummary cart={displayCart} />
                </div>
            </div>
        </div>
    );
}
