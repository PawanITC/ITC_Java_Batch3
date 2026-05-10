import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { User, Mail, Shield, KeyRound, Pencil, Check, X, ArrowLeft, Eye, EyeOff, Loader2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/use-toast";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------
async function apiPatch(path, body) {
    const res = await fetch(path, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(body),
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message ?? `Request failed (${res.status})`);
    }
    return res.json();
}

// ---------------------------------------------------------------------------
// Role badge
// ---------------------------------------------------------------------------
const ROLE_LABELS = { ROLE_ADMIN: "Admin", ROLE_MODERATOR: "Moderator", ROLE_USER: "User" };
const ROLE_COLOURS = {
    ROLE_ADMIN: "bg-accent text-accent-foreground",
    ROLE_MODERATOR: "bg-amber-100 text-amber-800 border border-amber-300",
    ROLE_USER: "bg-secondary text-secondary-foreground border border-border",
};

function RoleBadge({ role }) {
    const label = ROLE_LABELS[role] ?? role;
    const colour = ROLE_COLOURS[role] ?? ROLE_COLOURS.ROLE_USER;
    return (
        <span className={cn("inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full", colour)}>
            <Shield className="w-3.5 h-3.5" />
            {label}
        </span>
    );
}

// ---------------------------------------------------------------------------
// Inline editable field
// ---------------------------------------------------------------------------
function EditableField({ label, value, onSave, icon: Icon }) {
    const [editing, setEditing] = useState(false);
    const [draft, setDraft] = useState(value ?? "");
    const [saving, setSaving] = useState(false);
    const { toast } = useToast();

    const handleSave = async () => {
        if (draft.trim() === (value ?? "").trim()) { setEditing(false); return; }
        setSaving(true);
        try {
            await onSave(draft.trim());
            setEditing(false);
            toast({ title: "Saved", description: `${label} updated.` });
        } catch (err) {
            toast({ title: "Update failed", description: err?.message ?? "Something went wrong.", variant: "destructive" });
        } finally {
            setSaving(false);
        }
    };

    const handleCancel = () => { setDraft(value ?? ""); setEditing(false); };

    return (
        <div className="flex items-start gap-3 py-4 border-b border-border last:border-0">
            <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center shrink-0 mt-0.5">
                <Icon className="w-4 h-4 text-muted-foreground" />
            </div>
            <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-muted-foreground mb-1">{label}</p>
                {editing ? (
                    <div className="flex items-center gap-2">
                        <input
                            autoFocus
                            value={draft}
                            onChange={(e) => setDraft(e.target.value)}
                            onKeyDown={(e) => { if (e.key === "Enter") handleSave(); if (e.key === "Escape") handleCancel(); }}
                            className="flex-1 text-sm border border-primary/50 rounded-lg px-3 py-1.5 bg-background focus:outline-none focus:ring-2 focus:ring-primary/30"
                        />
                        <button onClick={handleSave} disabled={saving} className="text-green-600 hover:text-green-700 p-1 disabled:opacity-50">
                            {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                        </button>
                        <button onClick={handleCancel} className="text-muted-foreground hover:text-foreground p-1"><X className="w-4 h-4" /></button>
                    </div>
                ) : (
                    <div className="flex items-center justify-between gap-2">
                        <p className="text-sm font-medium truncate">{value ?? <span className="text-muted-foreground italic">Not set</span>}</p>
                        <button onClick={() => { setDraft(value ?? ""); setEditing(true); }} className="shrink-0 p-1 text-muted-foreground hover:text-foreground transition-colors">
                            <Pencil className="w-3.5 h-3.5" />
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

// ---------------------------------------------------------------------------
// Password change section
// ---------------------------------------------------------------------------
function ChangePasswordSection({ isOAuthUser }) {
    const { toast } = useToast();
    const [open, setOpen] = useState(false);
    const [current, setCurrent] = useState("");
    const [next, setNext] = useState("");
    const [confirm, setConfirm] = useState("");
    const [showCurrent, setShowCurrent] = useState(false);
    const [showNext, setShowNext] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (next !== confirm) { toast({ title: "Passwords don't match", variant: "destructive" }); return; }
        if (next.length < 8) { toast({ title: "Password too short", description: "Minimum 8 characters.", variant: "destructive" }); return; }
        setLoading(true);
        try {
            await apiPatch("/api/v1/users/password", { currentPassword: current, newPassword: next });
            toast({ title: "Password changed", description: "Your password has been updated." });
            setCurrent(""); setNext(""); setConfirm(""); setOpen(false);
        } catch (err) {
            toast({ title: "Failed", description: err?.message ?? "Could not change password.", variant: "destructive" });
        } finally {
            setLoading(false);
        }
    };

    if (isOAuthUser) {
        return (
            <div className="flex items-center gap-3 py-4 border-b border-border">
                <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center shrink-0">
                    <KeyRound className="w-4 h-4 text-muted-foreground" />
                </div>
                <div>
                    <p className="text-xs font-medium text-muted-foreground">Password</p>
                    <p className="text-sm text-muted-foreground italic mt-0.5">Managed via OAuth — password change unavailable</p>
                </div>
            </div>
        );
    }

    return (
        <div className="py-4 border-b border-border last:border-0">
            <div className="flex items-center gap-3 mb-3">
                <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center shrink-0">
                    <KeyRound className="w-4 h-4 text-muted-foreground" />
                </div>
                <div className="flex-1">
                    <p className="text-xs font-medium text-muted-foreground">Password</p>
                    <p className="text-sm">••••••••</p>
                </div>
                <Button variant="ghost" size="sm" onClick={() => setOpen((v) => !v)} className="text-xs">
                    {open ? "Cancel" : "Change"}
                </Button>
            </div>
            {open && (
                <form onSubmit={handleSubmit} className="ml-11 space-y-3">
                    {[
                        { label: "Current password", value: current, onChange: setCurrent, show: showCurrent, toggle: () => setShowCurrent((v) => !v) },
                        { label: "New password", value: next, onChange: setNext, show: showNext, toggle: () => setShowNext((v) => !v) },
                        { label: "Confirm new password", value: confirm, onChange: setConfirm, show: showNext, toggle: null },
                    ].map(({ label, value, onChange, show, toggle }) => (
                        <div key={label}>
                            <label className="text-xs font-medium text-muted-foreground mb-1 block">{label}</label>
                            <div className="relative">
                                <input
                                    type={show ? "text" : "password"}
                                    required
                                    value={value}
                                    onChange={(e) => onChange(e.target.value)}
                                    className="w-full text-sm border border-border rounded-lg px-3 py-2 pr-10 bg-background focus:outline-none focus:ring-2 focus:ring-primary/30"
                                />
                                {toggle && (
                                    <button type="button" onClick={toggle} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                                        {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                    <Button type="submit" size="sm" disabled={loading} className="gap-1.5">
                        {loading ? <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Saving…</> : "Save Password"}
                    </Button>
                </form>
            )}
        </div>
    );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------
export default function ProfilePage() {
    const navigate = useNavigate();
    const { user, refreshUser } = useAuth();

    const initial = ((user?.name ?? user?.email ?? "?")[0] ?? "?").toUpperCase();
    const isOAuth = user?.password === "{OAUTH}" || !user?.hasPassword;

    const handleNameSave = async (newName) => {
        await apiPatch("/api/v1/users/profile", { name: newName });
        await refreshUser();
    };

    return (
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
            {/* Back */}
            <button
                onClick={() => navigate(-1)}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back
            </button>

            {/* Avatar + role */}
            <div className="bg-card border border-border rounded-xl p-8 flex flex-col items-center gap-4">
                <div className="w-20 h-20 rounded-full bg-primary flex items-center justify-center text-3xl font-extrabold text-primary-foreground">
                    {initial}
                </div>
                <div className="text-center">
                    <p className="font-extrabold text-xl">{user?.name ?? "—"}</p>
                    <p className="text-sm text-muted-foreground mt-0.5">{user?.email}</p>
                </div>
                <RoleBadge role={user?.role} />
            </div>

            {/* Account details */}
            <div className="bg-card border border-border rounded-xl px-6 divide-y divide-border">
                <h2 className="font-bold text-base py-4">Account Details</h2>
                <EditableField
                    label="Display Name"
                    value={user?.name}
                    icon={User}
                    onSave={handleNameSave}
                />
                <div className="flex items-center gap-3 py-4 border-b border-border">
                    <div className="w-8 h-8 rounded-lg bg-secondary flex items-center justify-center shrink-0">
                        <Mail className="w-4 h-4 text-muted-foreground" />
                    </div>
                    <div>
                        <p className="text-xs font-medium text-muted-foreground">Email</p>
                        <p className="text-sm font-medium mt-0.5">{user?.email}</p>
                        <p className="text-xs text-muted-foreground mt-0.5">Email cannot be changed</p>
                    </div>
                </div>
                <ChangePasswordSection isOAuthUser={isOAuth} />
            </div>

            {/* Account status */}
            <div className="bg-card border border-border rounded-xl px-6 py-4">
                <h2 className="font-bold text-base mb-3">Account Status</h2>
                <div className="flex items-center gap-3">
                    <div className={cn("w-2.5 h-2.5 rounded-full", user?.isActive !== false ? "bg-green-500" : "bg-muted")} />
                    <span className="text-sm">{user?.isActive !== false ? "Active — your account is in good standing." : "Inactive — contact support."}</span>
                </div>
            </div>
        </div>
    );
}
