import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { cartApi } from "../lib/cartApi";

export const CART_QUERY_KEY = ["cart"];

/** Fetch cart via React Query (for components that prefer RQ over context) */
export function useCartQuery() {
    return useQuery({
        queryKey: CART_QUERY_KEY,
        queryFn: cartApi.getCart,
        staleTime: 1000 * 30,
    });
}

export function useAddItemMutation() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ productId, quantity }) => cartApi.addItem(productId, quantity),
        onSuccess: (data) => qc.setQueryData(CART_QUERY_KEY, data),
    });
}

export function useUpdateItemMutation() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ productId, quantity }) => cartApi.updateItem(productId, quantity),
        onSuccess: (data) => qc.setQueryData(CART_QUERY_KEY, data),
    });
}

export function useRemoveItemMutation() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ productId }) => cartApi.removeItem(productId),
        onSuccess: (data) => qc.setQueryData(CART_QUERY_KEY, data),
    });
}