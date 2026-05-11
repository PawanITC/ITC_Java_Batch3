import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminOrderApi } from "@/lib/adminApi";
import { useToast } from "@/components/ui/use-toast";

export const ADMIN_ORDERS_KEY = ["admin", "orders"];

export function useAdminOrders(params = {}) {
    return useQuery({
        queryKey: [...ADMIN_ORDERS_KEY, params],
        queryFn: () => adminOrderApi.getAllOrders(params).then((r) => r?.data ?? r),
    });
}

export function useUpdateOrderStatus() {
    const qc = useQueryClient();
    const { toast } = useToast();

    return useMutation({
        mutationFn: ({ id, newStatus }) => adminOrderApi.updateStatus(id, newStatus),
        onSuccess: (_data, { newStatus }) => {
            qc.invalidateQueries({ queryKey: ADMIN_ORDERS_KEY });
            toast({ title: `Order status updated to ${newStatus}` });
        },
        onError: (err) => {
            const msg =
                err?.response?.data?.message ??
                err?.message ??
                "Failed to update order status";
            toast({ title: "Update failed", description: msg, variant: "destructive" });
        },
    });
}
