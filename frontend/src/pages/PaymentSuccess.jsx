import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { CheckCircle2, ArrowRight, Package } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { orderApi } from "../lib/orderApi";
import { useCart } from "../context/CartContext.jsx";

export default function PaymentSuccess() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const { fetchCart } = useCart();

    const [order, setOrder] = useState(state?.orderData ?? null);
    const [loading, setLoading] = useState(!state?.orderData);

    const orderId =
        state?.orderData?.id ??
        state?.orderData?.orderId ??
        null;

    // Sync cart with backend on arrival — it will be empty after a successful checkout
    useEffect(() => {
        fetchCart();
    }, []);

    useEffect(() => {
        async function loadOrder() {
            if (order) return;

            try {
                if (!orderId) {
                    setLoading(false);
                    return;
                }

                const res = await orderApi.getOrder(orderId);
                setOrder(res.data ?? res);
            } catch (e) {
                console.error("Failed to fetch order", e);
            } finally {
                setLoading(false);
            }
        }

        loadOrder();
    }, [orderId]);

    if (loading) {
        return (
            <div className="text-center py-20 text-muted-foreground">
                Loading order...
            </div>
        );
    }

    const items = order?.items ?? [];
    const total = order?.totalAmount ?? 0;

    return (
        <div className="max-w-lg mx-auto px-4 py-16 text-center">
            <div className="w-20 h-20 rounded-full bg-green-50 border-4 border-green-100 flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
            </div>

            <h1 className="text-2xl font-extrabold mb-2">
                Payment Successful!
            </h1>

            <p className="text-muted-foreground mb-8">
                Your order has been placed successfully.
            </p>

            <div className="bg-white border rounded-xl p-6 text-left mb-6">
                <div className="flex items-center gap-2 mb-4">
                    <Package className="w-4 h-4 text-muted-foreground" />
                    <h2 className="font-semibold text-sm">Order Summary</h2>

                    {order?.id && (
                        <span className="ml-auto text-xs text-muted-foreground font-mono">
                            #{order.id}
                        </span>
                    )}
                </div>

                {items.length > 0 ? (
                    <div className="space-y-2 text-sm">
                        {items.map((item, i) => (
                            <div key={i} className="flex justify-between text-muted-foreground">
                                <span>
                                    {item.productName ?? item.name ?? `Product #${item.productId}`}
                                    {" "}× {item.quantity ?? 1}
                                </span>

                                <span className="font-medium text-foreground">
                                    ${(item.subTotal ?? 0).toFixed(2)}
                                </span>
                            </div>
                        ))}

                        <Separator className="my-3" />

                        <div className="flex justify-between font-bold text-base">
                            <span>Total Paid</span>
                            <span>${Number(total).toFixed(2)}</span>
                        </div>
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground">
                        Order is being finalized. Please check Orders page.
                    </p>
                )}
            </div>

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