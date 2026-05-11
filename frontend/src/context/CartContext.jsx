import { createContext, useContext, useState, useCallback } from "react";
import { cartApi } from "@/lib/cartApi";
import { useToast } from "@/components/ui/use-toast";

const CartContext = createContext(null);

export function CartProvider({ children }) {
    const [cart, setCart] = useState(null);
    const [loading, setLoading] = useState(false);
    // loadError: shown only when the initial cart fetch fails (cart is null)
    const [loadError, setLoadError] = useState(null);
    // operationError: shown inline after add/update/remove fails (cart is still loaded)
    const [operationError, setOperationError] = useState(null);
    const { toast } = useToast();

    // Backend wraps responses in ApiResponse<CartResponse> — unwrap .data
    const unwrap = (res) => res?.data ?? res;

    /** Run a cart mutation, show a toast on error, and keep cart data intact. */
    const run = async (fn, successMsg) => {
        try {
            setLoading(true);
            setOperationError(null);
            const res = await fn();
            const cartData = unwrap(res);
            setCart(cartData);
            if (successMsg) {
                toast({ title: successMsg });
            }
            return cartData;
        } catch (err) {
            const msg =
                err?.response?.data?.message ??
                err?.message ??
                "Cart operation failed";
            setOperationError(msg);
            toast({ title: "Cart error", description: msg, variant: "destructive" });
            throw err;
        } finally {
            setLoading(false);
        }
    };

    const addItem = (id, qty) => run(() => cartApi.addItem(id, qty), "Added to cart");
    const updateItem = (id, qty) => run(() => cartApi.updateItem(id, qty));
    const removeItem = (id) => run(() => cartApi.removeItem(id), "Item removed");

    const fetchCart = useCallback(async () => {
        try {
            setLoading(true);
            setLoadError(null);
            const data = await cartApi.getCart();
            setCart(unwrap(data));
        } catch (err) {
            const msg =
                err?.response?.data?.message ??
                err?.message ??
                "Failed to load cart";
            setLoadError(msg);
        } finally {
            setLoading(false);
        }
    }, []);

    const checkout = useCallback(async () => {
        try {
            const res = await cartApi.checkout();
            // Don't clear cart here — CheckoutPage needs it for the Order Summary.
            // Cart will be re-fetched (and found empty) on PaymentSuccess mount.
            toast({ title: "Checkout started", description: "Redirecting to payment…" });
            return res;
        } catch (err) {
            const msg =
                err?.response?.data?.message ??
                err?.message ??
                "Checkout failed";
            toast({ title: "Checkout failed", description: msg, variant: "destructive" });
            throw err;
        }
    }, [toast]);

    // Total quantity across all items (not unique item count)
    const itemCount = cart?.items?.reduce((sum, item) => sum + item.quantity, 0) ?? 0;

    return (
        <CartContext.Provider
            value={{
                cart,
                loading,
                itemCount,
                // keep backward-compat "error" pointing at load error
                error: loadError,
                loadError,
                operationError,
                clearOperationError: () => setOperationError(null),
                addItem,
                updateItem,
                removeItem,
                fetchCart,
                checkout,
            }}
        >
            {children}
        </CartContext.Provider>
    );
}

export const useCart = () => {
    const ctx = useContext(CartContext);
    if (!ctx) throw new Error("useCart must be inside CartProvider");
    return ctx;
};
