import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { LayoutDashboard, Users, ShoppingBag, Tag, ArrowRight, MapPin, Package } from "lucide-react";
import AdminOrders from "./AdminOrders";
import AdminCategories from "./AdminCategories";

const TABS = [
    { id: "orders",     label: "Orders",     icon: ShoppingBag },
    { id: "categories", label: "Categories", icon: Tag },
];

export default function AdminDashboard() {
    const [activeTab, setActiveTab] = useState("orders");
    const navigate = useNavigate();

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
            <div className="flex items-center gap-3 mb-8">
                <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
                    <LayoutDashboard className="w-5 h-5 text-primary-foreground" />
                </div>
                <div>
                    <h1 className="text-2xl font-extrabold">Admin Dashboard</h1>
                    <p className="text-sm text-muted-foreground">Manage orders, categories, and users.</p>
                </div>
            </div>

            {/* Quick action cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
                <button
                    onClick={() => navigate("/admin/users")}
                    className="flex items-center justify-between bg-white border border-border rounded-xl p-5 hover:shadow-md hover:border-primary/30 transition-all text-left group"
                >
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center">
                            <Users className="w-5 h-5 text-primary" />
                        </div>
                        <div>
                            <p className="font-bold text-sm">User Management</p>
                            <p className="text-xs text-muted-foreground">Roles &amp; accounts</p>
                        </div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
                </button>

                <button
                    onClick={() => navigate("/admin/products")}
                    className="flex items-center justify-between bg-white border border-border rounded-xl p-5 hover:shadow-md hover:border-primary/30 transition-all text-left group"
                >
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center">
                            <Package className="w-5 h-5 text-primary" />
                        </div>
                        <div>
                            <p className="font-bold text-sm">Products</p>
                            <p className="text-xs text-muted-foreground">Catalog &amp; inventory</p>
                        </div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
                </button>

                <button
                    onClick={() => navigate("/admin/tracking")}
                    className="flex items-center justify-between bg-white border border-border rounded-xl p-5 hover:shadow-md hover:border-primary/30 transition-all text-left group"
                >
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center">
                            <MapPin className="w-5 h-5 text-primary" />
                        </div>
                        <div>
                            <p className="font-bold text-sm">Order Tracking</p>
                            <p className="text-xs text-muted-foreground">Look up any order by ID</p>
                        </div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
                </button>

                {TABS.map(({ id, label, icon: Icon }) => (
                    <button
                        key={id}
                        onClick={() => setActiveTab(id)}
                        className={`flex items-center justify-between bg-white border rounded-xl p-5 hover:shadow-md transition-all text-left group
              ${activeTab === id ? "border-primary/50 shadow-sm" : "border-border hover:border-primary/30"}`}
                    >
                        <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center">
                                <Icon className="w-5 h-5 text-primary" />
                            </div>
                            <div>
                                <p className="font-bold text-sm">{label}</p>
                                <p className="text-xs text-muted-foreground">Manage {label.toLowerCase()}</p>
                            </div>
                        </div>
                        <ArrowRight className="w-4 h-4 text-muted-foreground group-hover:text-foreground transition-colors" />
                    </button>
                ))}
            </div>

            {/* Tab strip */}
            <div className="flex gap-1 border-b border-border mb-8">
                {TABS.map(({ id, label, icon: Icon }) => (
                    <button
                        key={id}
                        onClick={() => setActiveTab(id)}
                        className={`flex items-center gap-2 px-5 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px
              ${activeTab === id
                            ? "border-primary text-foreground"
                            : "border-transparent text-muted-foreground hover:text-foreground hover:border-border"
                        }`}
                    >
                        <Icon className="w-4 h-4" />
                        {label}
                    </button>
                ))}
            </div>

            <div>
                {activeTab === "orders"     && <AdminOrders />}
                {activeTab === "categories" && <AdminCategories />}
            </div>
        </div>
    );
}