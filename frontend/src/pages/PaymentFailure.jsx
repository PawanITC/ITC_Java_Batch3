import { useLocation, useNavigate } from "react-router-dom";
import { XCircle, RefreshCw, ShoppingCart } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function PaymentFailure() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const error = state?.error || "Your payment could not be processed.";

    return (
        <div className="max-w-lg mx-auto px-4 py-16 text-center">
            <div className="w-20 h-20 rounded-full bg-red-50 border-4 border-red-100 flex items-center justify-center mx-auto mb-6">
                <XCircle className="w-10 h-10 text-destructive" />
            </div>

            <h1 className="text-2xl font-extrabold mb-2">Payment Failed</h1>
            <p className="text-muted-foreground mb-3">We couldn't process your payment.</p>

            <div className="bg-destructive/8 border border-destructive/20 rounded-xl px-5 py-4 mb-8 text-sm text-destructive">
                {error}
            </div>

            <ul className="text-sm text-muted-foreground text-left bg-card border rounded-xl px-5 py-4 mb-8 space-y-2">
                <li>• Check your card details and try again</li>
                <li>• Ensure your card has sufficient funds</li>
                <li>• Contact your bank if the issue persists</li>
            </ul>

            <div className="flex flex-col gap-3">
                <Button onClick={() => navigate("/checkout")} className="gap-2">
                    <RefreshCw className="w-4 h-4" /> Try Again
                </Button>
                <Button variant="outline" onClick={() => navigate("/cart")} className="gap-2">
                    <ShoppingCart className="w-4 h-4" /> Back to Cart
                </Button>
            </div>
        </div>
    );
}