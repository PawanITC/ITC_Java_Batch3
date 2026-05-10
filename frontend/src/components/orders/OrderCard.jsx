import { useNavigate } from "react-router-dom";
import { format } from "date-fns";
import { ChevronRight, Package, CreditCard } from "lucide-react";
import OrderStatusBadge from "./OrderStatusBadge";

export default function OrderCard({ order }) {
    const navigate = useNavigate();
    const date = order.createdAt ? format(new Date(order.createdAt), "MMM d, yyyy") : "—";
    const itemCount = order.items?.reduce((s, i) => s + i.quantity, 0) ?? 0;
    const isPending = order.orderStatus === "PENDING";

    const handleResumePayment = (e) => {
        e.stopPropagation(); // don't also navigate to order detail
        navigate("/checkout", { state: { resumeOrder: order } });
    };

    return (
        // Use a div + role="button" so we can nest a real <button> inside without
        // triggering the "button inside button" DOM nesting violation.
        <div
            role="button"
            tabIndex={0}
            onClick={() => navigate(`/orders/${order.orderId}`)}
            onKeyDown={(e) => e.key === "Enter" && navigate(`/orders/${order.orderId}`)}
            className="w-full bg-card border border-border rounded-xl p-5 hover:shadow-md hover:border-primary/30 transition-all duration-200 text-left group cursor-pointer"
        >
            <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-lg bg-secondary flex items-center justify-center shrink-0">
                        <Package className="w-5 h-5 text-muted-foreground" />
                    </div>
                    <div>
                        <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-bold text-sm">Order #{order.orderId}</span>
                            <span className="text-xs text-muted-foreground">{date}</span>
                        </div>
                        {itemCount > 0 && (
                            <p className="text-xs text-muted-foreground mt-0.5">
                                {itemCount} item{itemCount > 1 ? "s" : ""} · £{Number(order.totalAmount ?? 0).toFixed(2)}
                            </p>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <OrderStatusBadge status={order.orderStatus} />
                    {isPending && (
                        <button
                            onClick={handleResumePayment}
                            className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-600 text-white transition-colors shrink-0"
                        >
                            <CreditCard className="w-3.5 h-3.5" />
                            Pay Now
                        </button>
                    )}
                    <ChevronRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
                </div>
            </div>
        </div>
    );
}
