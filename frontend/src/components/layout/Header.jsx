import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ShoppingCart, LogOut, LayoutDashboard, ClipboardList, Store, Menu, X, Package2, Bell } from "lucide-react";
import CartDrawer from "../cart/CartDrawer";
import { useCurrentUser, isAdmin } from "../../hooks/useCurrentUser";
import { useCart } from "../../context/CartContext.jsx";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function Header({ onLogout }) {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [mobileOpen, setMobileOpen] = useState(false);
    const { data: currentUser } = useCurrentUser();
    const admin = isAdmin(currentUser);
    const { itemCount } = useCart();
    const { pathname } = useLocation();

    const navLinks = [
        { to: "/products",      label: "Shop",         icon: Store },
        { to: "/orders",        label: "My Orders",    icon: ClipboardList },
        { to: "/notifications", label: "Notifications", icon: Bell },
        ...(admin ? [{ to: "/admin", label: "Admin", icon: LayoutDashboard }] : []),
    ];

    return (
        <>
            <header className="sticky top-0 z-40 w-full bg-white border-b border-border shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between h-16">

                        {/* Logo */}
                        <Link to="/" className="flex items-center gap-2.5 group">
                            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
                                <Package2 className="w-5 h-5 text-primary-foreground" />
                            </div>
                            <span className="font-extrabold text-xl tracking-tight text-foreground">
                Funk<span className="text-accent">Art</span>
              </span>
                        </Link>

                        {/* Desktop nav */}
                        <nav className="hidden md:flex items-center gap-1">
                            {navLinks.map(({ to, label }) => (
                                <Link
                                    key={to}
                                    to={to}
                                    className={cn(
                                        "px-4 py-2 rounded-lg text-sm font-medium transition-colors",
                                        pathname.startsWith(to)
                                            ? "bg-primary text-primary-foreground"
                                            : "text-muted-foreground hover:text-foreground hover:bg-secondary"
                                    )}
                                >
                                    {label}
                                </Link>
                            ))}
                        </nav>

                        {/* Right actions */}
                        <div className="flex items-center gap-2">
                            {currentUser && (
                                <span className="hidden lg:block text-sm text-muted-foreground mr-2">
                  Hey, <span className="font-semibold text-foreground">{currentUser.name?.split(" ")[0] || currentUser.email}</span>
                </span>
                            )}

                            {/* Cart button */}
                            <button
                                onClick={() => setDrawerOpen(true)}
                                className="relative p-2 rounded-lg hover:bg-secondary transition-colors"
                                aria-label="Open cart"
                            >
                                <ShoppingCart className="w-5 h-5 text-foreground" />
                                {itemCount > 0 && (
                                    <span className="absolute -top-0.5 -right-0.5 w-4 h-4 rounded-full bg-accent text-accent-foreground text-[10px] font-bold flex items-center justify-center leading-none">
                    {itemCount > 9 ? "9+" : itemCount}
                  </span>
                                )}
                            </button>

                            {/* Logout */}
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={onLogout}
                                className="text-muted-foreground hover:text-foreground"
                                aria-label="Logout"
                            >
                                <LogOut className="w-5 h-5" />
                            </Button>

                            {/* Mobile menu toggle */}
                            <button
                                className="md:hidden p-2 rounded-lg hover:bg-secondary transition-colors"
                                onClick={() => setMobileOpen((v) => !v)}
                            >
                                {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Mobile nav */}
                {mobileOpen && (
                    <div className="md:hidden border-t bg-white px-4 py-3 space-y-1">
                        {navLinks.map(({ to, label, icon: Icon }) => (
                            <Link
                                key={to}
                                to={to}
                                onClick={() => setMobileOpen(false)}
                                className={cn(
                                    "flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors",
                                    pathname.startsWith(to)
                                        ? "bg-primary text-primary-foreground"
                                        : "text-muted-foreground hover:text-foreground hover:bg-secondary"
                                )}
                            >
                                <Icon className="w-4 h-4" />
                                {label}
                            </Link>
                        ))}
                    </div>
                )}
            </header>

            <CartDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
        </>
    );
}