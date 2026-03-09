import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

const OAuthCallback = () => {

    const navigate = useNavigate();

    useEffect(() => {

        const params = new URLSearchParams(window.location.search);

        const token = params.get("token");

        if (token) {

            localStorage.setItem("authToken", token);

            // redirect to homepage
            navigate("/");

        } else {

            console.error("OAuth login failed");

            navigate("/login");

        }

    }, [navigate]);

    return <p>Logging you in...</p>;
};

export default OAuthCallback;