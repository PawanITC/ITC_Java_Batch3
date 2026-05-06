import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminOrderApi } from "../lib/adminApi";

export const ADMIN_ORDERS_KEY = ["admin", "orders"];

export function useAdminOrders(params = {}) {
    return useQuery({
        queryKey: [...ADMIN_ORDERS_KEY, params],
        queryFn: () => adminOrderApi.getAllOrders(params).then((r) => r?.data ?? r),
    });
}

export function useUpdateOrderStatus() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ id, newStatus }) => adminOrderApi.updateStatus(id, newStatus),
        onSuccess: () => qc.invalidateQueries({ queryKey: ADMIN_ORDERS_KEY }),
    });
}