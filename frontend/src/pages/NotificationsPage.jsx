import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Bell, Package, Tag, ShieldCheck, Info, CheckCheck, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { orderApi } from "@/lib/orderApi";

// ---------------------------------------------------------------------------
// Static promo / system dummy notifications (always shown)
// ---------------------------------------------------------------------------
const DUMMY_PROMOS = [
    { id: "promo-1", type: "promo",  read: false, title: "Flash Sale — 20% off prints", body: "Use code FUNKART20 at checkout. Ends tonight at midnight.", time: "5 hours ago", sortTs: Date.now() - 5 * 60 * 60 * 1000 },
    { id: "promo-2", type: "system", read: true,  title: "Welcome to FunkArt!", body: "Thanks for joining us. Browse our latest collection.", time: "3 days ago", sortTs: Date.now() - 3 * 24 * 60 * 60 * 1000 },
    { id: "promo-3", type: "promo",  read: true,  title: "New arrivals just dropped", body: "10 new limited-edition prints are now available. Shop before they sell out!", time: "5 days ago", sortTs: Date.now() - 5 * 24 * 60 * 60 * 1000 },
    { id: "promo-4", type: "system", read: true,  title: "Profile updated", body: "Your account details were successfully updated.", time: "1 week ago", sortTs: Date.now() - 7 * 24 * 60 * 60 * 1000 },
];

// ---------------------------------------------------------------------------
// Order status → readable notification copy
// ---------------------------------------------------------------------------
const ORDER_STATUS_COPY = {
    PENDING:   { title: (id) => `Order #${id} placed`,           body: "Your order is being processed and awaiting payment confirmation." },
    PAID:      { title: (id) => `Payment confirmed — Order #${id}`, body: "Great news! Your payment was verified and your order is being prepared." },
    CONFIRMED: { title: (id) => `Order #${id} confirmed`,        body: "Your order has been confirmed and will be dispatched soon." },
    SHIPPED:   { title: (id) => `Order #${id} has shipped!`,     body: "Your order is on its way. Check your order details for tracking info." },
    DELIVERED: { title: (id) => `Order #${id} delivered`,        body: "Your order was delivered. Leave a review to share your experience!" },
    CANCELLED: { title: (id) => `Order #${id} was cancelled`,    body: "Your order has been cancelled. Any payment will be refunded shortly." },
    FAILED:    { title: (id) => `Order #${id} payment failed`,   body: "Your payment could not be processed. Please try placing the order again." },
    REFUNDED:  { title: (id) => `Order #${id} refunded`,         body: "Your refund has been initiated. Funds should appear within 3–5 business days." },
};

// ---------------------------------------------------------------------------
// Relative time helper
// ---------------------------------------------------------------------------
function relativeTime(dateStr) {
    if (!dateStr) return "";
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins  = Math.floor(diff / 60_000);
    const hours = Math.floor(diff / 3_600_000);
    const days  = Math.floor(diff / 86_400_000);
    if (mins < 2)   return "just now";
    if (mins < 60)  return `${mins} minutes ago`;
    if (hours < 24) return `${hours} hour${hours > 1 ? "s" : ""} ago`;
    if (days < 7)   return `${days} day${days > 1 ? "s" : ""} ago`;
    return new Date(dateStr).toLocaleDateString("en-GB", { day: "numeric", month: "short" });
}

// ---------------------------------------------------------------------------
// Convert OrderResponse[] → notification shape
// ---------------------------------------------------------------------------
function ordersToNotifications(orders) {
    if (!Array.isArray(orders)) return [];
    return orders.map((order) => {
        const copy = ORDER_STATUS_COPY[order.orderStatus] ?? {
            title: (id) => `Order #${id} updated`,
            body: `Status: ${order.orderStatus}`,
        };
        const ts = order.createdAt ? new Date(order.createdAt).getTime() : Date.now();
        return {
            id: `order-${order.orderId}`,
            type: "order",
            read: ["DELIVERED", "CANCELLED", "REFUNDED"].includes(order.orderStatus),
            title: copy.title(order.orderId),
            body: copy.body,
            time: relativeTime(order.createdAt),
            sortTs: ts,
            orderId: order.orderId,
            orderStatus: order.orderStatus,
        };
    });
}

// ---------------------------------------------------------------------------
// UI constants
// ---------------------------------------------------------------------------
const TYPE_ICONS = {
    order:  { Icon: Package,     bg: "bg-blue-50",   color: "text-blue-600" },
    promo:  { Icon: Tag,         bg: "bg-amber-50",  color: "text-amber-600" },
    system: { Icon: Info,        bg: "bg-secondary", color: "text-muted-foreground" },
    admin:  { Icon: ShieldCheck, bg: "bg-purple-50", color: "text-purple-600" },
};

const TABS = ["All", "Orders", "Promos"];

// ---------------------------------------------------------------------------
// NotifCard
// ---------------------------------------------------------------------------
function NotifCard({ notif, onRead }) {
    const { Icon, bg, color } = TYPE_ICONS[notif.type] ?? TYPE_ICONS.system;
    return (
        <div
            className={cn(
                "flex gap-4 p-4 rounded-xl border transition-colors cursor-pointer",
                notif.read
                    ? "bg-card border-border"
                    : "bg-blue-50/40 border-blue-200 hover:bg-blue-50/60"
            )}
            onClick={() => !notif.read && onRead(notif.id)}
        >
            <div className={cn("w-10 h-10 rounded-full flex items-center justify-center shrink-0", bg)}>
                <Icon className={cn("w-5 h-5", color)} />
            </div>
            <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                    <p className={cn("text-sm", notif.read ? "font-medium text-foreground" : "font-semibold")}>
                        {notif.title}
                    </p>
                    {!notif.read && (
                        <span className="w-2 h-2 rounded-full bg-blue-500 shrink-0 mt-1.5" />
                    )}
                </div>
                <p className="text-sm text-muted-foreground mt-0.5 leading-relaxed">{notif.body}</p>
                <p className="text-xs text-muted-foreground mt-1.5">{notif.time}</p>
            </div>
        </div>
    );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------
export default function NotificationsPage() {
    const [activeTab, setActiveTab]       = useState("All");
    const [readIds,   setReadIds]         = useState(new Set());

    // Fetch real orders for order-event notifications
    const { data: orders, isLoading } = useQuery({
        queryKey: ["orderHistory"],
        queryFn: () => orderApi.getHistory().then(res => res?.data ?? res),
        retry: 1,
        staleTime: 60_000,
    });

    // Build merged + sorted notification list
    const allNotifications = useMemo(() => {
        const orderNotifs = ordersToNotifications(orders);
        const merged = [...orderNotifs, ...DUMMY_PROMOS];
        // Apply in-memory read state on top of initial read flag
        return merged
            .map((n) => ({ ...n, read: n.read || readIds.has(n.id) }))
            .sort((a, b) => b.sortTs - a.sortTs);
    }, [orders, readIds]);

    const filtered = useMemo(() => {
        if (activeTab === "Orders") return allNotifications.filter((n) => n.type === "order");
        if (activeTab === "Promos") return allNotifications.filter((n) => n.type === "promo" || n.type === "system");
        return allNotifications;
    }, [allNotifications, activeTab]);

    const unreadCount = allNotifications.filter((n) => !n.read).length;

    const markRead = (id) => setReadIds((prev) => new Set([...prev, id]));

    const markAllRead = () =>
        setReadIds(new Set(allNotifications.map((n) => n.id)));

    return (
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <div className="flex items-center gap-3">
                        <Bell className="w-6 h-6" />
                        <h1 className="text-2xl font-extrabold">Notifications</h1>
                        {unreadCount > 0 && (
                            <span className="bg-blue-500 text-white text-xs font-bold rounded-full px-2.5 py-0.5">
                                {unreadCount}
                            </span>
                        )}
                    </div>
                    <p className="text-sm text-muted-foreground mt-1">
                        {isLoading
                            ? "Loading your order updates…"
                            : unreadCount > 0
                                ? `${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}`
                                : "All caught up!"}
                    </p>
                </div>
                {unreadCount > 0 && (
                    <Button variant="outline" size="sm" onClick={markAllRead} className="gap-2">
                        <CheckCheck className="w-4 h-4" /> Mark all read
                    </Button>
                )}
            </div>

            {/* Tabs */}
            <div className="flex gap-2">
                {TABS.map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={cn(
                            "px-4 py-1.5 rounded-full text-sm font-medium transition-colors",
                            activeTab === tab
                                ? "bg-foreground text-background"
                                : "bg-secondary text-muted-foreground hover:text-foreground"
                        )}
                    >
                        {tab}
                    </button>
                ))}
            </div>

            {/* Loading state */}
            {isLoading && (
                <div className="flex justify-center py-6">
                    <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
                </div>
            )}

            {/* List */}
            {!isLoading && (
                <div className="space-y-3">
                    {filtered.length === 0 ? (
                        <p className="text-sm text-muted-foreground text-center py-8">
                            No {activeTab !== "All" ? activeTab.toLowerCase() : ""} notifications yet.
                        </p>
                    ) : (
                        filtered.map((n) => (
                            <NotifCard key={n.id} notif={n} onRead={markRead} />
                        ))
                    )}
                </div>
            )}
        </div>
    );
}
