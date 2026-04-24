import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const OAuthSuccess = () => {
    const { refreshUser } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const finalizeLogin = async () => {
            // The cookie is already present because the Gateway set it
            // before redirecting us here. Just fetch the user profile.
            await refreshUser();
            navigate("/", { replace: true });
        };

        finalizeLogin();
    }, [refreshUser, navigate]);

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h2>Authenticating with GitHub...</h2>
                <p>Please wait while we set up your session.</p>
            </div>
        </div>
    );
};

export default OAuthSuccess;