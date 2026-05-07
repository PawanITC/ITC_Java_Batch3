import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Loader2, Package, AlertTriangle } from "lucide-react";
import { useOrder, useCancelOrder } from "../hooks/useOrders";
import OrderStatusBadge from "../components/orders/OrderStatusBadge";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { format } from "date-fns";

const CANCELLABLE = ["PENDING", "CONFIRMED"];

export default function OrderDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { data: order, isLoading, isError } = useOrder(id);
    const cancelMutation = useCancelOrder();

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
            <div className="bg-white border rounded-xl p-6">
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
            <div className="bg-white border rounded-xl p-6 space-y-4">
                <h2 className="font-semibold">Items</h2>
                <div className="space-y-3">
                    {order.items?.map((item, i) => (
                        <div key={i} className="flex justify-between text-sm">
              <span className="text-muted-foreground">
                {item.productName ?? `Product #${item.productId}`}{" "}
                  <span className="text-foreground font-medium">× {item.quantity}</span>
              </span>
                            <span className="font-medium">${Number(item.subTotal ?? item.price * item.quantity).toFixed(2)}</span>
                        </div>
                    ))}
                </div>
                <Separator />
                <div className="flex justify-between font-semibold">
                    <span>Total</span>
                    <span>${Number(order.totalAmount ?? 0).toFixed(2)}</span>
                </div>
            </div>

            {/* Actions */}
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
    );
}