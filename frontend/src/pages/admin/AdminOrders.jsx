import { useState } from "react";
import { ShoppingBag, Loader2, RefreshCw } from "lucide-react";
import { useAdminOrders, useUpdateOrderStatus } from "../../hooks/useAdminOrders";
import OrderStatusBadge from "../../components/orders/OrderStatusBadge";
import StatusSelect from "../../components/admin/StatusSelect";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { format } from "date-fns";

const STATUS_FILTERS = ["ALL", "PENDING", "PROCESSING", "PAID", "SHIPPED", "DELIVERED", "CANCELLED"];

export default function AdminOrders() {
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [page, setPage] = useState(0);

    const params = {
        ...(statusFilter !== "ALL" && { status: statusFilter }),
        page,
        size: 10,
        sort: "createdAt,desc",
    };

    const { data: pageData, isLoading, isError, refetch } = useAdminOrders(params);
    const updateStatus = useUpdateOrderStatus();

    const orders = pageData?.content ?? pageData ?? [];
    const totalPages = pageData?.totalPages ?? 1;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                    <ShoppingBag className="w-6 h-6" />
                    <h2 className="text-xl font-semibold">Order Management</h2>
                </div>

                {/* Status filter */}
                <Select value={statusFilter} onValueChange={(v) => { setStatusFilter(v); setPage(0); }}>
                    <SelectTrigger className="w-40 h-9 text-sm">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {STATUS_FILTERS.map((s) => (
                            <SelectItem key={s} value={s} className="text-sm">{s}</SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            </div>

            {isLoading && (
                <div className="flex justify-center py-12">
                    <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
                </div>
            )}

            {isError && (
                <div className="flex items-center gap-3 text-destructive text-sm">
                    <p>Failed to load orders.</p>
                    <Button variant="outline" size="sm" onClick={refetch} className="gap-1.5">
                        <RefreshCw className="w-3.5 h-3.5" /> Retry
                    </Button>
                </div>
            )}

            {!isLoading && orders.length === 0 && (
                <p className="text-muted-foreground text-sm py-8 text-center">No orders found.</p>
            )}

            {orders.length > 0 && (
                <div className="bg-white border rounded-xl overflow-x-auto">
                    <table className="w-full text-sm min-w-[640px]">
                        <thead className="bg-muted/50">
                        <tr>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Order ID</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Date</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Customer</th>
                            <th className="text-right px-4 py-3 font-medium text-muted-foreground">Total</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Status</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Update</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y">
                        {orders.map((order) => (
                            <tr key={order.id} className="hover:bg-muted/20 transition-colors">
                                <td className="px-4 py-3 font-medium">#{order.id}</td>
                                <td className="px-4 py-3 text-muted-foreground">
                                    {order.createdAt ? format(new Date(order.createdAt), "MMM d, yyyy") : "—"}
                                </td>
                                <td className="px-4 py-3 text-muted-foreground">
                                    {order.userId ?? order.customerEmail ?? "—"}
                                </td>
                                <td className="px-4 py-3 text-right font-semibold">
                                    ${Number(order.totalAmount ?? 0).toFixed(2)}
                                </td>
                                <td className="px-4 py-3">
                                    <OrderStatusBadge status={order.status} />
                                </td>
                                <td className="px-4 py-3">
                                    <StatusSelect
                                        value={order.status}
                                        onChange={(newStatus) => updateStatus.mutate({ id: order.id, newStatus })}
                                        disabled={updateStatus.isPending}
                                    />
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center items-center gap-3 text-sm">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                        Previous
                    </Button>
                    <span className="text-muted-foreground">Page {page + 1} of {totalPages}</span>
                    <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                        Next
                    </Button>
                </div>
            )}
        </div>
    );
}