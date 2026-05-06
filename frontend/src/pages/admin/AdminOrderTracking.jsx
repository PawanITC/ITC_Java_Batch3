import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, ArrowLeft, Package, Loader2, User, Calendar, DollarSign } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { orderApi } from "../../lib/orderApi";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import OrderStatusBadge from "../../components/orders/OrderStatusBadge";
import OrderTimeline from "../../components/orders/OrderTimeline";
import { format } from "date-fns";

// Dummy fallback orders for demo purposes
const DUMMY_ORDERS = {
    "1001": {
        id: 1001, status: "SHIPPED", totalAmount: 124.99,
        createdAt: "2026-05-01T10:00:00Z",
        customerEmail: "alice@example.com",
        items: [
            { productName: "Neon Burst Print", quantity: 1, subTotal: 49.99 },
            { productName: "Abstract Flow Poster", quantity: 1, subTotal: 75.00 },
        ],
    },
    "1002": {
        id: 1002, status: "PROCESSING", totalAmount: 59.99,
        createdAt: "2026-05-03T14:30:00Z",
        customerEmail: "bob@example.com",
        items: [
            { productName: "Retro Wave Canvas", quantity: 1, subTotal: 59.99 },
        ],
    },
    "1003": {
        id: 1003, status: "DELIVERED", totalAmount: 210.00,
        createdAt: "2026-04-28T09:15:00Z",
        customerEmail: "carol@example.com",
        items: [
            { productName: "Funky Grid Print", quantity: 2, subTotal: 100.00 },
            { productName: "Limited Edition Foil", quantity: 1, subTotal: 110.00 },
        ],
    },
    "1004": {
        id: 1004, status: "CANCELLED", totalAmount: 34.99,
        createdAt: "2026-04-25T16:00:00Z",
        customerEmail: "dave@example.com",
        items: [
            { productName: "Mini Sticker Pack", quantity: 1, subTotal: 34.99 },
        ],
    },
};

function OrderTrackingResult({ orderId }) {
    const { data: liveOrder, isLoading, isError } = useQuery({
        queryKey: ["admin-track-order", orderId],
        queryFn: () => orderApi.getOrder(orderId),
        retry: false,
    });

    if (isLoading) {
        return (
            <div className="flex justify-center py-16">
                <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
            </div>
        );
    }

    // Fall back to dummy data if backend returns error
    const order = (!isError && liveOrder) ? liveOrder : DUMMY_ORDERS[String(orderId)];

    if (!order) {
        return (
            <div className="bg-muted rounded-xl px-6 py-10 text-center text-muted-foreground">
                <Package className="w-10 h-10 mx-auto mb-3 opacity-40" />
                <p className="font-medium">No order found with ID #{orderId}</p>
                <p className="text-sm mt-1">Try IDs 1001 – 1004 for demo orders.</p>
            </div>
        );
    }

    const date = order.createdAt ? format(new Date(order.createdAt), "MMMM d, yyyy 'at' h:mm a") : "—";

    return (
        <div className="space-y-6">
            {/* Order header */}
            <div className="bg-white border rounded-xl p-6">
                <div className="flex flex-wrap items-start justify-between gap-4 mb-4">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-muted flex items-center justify-center">
                            <Package className="w-5 h-5 text-muted-foreground" />
                        </div>
                        <div>
                            <h2 className="font-bold text-lg">Order #{order.id}</h2>
                            <p className="text-sm text-muted-foreground flex items-center gap-1">
                                <Calendar className="w-3 h-3" /> {date}
                            </p>
                        </div>
                    </div>
                    <OrderStatusBadge status={order.status} />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                    <div className="flex items-center gap-2 text-muted-foreground">
                        <User className="w-4 h-4" />
                        <span>{order.customerEmail ?? order.userId ?? "—"}</span>
                    </div>
                    <div className="flex items-center gap-2 font-semibold">
                        <DollarSign className="w-4 h-4 text-muted-foreground" />
                        <span>${Number(order.totalAmount ?? 0).toFixed(2)}</span>
                    </div>
                </div>
            </div>

            {/* Items */}
            <div className="bg-white border rounded-xl p-6 space-y-3">
                <h3 className="font-semibold text-sm">Items</h3>
                <div className="space-y-2">
                    {order.items?.map((item, i) => (
                        <div key={i} className="flex justify-between text-sm">
              <span className="text-muted-foreground">
                {item.productName ?? `Product #${item.productId}`}{" "}
                  <span className="text-foreground font-medium">× {item.quantity}</span>
              </span>
                            <span className="font-medium">${Number(item.subTotal).toFixed(2)}</span>
                        </div>
                    ))}
                </div>
                <Separator />
                <div className="flex justify-between font-semibold text-sm">
                    <span>Total</span>
                    <span>${Number(order.totalAmount ?? 0).toFixed(2)}</span>
                </div>
            </div>

            {/* Timeline */}
            <div className="bg-white border rounded-xl p-6">
                <h3 className="font-semibold text-sm mb-6">Tracking Timeline</h3>
                <OrderTimeline status={order.status} />
            </div>
        </div>
    );
}

export default function AdminOrderTracking() {
    const navigate = useNavigate();
    const [input, setInput] = useState("");
    const [searchedId, setSearchedId] = useState(null);

    const handleSearch = (e) => {
        e.preventDefault();
        if (input.trim()) setSearchedId(input.trim());
    };

    return (
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
            <button
                onClick={() => navigate("/admin")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Dashboard
            </button>

            <div>
                <h1 className="text-2xl font-extrabold mb-1">Order Tracking</h1>
                <p className="text-sm text-muted-foreground">Look up any order by ID to see its current status and timeline.</p>
            </div>

            <form onSubmit={handleSearch} className="flex gap-2">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="Enter order ID (e.g. 1001)"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        className="pl-9 bg-white"
                    />
                </div>
                <Button type="submit" disabled={!input.trim()}>Track</Button>
            </form>

            {searchedId && <OrderTrackingResult orderId={searchedId} />}

            {!searchedId && (
                <div className="bg-muted/50 rounded-xl px-6 py-10 text-center text-muted-foreground">
                    <Package className="w-10 h-10 mx-auto mb-3 opacity-30" />
                    <p className="text-sm">Enter an order ID above to track it.</p>
                    <p className="text-xs mt-1">Demo order IDs: 1001, 1002, 1003, 1004</p>
                </div>
            )}
        </div>
    );
}