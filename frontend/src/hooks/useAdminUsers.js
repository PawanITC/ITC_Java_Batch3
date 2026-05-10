import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminUserApi } from "@/lib/adminApi";

export const ADMIN_USERS_KEY = ["admin", "users"];

export function useAdminUsers() {
    return useQuery({
        queryKey: ADMIN_USERS_KEY,
        queryFn: () => adminUserApi.getAllUsers().then((r) => r?.data ?? r),
    });
}

export function useUpdateUserRole() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: ({ userId, role }) => adminUserApi.updateRole(userId, role),
        onSuccess: () => qc.invalidateQueries({ queryKey: ADMIN_USERS_KEY }),
    });
}

export function useToggleUserStatus() {
    const qc = useQueryClient();
    return useMutation({
        mutationFn: (userId) => adminUserApi.toggleStatus(userId),
        onSuccess: () => qc.invalidateQueries({ queryKey: ADMIN_USERS_KEY }),
    });
}