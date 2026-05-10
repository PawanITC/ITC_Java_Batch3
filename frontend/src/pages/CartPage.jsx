import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ShoppingBag, ArrowRight, Loader2, RefreshCw, X } from "lucide-react";
import { useCart } from "../context/CartContext";
import CartItem from "../components/cart/CartItem";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";

export default function CartPage() {
    const { cart, loading, loadError, operationError, clearOperationError, fetchCart, checkout } = useCart();
    const navigate = useNavigate();

    useEffect(() => { fetchCart(); }, [fetchCart]);

    const items = cart?.items ?? [];
    const total = cart?.totalAmount ?? 0;

    const handleCheckout = async () => {
        if (!items.length) return;
        // Snapshot cart BEFORE checkout() clears it from context
        const cartSnapshot = cart;
        const result = await checkout();
        navigate("/checkout", { state: { checkoutResult: result, cartSnapshot } });
    };

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
            <h1 className="text-3xl font-extrabold mb-2 flex items-center gap-3">
                <ShoppingBag className="w-7 h-7" /> Your Cart
            </h1>
            <p className="text-muted-foreground text-sm mb-8">
                {items.length === 0 ? "No items yet." : `${items.length} item${items.length > 1 ? "s" : ""} in your cart`}
            </p>

            {/* Initial load spinner — only while cart hasn't loaded yet */}
            {loading && !cart && (
                <div className="flex justify-center py-24">
                    <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                </div>
            )}

            {/* Load error — only when initial fetch failed and we have no cart data */}
            {loadError && !cart && (
                <div className="bg-destructive/10 text-destructive rounded-xl px-6 py-4 flex items-center justify-between mb-6">
                    <p>{loadError}</p>
                    <Button variant="outline" size="sm" onClick={fetchCart} className="gap-2">
                        <RefreshCw className="w-4 h-4" /> Retry
                    </Button>
                </div>
            )}

            {/* Operation error (add/update/remove) — dismissible, shown alongside loaded cart */}
            {operationError && (
                <div className="bg-amber-50 border border-amber-200 text-amber-800 rounded-xl px-5 py-3 flex items-center justify-between mb-5 text-sm">
                    <span>{operationError}</span>
                    <button
                        onClick={clearOperationError}
                        className="ml-4 text-amber-600 hover:text-amber-800 transition-colors"
                        aria-label="Dismiss"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>
            )}

            {!loading && !loadError && items.length === 0 && (
                <div className="flex flex-col items-center justify-center py-32 gap-5 text-center">
                    <div className="w-24 h-24 rounded-full bg-muted flex items-center justify-center">
                        <ShoppingBag className="w-10 h-10 text-muted-foreground/50" />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold mb-1">Your cart is empty</h2>
                        <p className="text-muted-foreground text-sm">Browse our collection and find something you love!</p>
                    </div>
                    <Button onClick={() => navigate("/products")} className="gap-2 mt-2">
                        Browse Products <ArrowRight className="w-4 h-4" />
                    </Button>
                </div>
            )}

            {items.length > 0 && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Items list */}
                    <div className="lg:col-span-2">
                        <div className="bg-card rounded-xl border shadow-sm overflow-hidden">
                            <div className="hidden md:grid grid-cols-12 px-5 py-3 bg-secondary/60 text-xs font-semibold text-muted-foreground uppercase tracking-widest border-b">
                                <span className="col-span-5">Product</span>
                                <span className="col-span-3 text-center">Quantity</span>
                                <span className="col-span-2 text-right">Subtotal</span>
                                <span className="col-span-2" />
                            </div>
                            <div className="divide-y">
                                {items.map((item) => (
                                    <CartItem key={item.productId} item={item} />
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Order summary */}
                    <div>
                        <div className="bg-card rounded-xl border shadow-sm p-6 sticky top-24 space-y-4">
                            <h2 className="font-bold text-lg">Order Summary</h2>

                            <div className="space-y-2 text-sm">
                                {items.map((item) => (
                                    <div key={item.productId} className="flex justify-between text-muted-foreground">
                                        <span className="truncate max-w-[180px]">
                                            {item.productName}{" "}
                                            <span className="text-foreground font-medium">×{item.quantity}</span>
                                        </span>
                                        <span className="ml-4 tabular-nums">£{item.subTotal.toFixed(2)}</span>
                                    </div>
                                ))}
                            </div>

                            <Separator />

                            <div className="flex justify-between font-bold text-base">
                                <span>Total</span>
                                <span className="text-lg tabular-nums">£{total.toFixed(2)}</span>
                            </div>

                            <Button
                                className="w-full gap-2 h-12 text-base font-semibold"
                                onClick={handleCheckout}
                                disabled={loading}
                            >
                                {loading
                                    ? <Loader2 className="w-5 h-5 animate-spin" />
                                    : <>Proceed to Checkout <ArrowRight className="w-4 h-4" /></>
                                }
                            </Button>

                            <Button
                                variant="ghost"
                                className="w-full text-muted-foreground"
                                onClick={() => navigate("/products")}
                            >
                                Continue Shopping
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
