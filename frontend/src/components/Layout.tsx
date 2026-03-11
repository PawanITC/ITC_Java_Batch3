import { Outlet } from "react-router-dom";
import Navbar from "./Navbar/Navbar";

export default function Layout() {
    return (
        <>
            <Navbar />
            <main>
                <Outlet /> {/* Render page content here */}
            </main>
        </>
    );
}