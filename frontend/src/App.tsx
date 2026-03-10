import {BrowserRouter, Routes, Route} from "react-router-dom";
import Navbar from "./components/Navbar/Navbar";
import Login from "./pages/Login";
import Signup from "./pages/Signup";

const Home = () => {
    return <h2 style={{padding: "20px"}}>Welcome to FunkArt</h2>;
};

function App() {

    return (
        <BrowserRouter>

            <Navbar/>

            <Routes>

                <Route path="/" element={<Home/>}/>

                <Route path="/login" element={<Login/>}/>

                <Route path="/signup" element={<Signup/>}/>

            </Routes>

        </BrowserRouter>
    );

}

export default App;