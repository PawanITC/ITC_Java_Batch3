
import LoginJWT from "./components/LoginJWT.tsx";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Signup from "./components/Signup.tsx";

function App() {

    return (
        <>
            <BrowserRouter>

                <Routes>
                    <Route path="/" element={<LoginJWT />} />
                    <Route path="/signup" element={<Signup />} />
                </Routes>

            </BrowserRouter>



        </>
    )
}

export default App
