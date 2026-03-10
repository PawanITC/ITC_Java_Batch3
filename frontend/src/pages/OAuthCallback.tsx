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

        // Hit backend to set JWT cookie (no need to parse JSON)
        fetch(`/oauth/github/callback?code=${code}`, {
            credentials: "include", // important to store cookie
        })
            .then(() => refreshUser()) // refresh AuthContext from /api/v1/users/me
            .then(() => navigate("/")) // redirect to home
            .catch(() => navigate("/login")); // fallback
    }, [code, refreshUser, navigate]);

    return <p>Logging you in…</p>;
};

export default OAuthCallback;