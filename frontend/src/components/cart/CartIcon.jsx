import { ShoppingCart } from "lucide-react";
import { useCart } from "@/context/CartContext.jsx";

export default function CartIcon({ onClick }) {
    const { itemCount } = useCart();

    return (
        <button
            onClick={onClick}
            className="relative p-2 rounded-md hover:bg-muted transition-colors"
            aria-label="Open cart"
        >
            <ShoppingCart className="w-5 h-5" />
            {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-primary text-primary-foreground text-xs
          rounded-full w-5 h-5 flex items-center justify-center font-medium leading-none">
          {itemCount > 99 ? "99+" : itemCount}
        </span>
            )}
        </button>
    );
}