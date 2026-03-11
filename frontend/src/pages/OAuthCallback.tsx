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

        fetch(`/oauth/github/callback?code=${code}`, {
            credentials: "include", // ensures cookie is saved
        })
            .then(() => refreshUser()) // refresh context to see logged-in user
            .then(() => navigate("/")) // redirect to home
            .catch(() => navigate("/login")); // fallback if something fails
    }, [code, refreshUser, navigate]);

    return <p>Logging you in…</p>;
};

export default OAuthCallback;