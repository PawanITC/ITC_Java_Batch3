import {
    createContext,
    useContext,
    useEffect,
    useState,
    useCallback,
} from "react";
import { queryClientInstance } from "../lib/query-client";
import { CURRENT_USER_KEY } from "../hooks/useCurrentUser";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    const refreshUser = useCallback(async () => {
        try {
            setLoading(true);

            const res = await fetch("/api/v1/users/me", {
                credentials: "include",
            });

            if (!res.ok) {
                setUser(null);
                return;
            }

            const data = await res.json();
            const userData = data.data ?? data;
            setUser(userData);
            // Sync the React Query cache so Header reflects the new user immediately
            queryClientInstance.setQueryData(CURRENT_USER_KEY, userData);
        } catch (err) {
            console.error("refreshUser failed:", err);
            setUser(null);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        refreshUser();
    }, [refreshUser]);

    const login = async (email, password) => {
        const res = await fetch("/api/v1/users/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ email, password }),
        });

        if (!res.ok) throw new Error("Login failed");

        await refreshUser();
    };

    const signup = async (payload) => {
        const res = await fetch("/api/v1/users/signup", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify(payload),
        });

        if (!res.ok) throw new Error("Signup failed");

        await refreshUser();
    };

    const logout = async () => {
        try {
            await fetch("/api/v1/users/logout", {
                method: "POST",
                credentials: "include",
            });
        } catch (err) {
            console.error(err);
        }

        // Clear both state systems so Header and routes both reflect logged-out state
        setUser(null);
        queryClientInstance.removeQueries({ queryKey: CURRENT_USER_KEY });
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                loading,
                isAuthenticated: !!user,
                refreshUser,
                login,
                signup,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be inside AuthProvider");
    return ctx;
}