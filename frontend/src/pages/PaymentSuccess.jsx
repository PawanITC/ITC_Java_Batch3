import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { CheckCircle2, ArrowRight, Package, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { orderApi } from "../lib/orderApi";
import { useCart } from "../context/CartContext.jsx";
import { useQueries } from "@tanstack/react-query";
import { productApi } from "../lib/productApi";

export default function PaymentSuccess() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const { fetchCart } = useCart();

    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);

    // PaymentResponse has orderId (not id)
    const orderId = state?.orderData?.orderId ?? null;

    // Sync cart on arrival — will be empty after successful checkout
    useEffect(() => {
        fetchCart();
    }, []);

    // Initial order fetch
    useEffect(() => {
        async function loadOrder() {
            if (!orderId) { setLoading(false); return; }
            try {
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

    // Poll until order leaves PENDING (Stripe webhook + Kafka is async)
    useEffect(() => {
        if (!orderId) return;
        if (order && order.orderStatus && order.orderStatus !== "PENDING") return;

        let attempts = 0;
        const MAX = 20; // 20 × 2s = 40s max
        const id = setInterval(async () => {
            if (attempts++ >= MAX) { clearInterval(id); return; }
            try {
                const res = await orderApi.getOrder(orderId);
                const updated = res.data ?? res;
                if (updated.orderStatus && updated.orderStatus !== "PENDING") {
                    setOrder(updated);
                    clearInterval(id);
                }
            } catch (_) { /* silent */ }
        }, 2000);

        return () => clearInterval(id);
    }, [orderId, order?.orderStatus]);

    // ── Compute items BEFORE any early return so hook count is stable ──
    const cartSnapshot = state?.cartSnapshot;
    const rawItems = order?.items?.length ? order.items : (cartSnapshot?.items ?? []);
    const items = rawItems;
    const total = order?.totalAmount ?? cartSnapshot?.totalAmount ?? 0;

    // Enrich items with product name + image (useQueries must be unconditional)
    const productQueries = useQueries({
        queries: items.map((item) => ({
            queryKey: ["product", item.productId],
            queryFn: () => productApi.getProduct(item.productId).then((r) => r?.data ?? r),
            enabled: !!item.productId,
            staleTime: 5 * 60 * 1000,
            retry: false,
        })),
    });

    const productMap = items.reduce((acc, item, i) => {
        const p = productQueries[i]?.data;
        if (p) acc[item.productId] = { name: p.name, imageUrl: p.imageUrls?.[0] ?? null };
        return acc;
    }, {});

    // ── Early return AFTER all hooks ──
    if (loading) {
        return (
            <div className="text-center py-20 text-muted-foreground">
                Loading order...
            </div>
        );
    }

    return (
        <div className="max-w-lg mx-auto px-4 py-16 text-center">
            <div className="w-20 h-20 rounded-full bg-green-50 border-4 border-green-100 flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
            </div>

            <h1 className="text-2xl font-extrabold mb-2">Payment Successful!</h1>

            <p className="text-muted-foreground mb-8">
                Your order has been placed successfully.
            </p>

            <div className="bg-card border rounded-xl p-6 text-left mb-6">
                <div className="flex items-center gap-2 mb-4">
                    <Package className="w-4 h-4 text-muted-foreground" />
                    <h2 className="font-semibold text-sm">Order Summary</h2>
                    {order?.orderId && (
                        <span className="ml-auto text-xs text-muted-foreground font-mono">
                            #{order.orderId}
                        </span>
                    )}
                </div>

                {/* Status badge */}
                {order?.orderStatus && (
                    <div className="mb-3 flex items-center gap-1.5">
                        {order.orderStatus === "PENDING" ? (
                            <>
                                <Loader2 className="w-3.5 h-3.5 animate-spin text-amber-500" />
                                <span className="text-xs text-amber-600 font-medium">Finalizing order…</span>
                            </>
                        ) : (
                            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                                order.orderStatus === "PAID"
                                    ? "bg-green-100 text-green-700"
                                    : order.orderStatus === "FAILED"
                                    ? "bg-red-100 text-red-700"
                                    : "bg-muted text-muted-foreground"
                            }`}>
                                {order.orderStatus}
                            </span>
                        )}
                    </div>
                )}

                {/* Line items */}
                {items.length > 0 && (
                    <div className="space-y-3 mb-4">
                        {items.map((item, idx) => {
                            const enriched  = productMap[item.productId];
                            const name      = enriched?.name ?? item.productName ?? item.name ?? `Product #${item.productId}`;
                            const imageUrl  = enriched?.imageUrl ?? item.imageUrl ?? null;
                            const lineTotal = Number(
                                item.subTotal ?? item.subtotal ?? item.totalPrice
                                ?? ((item.price ?? 0) * (item.quantity ?? 1))
                                ?? 0
                            );
                            return (
                                <div key={idx} className="flex items-center gap-3 text-sm">
                                    <div className="w-10 h-10 rounded-md bg-muted shrink-0 overflow-hidden">
                                        {imageUrl ? (
                                            <img src={imageUrl} alt={name} className="w-full h-full object-cover" />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center text-lg">🎨</div>
                                        )}
                                    </div>
                                    <span className="text-muted-foreground truncate flex-1 min-w-0">
                                        {name}
                                        {(item.quantity ?? 1) > 1 && (
                                            <span className="ml-1 text-foreground font-medium">×{item.quantity}</span>
                                        )}
                                    </span>
                                    <span className="tabular-nums font-medium shrink-0">
                                        £{lineTotal.toFixed(2)}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                )}

                <Separator />

                <div className="flex justify-between font-bold mt-4">
                    <span>Total</span>
                    <span className="text-lg tabular-nums">£{Number(total).toFixed(2)}</span>
                </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <Button onClick={() => navigate("/orders")} className="gap-2">
                    <Package className="w-4 h-4" /> View Orders
                </Button>
                <Button variant="outline" onClick={() => navigate("/products")} className="gap-2">
                    Continue Shopping <ArrowRight className="w-4 h-4" />
                </Button>
            </div>
        </div>
    );
}
