import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Users, Loader2, ShieldCheck, ShieldOff, ArrowLeft, Search, UserCheck, UserX } from "lucide-react";
import { useAdminUsers, useUpdateUserRole, useToggleUserStatus } from "@/hooks/useAdminUsers";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/use-toast";
import { cn } from "@/lib/utils";

export default function AdminUsersPage() {
    const navigate = useNavigate();
    const { data: users, isLoading, isError, refetch } = useAdminUsers();
    const updateRole = useUpdateUserRole();
    const toggleStatus = useToggleUserStatus();
    const { toast } = useToast();
    const [updating, setUpdating] = useState(null);
    const [toggling, setToggling] = useState(null);
    const [search, setSearch] = useState("");

    const handleRoleChange = async (userId, role) => {
        setUpdating(userId);
        try {
            await updateRole.mutateAsync({ userId, role });
            toast({ title: "Role updated", description: "User role changed successfully." });
        } catch {
            toast({ title: "Update failed", description: "Could not change the user's role.", variant: "destructive" });
        } finally {
            setUpdating(null);
        }
    };

    const handleToggleStatus = async (userId, currentlyActive) => {
        setToggling(userId);
        try {
            await toggleStatus.mutateAsync(userId);
            toast({ title: currentlyActive ? "User deactivated" : "User activated", description: currentlyActive ? "Account has been suspended." : "Account is now active." });
        } catch {
            toast({ title: "Action failed", description: "Could not change account status.", variant: "destructive" });
        } finally {
            setToggling(null);
        }
    };

    const filtered = (users ?? []).filter((u) => {
        const q = search.toLowerCase();
        return !q || (u.name ?? "").toLowerCase().includes(q) || (u.email ?? "").toLowerCase().includes(q);
    });

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
            {/* Back */}
            <button
                onClick={() => navigate("/admin")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Dashboard
            </button>

            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
                        <Users className="w-5 h-5 text-primary-foreground" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-extrabold">User Management</h1>
                        <p className="text-sm text-muted-foreground">
                            Promote, demote, or monitor all registered users.
                        </p>
                    </div>
                </div>
                {users && (
                    <Badge variant="secondary" className="text-sm px-3 py-1 self-start sm:self-auto">
                        {users.length} total users
                    </Badge>
                )}
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                    placeholder="Search by name or email…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="pl-10 bg-background"
                />
            </div>

            {/* States */}
            {isLoading && (
                <div className="flex justify-center py-16">
                    <Loader2 className="w-7 h-7 animate-spin text-muted-foreground" />
                </div>
            )}

            {isError && (
                <div className="bg-destructive/10 text-destructive rounded-xl px-6 py-4 flex items-center justify-between">
                    <p>Failed to load users.</p>
                    <Button variant="outline" size="sm" onClick={refetch}>Retry</Button>
                </div>
            )}

            {/* Table */}
            {!isLoading && !isError && (
                <div className="bg-card border border-border rounded-xl overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-secondary/60">
                        <tr>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground">User</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground hidden sm:table-cell">Email</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground">Status</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground">Role</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-border">
                        {filtered.length === 0 && (
                            <tr>
                                <td colSpan={4} className="text-center py-12 text-muted-foreground text-sm">
                                    No users found.
                                </td>
                            </tr>
                        )}
                        {filtered.map((user) => {
                            const initials = (user.name || user.email || "?")[0].toUpperCase();
                            const hasRole = (r) =>
                                user.role === r || (Array.isArray(user.roles) && user.roles.includes(r));
                            const currentRole = hasRole("ROLE_ADMIN") ? "ROLE_ADMIN"
                                : hasRole("ROLE_MODERATOR") ? "ROLE_MODERATOR"
                                : "ROLE_USER";
                            return (
                                <tr key={user.id} className="hover:bg-muted/20 transition-colors">
                                    <td className="px-5 py-4">
                                        <div className="flex items-center gap-3">
                                            <div className={cn(
                                                "w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-primary-foreground shrink-0",
                                                currentRole === "ROLE_ADMIN" ? "bg-accent"
                                                    : currentRole === "ROLE_MODERATOR" ? "bg-amber-500"
                                                    : "bg-primary"
                                            )}>
                                                {initials}
                                            </div>
                                            <div>
                                                <p className="font-semibold">{user.name ?? "—"}</p>
                                                <p className="text-xs text-muted-foreground sm:hidden">{user.email}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-5 py-4 text-muted-foreground hidden sm:table-cell">{user.email}</td>
                                    <td className="px-5 py-4">
                                        {user.isActive ? (
                                            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-green-700 bg-green-50 px-2 py-0.5 rounded-full border border-green-200">
                          <ShieldCheck className="w-3 h-3" /> Active
                        </span>
                                        ) : (
                                            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground bg-secondary px-2 py-0.5 rounded-full border border-border">
                          <ShieldOff className="w-3 h-3" /> Inactive
                        </span>
                                        )}
                                    </td>
                                    <td className="px-5 py-4">
                                        {updating === user.id ? (
                                            <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />
                                        ) : (
                                            <Select
                                                value={currentRole}
                                                onValueChange={(role) => handleRoleChange(user.id, role)}
                                            >
                                                <SelectTrigger className="w-36 h-8 text-xs">
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="ROLE_USER" className="text-xs">User</SelectItem>
                                                    <SelectItem value="ROLE_MODERATOR" className="text-xs">Moderator</SelectItem>
                                                    <SelectItem value="ROLE_ADMIN" className="text-xs">Admin</SelectItem>
                                                </SelectContent>
                                            </Select>
                                        )}
                                    </td>
                                    <td className="px-5 py-4">
                                        <button
                                            onClick={() => handleToggleStatus(user.id, user.isActive)}
                                            disabled={toggling === user.id}
                                            className={cn(
                                                "inline-flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg border transition-colors",
                                                user.isActive
                                                    ? "border-destructive/40 text-destructive hover:bg-destructive/10"
                                                    : "border-green-300 text-green-700 hover:bg-green-50"
                                            )}
                                        >
                                            {toggling === user.id ? (
                                                <Loader2 className="w-3 h-3 animate-spin" />
                                            ) : user.isActive ? (
                                                <><UserX className="w-3 h-3" /> Deactivate</>
                                            ) : (
                                                <><UserCheck className="w-3 h-3" /> Activate</>
                                            )}
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
