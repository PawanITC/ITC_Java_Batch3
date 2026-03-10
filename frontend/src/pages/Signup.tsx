import React, { useState, useContext } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";
import "../styles/auth.css";

export default function Signup() {
    const [formData, setFormData] = useState({ name: "", email: "", password: "" });
    const [error, setError] = useState("");
    const navigate = useNavigate();
    const { setUser } = useContext(AuthContext);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) =>
        setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h2>Sign Up</h2>
                <form className="auth-form" onSubmit={async (e) => {
                    e.preventDefault();
                    setError("");

                    const res = await fetch(`/api/v1/users/signup`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        credentials: "include",
                        body: JSON.stringify(formData),
                    });

                    if (!res.ok) {
                        const { message } = await res.json();
                        return setError(message || "Signup failed");
                    }

                    const userRes = await fetch(`/api/v1/users/me`, { credentials: "include" });
                    if (!userRes.ok) return setError("Signup succeeded but failed to fetch user");
                    setUser(await userRes.json());
                    navigate("/");
                }}>
                    <input type="text" name="name" placeholder="Name" value={formData.name} onChange={handleChange} required />
                    <input type="email" name="email" placeholder="Email" value={formData.email} onChange={handleChange} required />
                    <input type="password" name="password" placeholder="Password" value={formData.password} onChange={handleChange} required />
                    <button className="auth-button" type="submit">Create Account</button>
                </form>
                {error && <p className="error">{error}</p>}
            </div>
        </div>
    );
}