import { useLocation, useNavigate } from "react-router-dom";
import { CheckCircle2, ArrowRight, Package } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";

export default function PaymentSuccess() {
    const { state } = useLocation();
    const navigate = useNavigate();

    const orderData = state?.orderData ?? null;
    const cart = state?.cart ?? null;

    // Normalize items from either source
    const items =
        orderData?.items ??
        cart?.items ??
        [];

    const total =
        orderData?.totalAmount ??
        cart?.totalAmount ??
        items.reduce((sum, i) => {
            const price = i.subTotal ?? (i.price ?? 0) * (i.quantity ?? 1);
            return sum + Number(price);
        }, 0);

    const orderId = orderData?.id ?? orderData?.orderId ?? null;

    return (
        <div className="max-w-lg mx-auto px-4 py-16 text-center">
            <div className="w-20 h-20 rounded-full bg-green-50 border-4 border-green-100 flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
            </div>

            <h1 className="text-2xl font-extrabold mb-2">
                Payment Successful!
            </h1>

            <p className="text-muted-foreground mb-8">
                Thank you for your order. We're preparing it now and will notify you when it ships.
            </p>

            {(items.length > 0 || orderData) && (
                <div className="bg-white border rounded-xl p-6 text-left mb-6">
                    <div className="flex items-center gap-2 mb-4">
                        <Package className="w-4 h-4 text-muted-foreground" />
                        <h2 className="font-semibold text-sm">Order Summary</h2>

                        {orderId && (
                            <span className="ml-auto text-xs text-muted-foreground font-mono">
                                #{orderId}
                            </span>
                        )}
                    </div>

                    {items.length > 0 ? (
                        <div className="space-y-2 text-sm">
                            {items.map((item, i) => {
                                const name =
                                    item.productName ??
                                    item.name ??
                                    `Product #${item.productId}`;

                                const qty = item.quantity ?? 1;

                                const price =
                                    item.subTotal ??
                                    item.priceAtPurchase ??
                                    (item.price ? item.price * qty : 0);

                                return (
                                    <div
                                        key={i}
                                        className="flex justify-between text-muted-foreground"
                                    >
                                        <span>
                                            {name} × {qty}
                                        </span>
                                        <span className="font-medium text-foreground">
                                            ${Number(price).toFixed(2)}
                                        </span>
                                    </div>
                                );
                            })}

                            <Separator className="my-3" />

                            <div className="flex justify-between font-bold text-base">
                                <span>Total Paid</span>
                                <span>${Number(total).toFixed(2)}</span>
                            </div>
                        </div>
                    ) : (
                        <p className="text-sm text-muted-foreground">
                            Order details are being synced. Refresh Orders page if needed.
                        </p>
                    )}
                </div>
            )}

            <div className="flex flex-col gap-3">
                <Button onClick={() => navigate("/orders")} className="gap-2">
                    View My Orders <ArrowRight className="w-4 h-4" />
                </Button>

                <Button variant="outline" onClick={() => navigate("/products")}>
                    Continue Shopping
                </Button>
            </div>
        </div>
    );
}