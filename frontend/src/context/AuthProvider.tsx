import {type ReactNode, useEffect, useState} from "react";
import {AuthContext, type User} from "./AuthContext";

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    const refreshUser = async () => {
        setLoading(true);
        try {
            const res = await fetch("/api/v1/users/me", { credentials: "include" });
            if (res.ok) {
                const data = await res.json();
                setUser(data);
            } else {
                setUser(null);
            }
        } catch {
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        (async () => {
            await refreshUser();
        })();
    }, []);

    return (
        <AuthContext.Provider value={{ user, setUser, isAuthenticated: !!user, loading, refreshUser }}>
            {children}
        </AuthContext.Provider>
    );
};