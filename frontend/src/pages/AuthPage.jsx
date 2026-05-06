import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function AuthPage() {
    const [mode, setMode] = useState("login");
    return (
        <div className="min-h-screen flex items-center justify-center">
            {mode === "login" ? (
                <LoginForm onSwitch={() => setMode("signup")} />
            ) : (
                <SignupForm onSwitch={() => setMode("login")} />
            )}
        </div>
    );
}

function LoginForm({ onSwitch }) {
    const { login } = useAuth();
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");

    const submit = async (e) => {
        e.preventDefault();
        setError("");

        try {
            await login(email, password);
            navigate("/");
        } catch {
            setError("Invalid credentials");
        }
    };

    return (
        <form onSubmit={submit} className="space-y-3 w-80">
            <h2 className="text-xl font-bold">Login</h2>

            <Input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email" />
            <Input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" type="password" />

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <Button className="w-full">Login</Button>

            <button type="button" onClick={onSwitch} className="text-sm underline">
                Create account
            </button>
        </form>
    );
}

function SignupForm({ onSwitch }) {
    const { signup } = useAuth();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        name: "",
        email: "",
        password: "",
    });

    const submit = async (e) => {
        e.preventDefault();

        await signup(form);
        navigate("/");
    };

    return (
        <form onSubmit={submit} className="space-y-3 w-80">
            <h2 className="text-xl font-bold">Sign Up</h2>

            <Input
                placeholder="Name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
            <Input
                placeholder="Email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
            />
            <Input
                placeholder="Password"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
            />

            <Button className="w-full">Create Account</Button>

            <button type="button" onClick={onSwitch} className="text-sm underline">
                Back to login
            </button>
        </form>
    );
}