import './loginSignup.css'
import { Link } from "react-router-dom";
import { useState } from "react";



function Login() {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    function handleSubmit(e: { preventDefault: () => void; }) {
        e.preventDefault();
        console.log("Logging in with:", email, password);
        // here you'd normally call your backend login API
        window.alert("Logged in successfully!");
    }

    function handleGithubLogin() {
        // Redirect to backend route that starts GitHub OAuth
        window.location.href = "http://localhost:5000/auth/github";
        // replace with your backend GitHub OAuth endpoint
    }

    return (
        <div className="login-page">

            <div className="login-card">

                <h1 className="login-title">Sign In</h1>

                <form onSubmit={handleSubmit}>

                    <label>Email</label>
                    <input type="email" placeholder="Enter your email" value={email}
                           onChange={(e) => setEmail(e.target.value)}/>

                    <label>Password</label>
                    <input type="password" placeholder="Enter your password"  value={password}
                           onChange={(e) => setPassword(e.target.value)}/>

                    <button className="login-button">Sign In</button>

                </form>

                <div className="divider">OR</div>

                <button  className="github-button" onClick={handleGithubLogin}>
                    <img
                        src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
                        alt="GitHub Logo"
                        className="github-logo"
                    />
                    Sign in with GitHub
                </button>

                <p className="login-footer">
                    New customer? <Link to="/Signup"> Create your account</Link>
                </p>

            </div>

        </div>
    )
}

export default Login