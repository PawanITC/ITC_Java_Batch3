import { createContext, useContext } from "react";

export interface User {
    id: number;
    name: string;
    email: string;
}

export interface AuthContextType {
    user: User | null;
    setUser: (user: User | null) => void;
    isAuthenticated: boolean;
    loading: boolean;
    refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType>({
    user: null,
    setUser: () => {},
    isAuthenticated: false,
    loading: true,
    refreshUser: async () => {},
});

export const useAuth = () => useContext(AuthContext);