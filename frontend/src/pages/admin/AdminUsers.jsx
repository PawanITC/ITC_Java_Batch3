import { useState } from "react";
import { Users, Loader2, ShieldCheck, ShieldOff } from "lucide-react";
import { useAdminUsers, useUpdateUserRole } from "../../hooks/useAdminUsers";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";

export default function AdminUsers() {
    const { data: users, isLoading, isError } = useAdminUsers();
    const updateRole = useUpdateUserRole();
    const [updating, setUpdating] = useState(null);

    const handleRoleChange = async (userId, role) => {
        setUpdating(userId);
        await updateRole.mutateAsync({ userId, role });
        setUpdating(null);
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-3">
                <Users className="w-6 h-6" />
                <h2 className="text-xl font-semibold">User Management</h2>
                {users && (
                    <Badge variant="secondary">{users.length} users</Badge>
                )}
            </div>

            {isLoading && (
                <div className="flex justify-center py-12">
                    <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
                </div>
            )}

            {isError && (
                <p className="text-destructive text-sm">Failed to load users.</p>
            )}

            {users?.length > 0 && (
                <div className="bg-white border rounded-xl overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-muted/50">
                        <tr>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">User</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Email</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Status</th>
                            <th className="text-left px-4 py-3 font-medium text-muted-foreground">Role</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y">
                        {users.map((user) => (
                            <tr key={user.id} className="hover:bg-muted/20 transition-colors">
                                <td className="px-4 py-3 font-medium">
                                    <div className="flex items-center gap-2">
                                        <div className="w-7 h-7 rounded-full bg-muted flex items-center justify-center text-xs font-semibold">
                                            {(user.name || user.email || "?")[0].toUpperCase()}
                                        </div>
                                        {user.name ?? "—"}
                                    </div>
                                </td>
                                <td className="px-4 py-3 text-muted-foreground">{user.email}</td>
                                <td className="px-4 py-3">
                                    {user.isActive ? (
                                        <span className="inline-flex items-center gap-1 text-xs text-green-700">
                        <ShieldCheck className="w-3.5 h-3.5" /> Active
                      </span>
                                    ) : (
                                        <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                        <ShieldOff className="w-3.5 h-3.5" /> Inactive
                      </span>
                                    )}
                                </td>
                                <td className="px-4 py-3">
                                    {updating === user.id ? (
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                    ) : (
                                        <Select
                                            value={user.role}
                                            onValueChange={(role) => handleRoleChange(user.id, role)}
                                        >
                                            <SelectTrigger className="w-32 h-7 text-xs">
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="ROLE_USER" className="text-xs">User</SelectItem>
                                                <SelectItem value="ROLE_ADMIN" className="text-xs">Admin</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    )}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}