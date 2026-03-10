import { useAuth } from "../../context/AuthContext";
import "./Navbar.css"
import {Link} from "react-router-dom";

export default function Navbar() {
    const { user, isAuthenticated, loading, setUser } = useAuth();

    if (loading) return null; // optional: prevent flash of logged-out state

    const logout = async () => {
        await fetch("/oauth/github/logout", { method: "GET", credentials: "include" });
        setUser(null); // update context → Navbar re-renders
    };

    return (
        <nav className="navbar">
            <div className="navbar-brand">FunkArt</div>
            <div className="navbar-links">
                {isAuthenticated ? (
                    <>
                        <span className="nav-user">Hello, {user?.name}</span>
                        <button onClick={logout} className="btn-logout">
                            Logout
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