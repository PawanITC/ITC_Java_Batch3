import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Loader2 } from "lucide-react";

export default function OAuthSuccess() {
    const { refreshUser } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const run = async () => {
            try {
                await refreshUser();
                navigate("/", { replace: true });
            } catch {
                navigate("/login", { replace: true });
            }
        };

        run();
    }, [refreshUser, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center">
            <div className="flex flex-col items-center gap-3">
                <Loader2 className="w-6 h-6 animate-spin" />
                <p>Signing you in…</p>
            </div>
        </div>
    );
}