import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ShoppingCart, LogOut, LayoutDashboard, ClipboardList, Store, Menu, X, Bell, UserCircle, Sun, Moon } from "lucide-react";
import CartDrawer from "../cart/CartDrawer";
import LogoMark from "./LogoMark";
import { useCurrentUser, isAdmin } from "../../hooks/useCurrentUser";
import { useCart } from "../../context/CartContext.jsx";
import { useTheme } from "../../context/ThemeContext.jsx";
import { cn } from "@/lib/utils";

export default function Header({ onLogout }) {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [mobileOpen, setMobileOpen] = useState(false);
    const { dark, toggle: toggleTheme } = useTheme();
    const { data: currentUser } = useCurrentUser();
    const admin = isAdmin(currentUser);
    const { itemCount } = useCart();
    const { pathname } = useLocation();
    const navigate = useNavigate();

    const navLinks = [
        { to: "/products",      label: "Shop",          icon: Store },
        { to: "/orders",        label: "My Orders",     icon: ClipboardList },
        { to: "/notifications", label: "Notifications", icon: Bell },
        ...(admin ? [{ to: "/admin", label: "Admin", icon: LayoutDashboard }] : []),
    ];

    return (
        <>
            {/* Clean white header — no heavy shadow, just a hairline border */}
            <header className="sticky top-0 z-40 w-full bg-card border-b border-border/60">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between h-[68px]">

                        {/* Logo */}
                        <Link to="/" className="shrink-0 flex items-center" aria-label="Funkart home">
                            <LogoMark size={30} dark={dark} />
                        </Link>

                        {/* Desktop nav — pill style, no filled backgrounds on active */}
                        <nav className="hidden md:flex items-center gap-0.5">
                            {navLinks.map(({ to, label }) => (
                                <Link
                                    key={to}
                                    to={to}
                                    className={cn(
                                        "px-4 py-2 rounded-full text-sm transition-all duration-150",
                                        pathname.startsWith(to)
                                            ? "font-semibold text-foreground bg-secondary"
                                            : "font-medium text-muted-foreground hover:text-foreground hover:bg-secondary/70"
                                    )}
                                >
                                    {label}
                                </Link>
                            ))}
                        </nav>

                        {/* Right side actions */}
                        <div className="flex items-center gap-1">
                            {/* Greeting */}
                            {currentUser && (
                                <button
                                    onClick={() => navigate("/profile")}
                                    className="hidden lg:flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors px-3 py-1.5 rounded-full hover:bg-secondary/70 mr-1"
                                    title="View profile"
                                >
                                    <UserCircle className="w-4 h-4" />
                                    <span>Hi, <span className="font-semibold text-foreground">{currentUser.name?.split(" ")[0] || currentUser.email}</span></span>
                                </button>
                            )}

                            {/* Theme toggle */}
                            <button
                                onClick={toggleTheme}
                                className="p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-secondary/70 transition-colors"
                                aria-label="Toggle theme"
                            >
                                {dark ? <Sun className="w-[18px] h-[18px]" /> : <Moon className="w-[18px] h-[18px]" />}
                            </button>

                            {/* Cart — bordered pill, count badge inline */}
                            <button
                                onClick={() => setDrawerOpen(true)}
                                className="flex items-center gap-2 px-3 py-1.5 rounded-full border border-border hover:border-foreground/20 hover:shadow-sm transition-all duration-150 ml-1"
                                aria-label="Open cart"
                            >
                                <ShoppingCart className="w-4 h-4 text-foreground" />
                                {itemCount > 0 && (
                                    <span className="min-w-[18px] h-[18px] px-1 rounded-full bg-primary text-primary-foreground text-[10px] font-bold flex items-center justify-center tabular-nums">
                                        {itemCount > 9 ? "9+" : itemCount}
                                    </span>
                                )}
                            </button>

                            {/* Logout */}
                            <button
                                onClick={onLogout}
                                className="p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-secondary/70 transition-colors"
                                aria-label="Logout"
                                title="Logout"
                            >
                                <LogOut className="w-[18px] h-[18px]" />
                            </button>

                            {/* Mobile hamburger */}
                            <button
                                className="md:hidden p-2 rounded-full text-muted-foreground hover:text-foreground hover:bg-secondary/70 transition-colors"
                                onClick={() => setMobileOpen((v) => !v)}
                            >
                                {mobileOpen ? <X className="w-[18px] h-[18px]" /> : <Menu className="w-[18px] h-[18px]" />}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Mobile nav dropdown */}
                {mobileOpen && (
                    <div className="md:hidden border-t border-border/50 bg-card px-4 py-4 space-y-1">
                        {navLinks.map(({ to, label, icon: Icon }) => (
                            <Link
                                key={to}
                                to={to}
                                onClick={() => setMobileOpen(false)}
                                className={cn(
                                    "flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors",
                                    pathname.startsWith(to)
                                        ? "bg-secondary text-foreground font-semibold"
                                        : "text-muted-foreground hover:text-foreground hover:bg-secondary/70"
                                )}
                            >
                                <Icon className="w-4 h-4" />
                                {label}
                            </Link>
                        ))}
                        <Link
                            to="/profile"
                            onClick={() => setMobileOpen(false)}
                            className={cn(
                                "flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors",
                                pathname === "/profile"
                                    ? "bg-secondary text-foreground font-semibold"
                                    : "text-muted-foreground hover:text-foreground hover:bg-secondary/70"
                            )}
                        >
                            <UserCircle className="w-4 h-4" />
                            Profile
                        </Link>
                        <button
                            onClick={() => { setMobileOpen(false); onLogout(); }}
                            className="flex w-full items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-secondary/70 transition-colors"
                        >
                            <LogOut className="w-4 h-4" />
                            Logout
                        </button>
                    </div>
                )}
            </header>

            <CartDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
        </>
    );
}
