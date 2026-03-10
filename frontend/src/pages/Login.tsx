import { useState, useContext } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";
import "../styles/auth.css";

export default function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();
    const { setUser } = useContext(AuthContext);

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h2>Login</h2>
                <form className="auth-form" onSubmit={async (e) => {
                    e.preventDefault();

                    const res = await fetch(`/api/v1/users/login`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        credentials: "include",
                        body: JSON.stringify({ email, password }),
                    });

                    if (!res.ok) {
                        console.error("Login failed:", res.status);
                        return;
                    }

                    const { data } = await res.json();
                    setUser(data.user || { email });
                    navigate("/");
                }}>
                    <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                    <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                    <button className="auth-button" type="submit">Login</button>
                </form>
                <div className="oauth-divider">or</div>
                <button
                    className="github-button"
                    onClick={() =>
                        (window.location.href = "http://localhost:8080/oauth/github/login")
                    }
                >
                    Login with GitHub
                </button>
            </div>
        </div>
    );
}