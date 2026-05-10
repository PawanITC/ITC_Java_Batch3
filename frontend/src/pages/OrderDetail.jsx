import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Loader2, Package, AlertTriangle, CreditCard } from "lucide-react";
import { useQueries } from "@tanstack/react-query";
import { useOrder, useCancelOrder } from "@/hooks/useOrders";
import OrderStatusBadge from "@/components/orders/OrderStatusBadge";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { productApi } from "@/lib/productApi";
import { format } from "date-fns";

const CANCELLABLE = ["PENDING", "CONFIRMED"];

export default function OrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { data: order, isLoading, isError } = useOrder(id);
    const cancelMutation = useCancelOrder();

    // Fetch product details for each order item to get name + image
    const productQueries = useQueries({
        queries: (order?.items ?? []).map((item) => ({
            queryKey: ["product", item.productId],
            queryFn: () => productApi.getProduct(item.productId).then((r) => r?.data ?? r),
            staleTime: 1000 * 60 * 5,
        })),
    });

    // Build a lookup map: productId -> { name, imageUrl }
    const productMap = (order?.items ?? []).reduce((acc, item, i) => {
        const p = productQueries[i]?.data;
        acc[item.productId] = p ? { name: p.name, imageUrl: p.imageUrls?.[0] ?? null } : null;
        return acc;
    }, {});

    if (isLoading) {
        return (
            <div className="flex justify-center py-20">
                <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
        );
    }

    if (isError || !order) {
        return (
            <div className="max-w-lg mx-auto px-4 py-20 text-center space-y-4">
                <AlertTriangle className="w-12 h-12 text-destructive mx-auto" />
                <h2 className="text-xl font-semibold">Order not found</h2>
                <Button variant="outline" onClick={() => navigate("/orders")}>Back to Orders</Button>
            </div>
        );
    }

    const canCancel = CANCELLABLE.includes(order.orderStatus);
    const date = order.createdAt ? format(new Date(order.createdAt), "MMMM d, yyyy 'at' h:mm a") : "—";

    return (
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
            {/* Back */}
            <button
                onClick={() => navigate("/orders")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Orders
            </button>

            {/* Header */}
            <div className="bg-card border rounded-xl p-6">
                <div className="flex items-start justify-between gap-4 flex-wrap">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-muted flex items-center justify-center">
                            <Package className="w-5 h-5 text-muted-foreground" />
                        </div>
                        <div>
                            <h1 className="font-bold text-lg">Order #{order.orderId}</h1>
                            <p className="text-sm text-muted-foreground">{date}</p>
                        </div>
                    </div>
                    <OrderStatusBadge status={order.orderStatus} />
                </div>
            </div>

            {/* Items */}
            <div className="bg-card border rounded-xl p-6 space-y-4">
                <h2 className="font-semibold">Items</h2>
                <div className="space-y-3">
                    {order.items?.map((item, i) => {
                        const product = productMap[item.productId];
                        const name = product?.name ?? `Product #${item.productId}`;
                        const imageUrl = product?.imageUrl;
                        return (
                            <div key={i} className="flex items-center gap-3">
                                {/* Product thumbnail */}
                                <div className="w-12 h-12 rounded-lg bg-muted shrink-0 overflow-hidden">
                                    {imageUrl ? (
                                        <img
                                            src={imageUrl}
                                            alt={name}
                                            className="w-full h-full object-cover"
                                        />
                                    ) : (
                                        <div className="w-full h-full flex items-center justify-center text-xl">🎨</div>
                                    )}
                                </div>
                                {/* Name + qty */}
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium truncate">{name}</p>
                                    <p className="text-xs text-muted-foreground">× {item.quantity}</p>
                                </div>
                                {/* Subtotal */}
                                <span className="text-sm font-medium shrink-0">
                                    £{Number(item.subTotal ?? item.priceAtPurchase * item.quantity).toFixed(2)}
                                </span>
                            </div>
                        );
                    })}
                </div>
                <Separator />
                <div className="flex justify-between font-semibold">
                    <span>Total</span>
                    <span>£{Number(order.totalAmount ?? 0).toFixed(2)}</span>
                </div>
            </div>

            {/* Actions */}
            <div className="flex flex-col gap-3">
                {/* Resume payment — only for PENDING orders */}
                {order.orderStatus === "PENDING" && (
                    <Button
                        className="w-full gap-2 bg-amber-500 hover:bg-amber-600 text-white"
                        onClick={() => navigate("/checkout", { state: { resumeOrder: order } })}
                    >
                        <CreditCard className="w-4 h-4" />
                        Complete Payment
                    </Button>
                )}

                {canCancel && (
                    <Button
                        variant="destructive"
                        className="w-full"
                        disabled={cancelMutation.isPending}
                        onClick={() => cancelMutation.mutate(order.orderId)}
                    >
                        {cancelMutation.isPending ? (
                            <span className="flex items-center gap-2">
                                <Loader2 className="w-4 h-4 animate-spin" /> Cancelling…
                            </span>
                        ) : (
                            "Cancel Order"
                        )}
                    </Button>
                )}
            </div>
        </div>
    );
}