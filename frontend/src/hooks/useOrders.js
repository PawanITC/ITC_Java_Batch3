import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "../lib/orderApi";

export const ORDER_HISTORY_KEY = ["orders", "history"];

export function useOrderHistory() {
    return useQuery({
        queryKey: ORDER_HISTORY_KEY,
        queryFn: () => orderApi.getHistory().then((r) => r?.data ?? r),
        // Always fetch fresh data when the page is opened — never show a stale list.
        refetchOnMount: "always",
        staleTime: 0,
        // Keep polling every 3 s while any order is PENDING so the list automatically
        // reflects the status once the payment saga completes on the backend.
        refetchInterval: (query) => {
            const orders = query.state.data;
            return Array.isArray(orders) && orders.some((o) => o.orderStatus === "PENDING")
                ? 3000
                : false;
        },
    });
}

export function useOrder(id) {
    return useQuery({
        queryKey: ["orders", id],
        queryFn: () => orderApi.getOrder(id).then((r) => r?.data ?? r),
        enabled: !!id,
    });
}

export function useCancelOrder() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (id) => orderApi.cancelOrder(id),
        onSuccess: () => qc.invalidateQueries({ queryKey: ORDER_HISTORY_KEY }),
    });
}