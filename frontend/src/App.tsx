
import LoginJWT from "./components/LoginJWT.tsx";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Signup from "./components/Signup.tsx";
import OAuthCallback from "./pages/OAuthCallback";

function App() {

    return (
        <>
            <BrowserRouter>

                <Routes>
                    <Route path="/" element={<LoginJWT />} />
                    <Route path="/signup" element={<Signup />} />
                    <Route path="/oauth/callback" element={<OAuthCallback />} />
                </Routes>

            </BrowserRouter>



        </>
    )
}

export default App
