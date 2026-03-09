import './loginJWT.css'
import { Link } from "react-router-dom";

function LoginJWT() {
    return (
        <div className="login-page">

            <div className="login-card">

                <h1 className="login-title">Sign In</h1>

                <form>

                    <label>Email</label>
                    <input type="email" placeholder="Enter your email" />

                    <label>Password</label>
                    <input type="password" placeholder="Enter your password" />

                    <button className="login-button">Sign In</button>

                </form>

                <p className="login-footer">
                    New customer? <Link to="/Signup"> Create your account</Link>
                </p>

            </div>

        </div>
    )
}

export default LoginJWT