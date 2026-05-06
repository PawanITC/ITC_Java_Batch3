import { ClipboardList, Loader2, RefreshCw, ArrowRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useOrderHistory } from "../hooks/useOrders";
import OrderCard from "../components/orders/OrderCard";
import { Button } from "@/components/ui/button";

export default function OrderHistory() {
    const { data: orders, isLoading, isError, refetch } = useOrderHistory();
    const navigate = useNavigate();

    return (
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
            <div className="mb-8">
                <h1 className="text-3xl font-extrabold flex items-center gap-3 mb-1">
                    <ClipboardList className="w-7 h-7" /> Order History
                </h1>
                <p className="text-muted-foreground text-sm">Track and manage all your past orders.</p>
            </div>

            {isLoading && (
                <div className="flex justify-center py-24">
                    <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                </div>
            )}

            {isError && (
                <div className="bg-destructive/10 text-destructive rounded-xl px-6 py-4 flex items-center justify-between">
                    <p>Failed to load orders.</p>
                    <Button variant="outline" size="sm" onClick={refetch} className="gap-2">
                        <RefreshCw className="w-4 h-4" /> Retry
                    </Button>
                </div>
            )}

            {!isLoading && !isError && (!orders || orders.length === 0) && (
                <div className="flex flex-col items-center justify-center py-32 gap-5 text-center">
                    <div className="w-20 h-20 rounded-full bg-muted flex items-center justify-center">
                        <ClipboardList className="w-9 h-9 text-muted-foreground/50" />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold mb-1">No orders yet</h2>
                        <p className="text-muted-foreground text-sm">Your completed orders will appear here.</p>
                    </div>
                    <Button onClick={() => navigate("/products")} className="gap-2 mt-2">
                        Start Shopping <ArrowRight className="w-4 h-4" />
                    </Button>
                </div>
            )}

            {orders?.length > 0 && (
                <div className="space-y-4">
                    {orders.map((order) => (
                        <OrderCard key={order.id} order={order} />
                    ))}
                </div>
            )}
        </div>
    );
}