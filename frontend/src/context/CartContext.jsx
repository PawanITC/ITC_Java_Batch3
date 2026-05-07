import { createContext, useContext, useState, useCallback } from "react";
import { cartApi } from "@/lib/cartApi";

const CartContext = createContext(null);

export function CartProvider({ children }) {
    const [cart, setCart] = useState(null);
    const [loading, setLoading] = useState(false);

    // Backend wraps responses in ApiResponse<CartResponse> — unwrap .data
    const unwrap = (res) => res?.data ?? res;

    const run = async (fn) => {
        try {
            setLoading(true);
            const res = await fn();
            const cartData = unwrap(res);
            setCart(cartData);
            return cartData;
        } finally {
            setLoading(false);
        }
    };

    const addItem = (id, qty) => run(() => cartApi.addItem(id, qty));
    const updateItem = (id, qty) => run(() => cartApi.updateItem(id, qty));
    const removeItem = (id) => run(() => cartApi.removeItem(id));

    const fetchCart = useCallback(async () => {
        setLoading(true);
        const data = await cartApi.getCart();
        setCart(unwrap(data));
        setLoading(false);
    }, []);

    const checkout = useCallback(async () => {
        const res = await cartApi.checkout();
        setCart(null);
        return res;
    }, []);

    return (
        <CartContext.Provider
            value={{
                cart,
                loading,
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
    if (!ctx) throw new Error("useCart must be inside provider");
    return ctx;
};