import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useToast } from "@/components/ui/use-toast";

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
    const { toast } = useToast();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const submit = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            await login(email, password);
            toast({ title: "Welcome back!", description: "You are now logged in." });
            navigate("/");
        } catch {
            setError("Invalid credentials. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={submit} className="space-y-3 w-80">
            <h2 className="text-xl font-bold">Login</h2>

            <Input
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Email"
                type="email"
                required
            />
            <Input
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password"
                type="password"
                required
            />

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <Button className="w-full" disabled={loading}>
                {loading ? "Logging in…" : "Login"}
            </Button>

            <button type="button" onClick={onSwitch} className="text-sm underline">
                Create account
            </button>
        </form>
    );
}

function SignupForm({ onSwitch }) {
    const { signup } = useAuth();
    const navigate = useNavigate();
    const { toast } = useToast();

    const [form, setForm] = useState({
        name: "",
        email: "",
        password: "",
    });
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const submit = async (e) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            await signup(form);
            toast({ title: "Account created!", description: "Welcome to Funkart." });
            navigate("/");
        } catch (err) {
            const msg =
                err?.response?.data?.message ??
                err?.response?.data?.error ??
                err?.message ??
                "Registration failed. Please try again.";
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <form onSubmit={submit} className="space-y-3 w-80">
            <h2 className="text-xl font-bold">Sign Up</h2>

            <Input
                placeholder="Name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
            />
            <Input
                placeholder="Email"
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
            />
            <Input
                placeholder="Password"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
            />

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <Button className="w-full" disabled={loading}>
                {loading ? "Creating…" : "Create Account"}
            </Button>

            <button type="button" onClick={onSwitch} className="text-sm underline">
                Back to login
            </button>
        </form>
    );
}
