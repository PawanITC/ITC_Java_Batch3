import React, {useState} from "react";
import "./Login.css";
import {API_BASE_URL, BACKEND_URL} from "../config/env.ts";

const API = API_BASE_URL;

const Login = () => {

    const [email, setEmail] = useState<string>("");
    const [password, setPassword] = useState<string>("");

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        const response = await fetch(`${API}/users/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email,
                password
            })
        });

        const data = await response.json();

        if (data?.data?.token) {
            localStorage.setItem("jwt", data.data.token);
            alert("Login successful");
        } else {
            alert("Login failed");
        }
    };

    return (
        <div className="login-container">

            <h2>Login</h2>

            <form onSubmit={handleSubmit}>

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

                <button type="submit">Login</button>

            </form>

            <br/>

            <div className="oauth-container">

                <p className="oauth-divider">or</p>

                <a
                    href={`${BACKEND_URL}/oauth/github/login`}
                    className="github-login-btn"
                >
                    Login with GitHub
                </a>

            </div>

        </div>
    );
};

export default Login;