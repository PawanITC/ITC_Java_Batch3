import { useAuth } from "../../context/AuthContext";
import "./Navbar.css"
import { Link } from "react-router-dom";
import { useState } from "react";

export default function Navbar() {
    const { user, isAuthenticated, loading, setUser } = useAuth();
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    // Don't render while checking authentication
    if (loading) return null;

    const logout = async () => {
        setIsLoggingOut(true);
        try {
            // Call backend logout endpoint to clear cookie
            const res = await fetch("/oauth/github/logout", {
                method: "GET",
                credentials: "include"
            });

            if (res.ok) {
                console.log("✓ Logged out from server");
            } else {
                console.warn("⚠ Server logout returned status:", res.status);
            }
        } catch (err) {
            console.error("✗ Logout request failed:", err);
            // Continue logout even if request fails
        } finally {
            // Clear frontend context state
            setUser(null);
            setIsLoggingOut(false);
        }
    };

    return (
        <nav className="navbar">
            <div className="navbar-brand">FunkArt</div>
            <div className="navbar-links">
                {isAuthenticated && user ? (
                    <>
                        <span className="nav-user">Welcome {user.name}</span>
                        <button
                            onClick={logout}
                            className="btn-logout"
                            disabled={isLoggingOut}
                        >
                            {isLoggingOut ? "Logging out..." : "Logout"}
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login" className="nav-link">Login</Link>
                        <Link to="/signup" className="nav-link">Signup</Link>
                    </>
                )}
            </div>
        </nav>
    );
}