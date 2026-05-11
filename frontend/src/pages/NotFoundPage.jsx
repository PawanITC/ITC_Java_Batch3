import { useNavigate } from "react-router-dom";
import { PackageSearch, ArrowLeft, Home } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function NotFoundPage() {
    const navigate = useNavigate();

    return (
        <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 text-center gap-6">
            <div className="w-24 h-24 rounded-full bg-secondary flex items-center justify-center">
                <PackageSearch className="w-12 h-12 text-muted-foreground/60" />
            </div>

            <div>
                <h1 className="text-6xl font-extrabold text-foreground mb-2">404</h1>
                <p className="text-xl font-semibold mb-1">Page not found</p>
                <p className="text-muted-foreground text-sm max-w-sm">
                    The page you're looking for doesn't exist or has been moved.
                </p>
            </div>

            <div className="flex gap-3">
                <Button variant="outline" onClick={() => navigate(-1)} className="gap-2">
                    <ArrowLeft className="w-4 h-4" /> Go Back
                </Button>
                <Button onClick={() => navigate("/products")} className="gap-2">
                    <Home className="w-4 h-4" /> Back to Shop
                </Button>
            </div>
        </div>
    );
}
