import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import "./loginSignup.css";

const Signup = () => {

    const [name, setName] = useState<string>("");
    const [email, setEmail] = useState<string>("");
    const [password, setPassword] = useState<string>("");
    const [confirmPassword, setConfirmPassword] = useState<string>("");
    const [error, setError] = useState<string>("");

    const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        // Check passwords match
        if (password !== confirmPassword) {
            setError("Passwords do not match");
            return;
        }

        setError("");

        const newUser = {
            name,
            email,
            password
        };

        console.log("Signup data:", newUser);

        // Later send to backend API
    };

    return (
        <div className="login-page">

            <div className="login-card">

                <h1 className="login-title">Create Account</h1>

                <form onSubmit={handleSubmit}>

                    <label>Full Name</label>
                    <input
                        type="text"
                        placeholder="Enter your name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                    />

                    <label>Email</label>
                    <input
                        type="email"
                        placeholder="Enter your email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />

                    <label>Password</label>
                    <input
                        type="password"
                        placeholder="Create a password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                    />

                    <label>Confirm Password</label>
                    <input
                        type="password"
                        placeholder="Confirm your password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                    />

                    {error && <p className="error-message">{error}</p>}

                    <button type="submit" className="login-button">
                        Create Account
                    </button>

                </form>

                <p className="login-footer">
                    Already have an account? <Link to="/">Sign in</Link>
                </p>

            </div>

        </div>
    );
};

export default Signup;