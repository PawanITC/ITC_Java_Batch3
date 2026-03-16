import { useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const OAuthCallback = () => {
    const [searchParams] = useSearchParams();
    const code = searchParams.get("code");
    const { refreshUser } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (!code) return;

        const handleOAuth = async () => {
            try {
                const res = await fetch(`/oauth/github/callback?code=${code}`, {
                    credentials: "include",
                });

                if (!res.ok) throw new Error("OAuth failed");

                await refreshUser(); // update user context
                navigate("/"); // redirect to home
            } catch (err) {
                console.error("OAuth callback error:", err);
                navigate("/login"); // fallback if something fails
            }
        };

        handleOAuth();
    }, [code, refreshUser, navigate]);

    return <p>Logging you in…</p>;
};

export default OAuthCallback;