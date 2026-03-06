import './login.css'

function Login() {
    return (
        <div className="login-container">
            <h1>Login</h1>

            <form>
                <input type="text" placeholder="Email" />
                <input type="password" placeholder="Password" />
                <button>Sign Up</button>
                <button>Login</button>
            </form>
        </div>
    )
}

export default Login