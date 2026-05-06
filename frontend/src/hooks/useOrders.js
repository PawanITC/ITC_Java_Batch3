import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { orderApi } from "../lib/orderApi";

export const ORDER_HISTORY_KEY = ["orders", "history"];

export function useOrderHistory() {
    return useQuery({
        queryKey: ORDER_HISTORY_KEY,
        queryFn: () => orderApi.getHistory().then((r) => r?.data ?? r),
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