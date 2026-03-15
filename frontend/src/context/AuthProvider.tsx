import { type ReactNode, useCallback, useEffect, useState } from "react";
import { AuthContext, type User } from "./AuthContext";

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    // Unified function to refresh user state from backend
    const refreshUser = useCallback(async () => {
        setLoading(true);
        try {
            const res = await fetch("/api/v1/users/me", {
                credentials: "include",
                headers: { "Content-Type": "application/json" },
            });

            if (res.ok) {
                const response = await res.json();

                // Handle both response shapes: { data: user } or just user
                const userData = response.data ?? response;
                setUser(userData);

                console.debug("✓ User authenticated:", userData);
            } else {
                setUser(null);
                console.debug("ℹ User not authenticated");
            }
        } catch (err) {
            setUser(null);
            console.error("✗ Failed to refresh user:", err);
        } finally {
            setLoading(false);
        }
    }, []);

    // Run once on app load
    useEffect(() => {
        refreshUser();
    }, [refreshUser]);

    return (
        <AuthContext.Provider
            value={{
                user,
                setUser,
                isAuthenticated: !!user,
                loading,
                refreshUser,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};