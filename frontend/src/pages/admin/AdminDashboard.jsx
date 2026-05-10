import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { LayoutDashboard, Users, ShoppingBag, Tag, MapPin, Package, ExternalLink } from "lucide-react";
import AdminOrders from "./AdminOrders";
import AdminCategories from "./AdminCategories";
import { cn } from "@/lib/utils";

// Tabs rendered inline in the dashboard
const TABS = [
    { id: "orders",     label: "Orders",     icon: ShoppingBag },
    { id: "categories", label: "Categories", icon: Tag },
];

// Links that navigate away to their own pages
const NAV_LINKS = [
    { label: "Users",          icon: Users,   path: "/admin/users" },
    { label: "Products",       icon: Package, path: "/admin/products" },
    { label: "Order Tracking", icon: MapPin,  path: "/admin/tracking" },
];

export default function AdminDashboard() {
    const [activeTab, setActiveTab] = useState("orders");
    const navigate = useNavigate();

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

            {/* Page heading */}
            <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
                    <LayoutDashboard className="w-5 h-5 text-primary-foreground" />
                </div>
                <div>
                    <h1 className="text-2xl font-extrabold">Admin Dashboard</h1>
                    <p className="text-sm text-muted-foreground">Manage orders, categories, products, and users.</p>
                </div>
            </div>

            {/* Single toolbar row */}
            <div className="flex flex-wrap items-center gap-2 p-2 bg-secondary/50 border border-border rounded-xl mb-8">

                {/* Inline tab buttons */}
                {TABS.map(({ id, label, icon: Icon }) => (
                    <button
                        key={id}
                        onClick={() => setActiveTab(id)}
                        className={cn(
                            "flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all",
                            activeTab === id
                                ? "bg-primary text-primary-foreground shadow-sm"
                                : "text-muted-foreground hover:text-foreground hover:bg-background"
                        )}
                    >
                        <Icon className="w-4 h-4" />
                        {label}
                    </button>
                ))}

                <div className="w-px h-6 bg-border mx-1 hidden sm:block" />

                {/* External nav links */}
                {NAV_LINKS.map(({ label, icon: Icon, path }) => (
                    <button
                        key={path}
                        onClick={() => navigate(path)}
                        className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-background transition-all"
                    >
                        <Icon className="w-4 h-4" />
                        {label}
                        <ExternalLink className="w-3 h-3 opacity-50" />
                    </button>
                ))}
            </div>

            {/* Active panel */}
            <div>
                {activeTab === "orders"     && <AdminOrders />}
                {activeTab === "categories" && <AdminCategories />}
            </div>
        </div>
    );
}

