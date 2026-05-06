import { Plus, Minus, Trash2 } from "lucide-react";
import { useCart } from "../../context/CartContext.jsx";

export default function CartItem({ item }) {
    const { updateItem, removeItem, loading } = useCart();

    return (
        <div className="flex items-center gap-4 py-4 border-b last:border-0">
            {/* Product info */}
            <div className="flex-1 min-w-0">
                <h3 className="font-medium text-sm md:text-base truncate">{item.productName}</h3>
                <p className="text-muted-foreground text-sm mt-0.5">${item.price.toFixed(2)} each</p>
            </div>

            {/* Quantity controls */}
            <div className="flex items-center gap-2 shrink-0">
                <button
                    onClick={() =>
                        item.quantity > 1
                            ? updateItem(item.productId, item.quantity - 1)
                            : removeItem(item.productId)
                    }
                    disabled={loading}
                    className="w-8 h-8 rounded-md border flex items-center justify-center
            hover:bg-muted transition-colors disabled:opacity-50"
                    aria-label="Decrease quantity"
                >
                    <Minus className="w-3.5 h-3.5" />
                </button>
                <span className="w-8 text-center font-medium text-sm">{item.quantity}</span>
                <button
                    onClick={() => updateItem(item.productId, item.quantity + 1)}
                    disabled={loading}
                    className="w-8 h-8 rounded-md border flex items-center justify-center
            hover:bg-muted transition-colors disabled:opacity-50"
                    aria-label="Increase quantity"
                >
                    <Plus className="w-3.5 h-3.5" />
                </button>
            </div>

            {/* Subtotal */}
            <div className="w-20 text-right shrink-0">
                <p className="font-semibold">${item.subTotal.toFixed(2)}</p>
            </div>

            {/* Remove */}
            <button
                onClick={() => removeItem(item.productId)}
                disabled={loading}
                className="text-destructive hover:text-destructive/70 transition-colors disabled:opacity-50 shrink-0"
                aria-label="Remove item"
            >
                <Trash2 className="w-4 h-4" />
            </button>
        </div>
    );
}