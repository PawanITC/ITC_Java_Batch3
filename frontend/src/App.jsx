import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClientInstance } from "@/lib/query-client";
import { Toaster } from "@/components/ui/toaster";

import { AuthProvider, useAuth } from "@/context/AuthContext";
import { CartProvider } from "@/context/CartContext";

import Layout from "@/components/layout/Layout";
import AuthPage from "@/pages/AuthPage";
import OAuthSuccess from "@/pages/OAuthSuccess";

import ProductsPage from "@/pages/ProductsPage";
import CartPage from "@/pages/CartPage";
import CheckoutPage from "@/pages/CheckoutPage";
import PaymentSuccess from "@/pages/PaymentSuccess";
import PaymentFailure from "@/pages/PaymentFailure";
import OrderHistory from "@/pages/OrderHistory";
import OrderDetail from "@/pages/OrderDetail";
import NotificationsPage from "@/pages/NotificationsPage";
import ProductReviews from "@/pages/ProductReviews";
import ProfilePage from "@/pages/ProfilePage";

import NotFoundPage from "@/pages/NotFoundPage";
import AdminDashboard from "@/pages/admin/AdminDashboard";
import AdminUsersPage from "@/pages/admin/AdminUsersPage";
import AdminOrders from "@/pages/admin/AdminOrders";
import AdminOrderTracking from "@/pages/admin/AdminOrderTracking";
import AdminProducts from "@/pages/admin/AdminProducts";

function AppRoutes() {
    const { loading, isAuthenticated, user, logout } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                Loading...
            </div>
        );
    }

    // OAuth callback — must be reachable before auth resolves
    // (handled below via top-level route)

    if (!isAuthenticated) {
        return (
            <Routes>
                <Route path="/oauth-success" element={<OAuthSuccess />} />
                <Route path="*" element={<AuthPage />} />
            </Routes>
        );
    }

    const isAdmin = Array.isArray(user?.roles)
        ? user.roles.includes("ROLE_ADMIN")
        : user?.role === "ROLE_ADMIN";

    return (
        <CartProvider>
            <Routes>
                {/* OAuth landing (in case cookie arrives before refresh completes) */}
                <Route path="/oauth-success" element={<OAuthSuccess />} />

                {/* Main layout routes */}
                <Route element={<Layout onLogout={logout} />}>
                    <Route path="/" element={<Navigate to="/products" replace />} />
                    <Route path="/products" element={<ProductsPage />} />
                    <Route path="/products/:id/reviews" element={<ProductReviews />} />
                    <Route path="/cart" element={<CartPage />} />
                    <Route path="/checkout" element={<CheckoutPage />} />
                    <Route path="/payment-success" element={<PaymentSuccess />} />
                    <Route path="/payment-failure" element={<PaymentFailure />} />
                    <Route path="/orders" element={<OrderHistory />} />
                    <Route path="/orders/history" element={<OrderHistory />} />
                    <Route path="/orders/:id" element={<OrderDetail />} />
                    <Route path="/notifications" element={<NotificationsPage />} />
                    <Route path="/profile" element={<ProfilePage />} />

                    {/* Admin — only accessible with ROLE_ADMIN */}
                    {isAdmin && (
                        <>
                            <Route path="/admin" element={<AdminDashboard />} />
                            <Route path="/admin/users" element={<AdminUsersPage />} />
                            <Route path="/admin/products" element={<AdminProducts />} />
                            {/* /admin/orders = order management table (CRUD) */}
                            <Route path="/admin/orders" element={<AdminOrders />} />
                            {/* /admin/tracking = order lookup by ID with timeline */}
                            <Route path="/admin/tracking" element={<AdminOrderTracking />} />
                        </>
                    )}
                </Route>

                <Route path="*" element={<NotFoundPage />} />
            </Routes>
        </CartProvider>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <QueryClientProvider client={queryClientInstance}>
                <Router>
                    <AppRoutes />
                </Router>
                <Toaster />
            </QueryClientProvider>
        </AuthProvider>
    );
}
