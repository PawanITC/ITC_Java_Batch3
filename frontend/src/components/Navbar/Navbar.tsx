import { Link } from "react-router-dom";
import "./Navbar.css";

const Navbar = () => {

    const handleLogout = (): void => {
        localStorage.removeItem("jwt");
        alert("Logged out");
    };

    return (
        <nav className="navbar">

            <div className="navbar-logo">
                FunkArt
            </div>

            <ul className="navbar-links">

                <li>
                    <Link to="/">Home</Link>
                </li>

                <li>
                    <Link to="/login">Login</Link>
                </li>

                <li>
                    <Link to="/signup">Signup</Link>
                </li>

                <li>
                    <button onClick={handleLogout} className="logout-btn">
                        Logout
                    </button>
                </li>

            </ul>

        </nav>
    );
};

export default Navbar;