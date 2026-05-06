import { useState } from "react";
import { Bell, Package, Tag, ShieldCheck, Info, CheckCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const DUMMY_NOTIFICATIONS = [
    { id: 1, type: "order",  read: false, title: "Your order has shipped!", body: "Order #1042 is on its way. Expected delivery: May 9, 2026.", time: "2 hours ago" },
    { id: 2, type: "promo",  read: false, title: "Flash Sale — 20% off prints", body: "Use code FUNKART20 at checkout. Ends tonight at midnight.", time: "5 hours ago" },
    { id: 3, type: "order",  read: true,  title: "Order delivered", body: "Order #1038 was delivered on May 4. Leave a review!", time: "2 days ago" },
    { id: 4, type: "system", read: true,  title: "Welcome to FunkArt!", body: "Thanks for joining us. Browse our latest collection and find your next favourite piece.", time: "3 days ago" },
    { id: 5, type: "promo",  read: true,  title: "New arrivals just dropped", body: "10 new limited-edition prints are now available. Shop before they sell out!", time: "5 days ago" },
    { id: 6, type: "system", read: true,  title: "Profile updated", body: "Your account details were successfully updated.", time: "1 week ago" },
];

const TYPE_ICONS = {
    order:  { Icon: Package,     bg: "bg-blue-50",   color: "text-blue-600" },
    promo:  { Icon: Tag,         bg: "bg-amber-50",  color: "text-amber-600" },
    system: { Icon: Info,        bg: "bg-secondary", color: "text-muted-foreground" },
    admin:  { Icon: ShieldCheck, bg: "bg-purple-50", color: "text-purple-600" },
};

function NotifCard({ notif, onRead }) {
    const { Icon, bg, color } = TYPE_ICONS[notif.type] ?? TYPE_ICONS.system;
    return (
        <div
            className={cn(
                "flex gap-4 p-4 rounded-xl border transition-colors cursor-pointer group",
                notif.read
                    ? "bg-white border-border"
                    : "bg-blue-50/40 border-blue-200 hover:bg-blue-50/60"
            )}
            onClick={() => !notif.read && onRead(notif.id)}
        >
            <div className={cn("w-10 h-10 rounded-full flex items-center justify-center shrink-0", bg)}>
                <Icon className={cn("w-5 h-5", color)} />
            </div>
            <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                    <p className={cn("text-sm font-semibold", notif.read && "font-medium text-foreground")}>
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

export default function NotificationsPage() {
    const [notifications, setNotifications] = useState(DUMMY_NOTIFICATIONS);

    const unreadCount = notifications.filter((n) => !n.read).length;

    const markRead = (id) =>
        setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));

    const markAllRead = () =>
        setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));

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
                        {unreadCount > 0 ? `${unreadCount} unread notification${unreadCount > 1 ? "s" : ""}` : "All caught up!"}
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
                {["All", "Orders", "Promos"].map((tab) => (
                    <span
                        key={tab}
                        className="px-4 py-1.5 rounded-full text-sm font-medium bg-secondary text-muted-foreground cursor-default select-none"
                    >
            {tab}
          </span>
                ))}
            </div>

            {/* List */}
            <div className="space-y-3">
                {notifications.map((n) => (
                    <NotifCard key={n.id} notif={n} onRead={markRead} />
                ))}
            </div>
        </div>
    );
}