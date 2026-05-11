import { Outlet } from "react-router-dom";
import Header from "./Header";

export default function Layout({ onLogout }) {
    return (
        <div className="min-h-screen bg-background flex flex-col font-inter">
            <Header onLogout={onLogout} />
            <main className="flex-1">
                <Outlet />
            </main>

            {/* Footer — refined, whitespace-forward */}
            <footer className="border-t border-border/60 bg-card mt-16">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
                    <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
                        <span className="font-extrabold text-foreground tracking-tight">Funkart</span>
                        <p className="text-sm text-muted-foreground">
                            © {new Date().getFullYear()} Funkart. All rights reserved.
                        </p>
                        <p className="text-sm text-muted-foreground">
                            Shop smarter. Ship faster.
                        </p>
                    </div>
                </div>
            </footer>
        </div>
    );
}
