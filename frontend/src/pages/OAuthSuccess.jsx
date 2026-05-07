import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Loader2 } from "lucide-react";

export default function OAuthSuccess() {
    const { refreshUser } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const finalizeLogin = async () => {
            await refreshUser();
            navigate("/", { replace: true });
        };
        finalizeLogin();
    }, [refreshUser, navigate]);

    return (
        <div className="min-h-screen bg-background flex items-center justify-center">
            <div className="flex flex-col items-center gap-4 text-center">
                <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                <p className="font-semibold text-foreground">Authenticating with GitHub…</p>
                <p className="text-sm text-muted-foreground">Please wait while we set up your session.</p>
            </div>
        </div>
    );
}