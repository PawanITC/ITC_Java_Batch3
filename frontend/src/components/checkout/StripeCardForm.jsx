import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Lock, Loader2 } from "lucide-react";

/**
 * StripeCardForm
 *
 * Props:
 *  - stripe: Stripe instance (from loadStripe)
 *  - elements: Stripe Elements instance
 *  - onSubmit: async (stripe, elements) => void
 *  - loading: boolean
 *  - error: string | null
 */
export default function StripeCardForm({ stripe, elements, onSubmit, loading, error }) {
    const cardRef = useRef(null);
    const [cardReady, setCardReady] = useState(false);
    const [cardError, setCardError] = useState(null);

    useEffect(() => {
        if (!elements) return;

        // Detect dark mode once at mount time by checking the <html> class.
        // We intentionally do NOT add the theme as a dep — destroying and recreating
        // the card element on every toggle breaks interactivity in React StrictMode.
        // Users switching themes mid-checkout is rare; they can reload if needed.
        const isDark = document.documentElement.classList.contains("dark");
        const textColor        = isDark ? "#f5f5f5" : "#0a0a0a";
        const placeholderColor = isDark ? "#737373" : "#a3a3a3";

        // card.destroy() (not unmount) deregisters the element from the Elements instance.
        // This is critical in React StrictMode which runs cleanup + re-mount in dev:
        // card.unmount() only removes from DOM but keeps it registered, so the second
        // elements.create("card") call throws "Can only create one Element of type card".
        const card = elements.create("card", {
            style: {
                base: {
                    fontSize: "16px",
                    color: textColor,
                    fontFamily: "ui-sans-serif, system-ui, sans-serif",
                    "::placeholder": { color: placeholderColor },
                },
                invalid: { color: "#ef4444" },
            },
        });

        card.mount(cardRef.current);
        card.on("change", (e) => setCardError(e.error?.message ?? null));
        setCardReady(true);

        return () => {
            setCardReady(false);
            card.destroy();
        };
    }, [elements]); // elements only — do not add dark/theme here

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!stripe || !elements || loading) return;
        await onSubmit(stripe, elements);
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label className="block text-sm font-medium mb-2">Card Details</label>
                <div
                    ref={cardRef}
                    className="border rounded-lg px-4 py-3 bg-background focus-within:ring-2 focus-within:ring-ring transition min-h-[46px]"
                />
                {cardError && (
                    <p className="text-destructive text-sm mt-1">{cardError}</p>
                )}
            </div>

            {error && (
                <div className="bg-destructive/10 text-destructive text-sm rounded-lg px-4 py-3">
                    {error}
                </div>
            )}

            <Button type="submit" className="w-full h-11" disabled={!stripe || !cardReady || loading}>
                {loading ? (
                    <span className="flex items-center gap-2">
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Processing…
                    </span>
                ) : (
                    <span className="flex items-center gap-2">
                        <Lock className="w-4 h-4" />
                        Pay Now
                    </span>
                )}
            </Button>

            <p className="text-center text-xs text-muted-foreground flex items-center justify-center gap-1">
                <Lock className="w-3 h-3" />
                Secured by Stripe
            </p>
        </form>
    );
}
