import { Outlet } from "react-router-dom";
import Header from "./Header";

export default function Layout({ onLogout }) {
    return (
        <div className="min-h-screen bg-background flex flex-col font-inter">
            <Header onLogout={onLogout} />
            <main className="flex-1">
                <Outlet />
            </main>
            <footer className="border-t bg-white py-8">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-sm text-muted-foreground">
                    <span className="font-semibold text-foreground">FunkArt</span>
                    <span>© {new Date().getFullYear()} FunkArt. All rights reserved.</span>
                    <span>Premium Art Prints &amp; Collectibles</span>
                </div>
            </footer>
        </div>
    );
}