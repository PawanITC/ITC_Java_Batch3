import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { X, Plus, Minus, Trash2, ShoppingBag, ArrowRight } from "lucide-react";
import { useCart } from "../../context/CartContext.jsx";
import { Button } from "@/components/ui/button";

export default function CartDrawer({ open, onClose }) {
    const { cart, loading, itemCount, updateItem, removeItem } = useCart();
    const navigate = useNavigate();

    useEffect(() => {
        const handler = (e) => { if (e.key === "Escape") onClose(); };
        document.addEventListener("keydown", handler);
        return () => document.removeEventListener("keydown", handler);
    }, [onClose]);

    useEffect(() => {
        document.body.style.overflow = open ? "hidden" : "";
        return () => { document.body.style.overflow = ""; };
    }, [open]);

    const items = cart?.items ?? [];
    const total = cart?.totalAmount ?? 0;

    return (
        <>
            {open && (
                <div
                    className="fixed inset-0 bg-foreground/40 backdrop-blur-sm z-40 transition-opacity"
                    onClick={onClose}
                />
            )}

            <div
                className={`fixed top-0 right-0 h-full w-full sm:w-[420px] bg-white shadow-2xl z-50 flex flex-col
          transition-transform duration-300 ease-in-out
          ${open ? "translate-x-0" : "translate-x-full"}`}
            >
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-5 border-b">
                    <div className="flex items-center gap-2.5">
                        <ShoppingBag className="w-5 h-5" />
                        <h2 className="font-bold text-lg">Cart</h2>
                        {itemCount > 0 && (
                            <span className="bg-accent text-accent-foreground text-xs font-bold rounded-full px-2 py-0.5">
                                {itemCount}
                            </span>
                        )}
                    </div>
                    <button
                        onClick={onClose}
                        className="p-1.5 rounded-lg hover:bg-secondary transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Items */}
                <div className="flex-1 overflow-y-auto px-6 py-4 space-y-5">
                    {loading && (
                        <div className="flex justify-center py-16">
                            <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                        </div>
                    )}

                    {!loading && items.length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20 text-center gap-4">
                            <div className="w-16 h-16 rounded-full bg-secondary flex items-center justify-center">
                                <ShoppingBag className="w-7 h-7 text-muted-foreground" />
                            </div>
                            <div>
                                <p className="font-semibold mb-1">Your cart is empty</p>
                                <p className="text-muted-foreground text-sm">Add some products to get started</p>
                            </div>
                            <Button size="sm" onClick={() => { onClose(); navigate("/products"); }} className="gap-2">
                                Browse Products <ArrowRight className="w-4 h-4" />
                            </Button>
                        </div>
                    )}

                    {items.map((item) => (
                        <div key={item.productId} className="flex gap-3 items-start py-1">
                            {/* Thumbnail — click to navigate to product */}
                            <button
                                onClick={() => { onClose(); navigate(`/products/${item.productId}/reviews`); }}
                                className="w-16 h-16 rounded-lg overflow-hidden shrink-0 border border-border hover:opacity-80 transition-opacity focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                                title={item.productName}
                            >
                                {item.imageUrl ? (
                                    <img
                                        src={item.imageUrl}
                                        alt={item.productName}
                                        className="w-full h-full object-cover"
                                    />
                                ) : (
                                    <div className="w-full h-full bg-secondary flex items-center justify-center text-2xl">
                                        🎨
                                    </div>
                                )}
                            </button>

                            <div className="flex-1 min-w-0">
                                <button
                                    onClick={() => { onClose(); navigate(`/products/${item.productId}/reviews`); }}
                                    className="font-semibold text-sm text-left line-clamp-2 leading-snug hover:text-primary transition-colors"
                                >
                                    {item.productName}
                                </button>
                                <p className="text-muted-foreground text-xs mt-0.5">£{Number(item.price).toFixed(2)} each</p>
                                <div className="flex items-center gap-2 mt-2">
                                    <button
                                        onClick={() => item.quantity > 1 ? updateItem(item.productId, -1) : removeItem(item.productId)}
                                        className="w-6 h-6 rounded-md border border-border flex items-center justify-center hover:bg-secondary transition-colors"
                                        disabled={loading}
                                    >
                                        <Minus className="w-3 h-3" />
                                    </button>
                                    <span className="text-sm w-5 text-center font-medium">{item.quantity}</span>
                                    <button
                                        onClick={() => updateItem(item.productId, 1)}
                                        className="w-6 h-6 rounded-md border border-border flex items-center justify-center hover:bg-secondary transition-colors"
                                        disabled={loading}
                                    >
                                        <Plus className="w-3 h-3" />
                                    </button>
                                </div>
                            </div>

                            <div className="flex flex-col items-end gap-2 shrink-0">
                                <p className="font-bold text-sm">£{Number(item.subTotal).toFixed(2)}</p>
                                <button
                                    onClick={() => removeItem(item.productId)}
                                    className="text-muted-foreground hover:text-destructive transition-colors"
                                    disabled={loading}
                                >
                                    <Trash2 className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Footer */}
                {items.length > 0 && (
                    <div className="border-t px-6 py-5 space-y-4 bg-secondary/30">
                        <div className="flex justify-between items-center">
                            <span className="text-muted-foreground text-sm">Subtotal</span>
                            <span className="font-extrabold text-xl">£{total.toFixed(2)}</span>
                        </div>
                        <div className="flex gap-2">
                            <Button variant="outline" className="flex-1 bg-white" onClick={() => { onClose(); navigate("/cart"); }}>
                                View Cart
                            </Button>
                            <Button className="flex-1 gap-1" onClick={() => { onClose(); navigate("/cart"); }}>
                                Checkout <ArrowRight className="w-4 h-4" />
                            </Button>
                        </div>
                    </div>
                )}
            </div>
        </>
    );
}