import { useState, useContext, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";
import "../styles/auth.css";

export default function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState(""); // optional improvement
    const navigate = useNavigate();
    const { setUser } = useContext(AuthContext);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError("");

        try {
            const res = await fetch("/api/v1/users/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({ email, password }),
            });

            const body = await res.json().catch(() => ({}));

            if (!res.ok) {
                setError(body.message || "Login failed");
                return;
            }

            setUser(body.data);
            navigate("/");
        } catch (err) {
            console.error("Login error:", err);
            setError("Something went wrong. Please try again.");
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h2>Login</h2>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <input
                        type="email"
                        placeholder="Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                    <input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                    <button className="auth-button" type="submit">
                        Login
                    </button>
                </form>

                {error && <p className="error">{error}</p>}

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