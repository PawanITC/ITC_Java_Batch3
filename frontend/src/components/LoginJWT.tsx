import './loginJWT.css'
import { Link } from "react-router-dom";
//onClick={handleGithubLogin}
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

                <div className="divider">OR</div>

                <button  className="github-button">
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

export default LoginJWT