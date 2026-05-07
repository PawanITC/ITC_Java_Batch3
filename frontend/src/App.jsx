import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClientInstance } from "@/lib/query-client";
import { Toaster } from "@/components/ui/toaster";

import { AuthProvider, useAuth } from "@/context/AuthContext";
import { CartProvider } from "@/context/CartContext";

import Layout from "@/components/layout/Layout";
import AuthPage from "@/pages/AuthPage";

import ProductsPage from "@/pages/ProductsPage";
import CartPage from "@/pages/CartPage";
import CheckoutPage from "@/pages/CheckoutPage";

function AppRoutes() {
    const { loading, isAuthenticated, logout } = useAuth();

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                Loading...
            </div>
        );
    }

    if (!isAuthenticated) {
        return <AuthPage />;
    }

    return (
        <CartProvider>
            <Routes>
                <Route element={<Layout onLogout={logout} />}>
                    <Route path="/" element={<Navigate to="/products" replace />} />
                    <Route path="/products" element={<ProductsPage />} />
                    <Route path="/cart" element={<CartPage />} />
                    <Route path="/checkout" element={<CheckoutPage />} />
                </Route>

                <Route path="*" element={<Navigate to="/products" />} />
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