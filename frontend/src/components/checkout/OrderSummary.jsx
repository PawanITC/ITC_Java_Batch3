import { Separator } from "@/components/ui/separator";

export default function OrderSummary({ cart }) {
    if (!cart) return null;

    const items = cart.items ?? [];
    const total = cart.totalAmount ?? 0;

    return (
        <div className="bg-muted/40 rounded-xl p-6 space-y-4">
            <h2 className="font-semibold text-lg">Order Summary</h2>
            <div className="space-y-3">
                {items.map((item) => (
                    <div key={item.productId} className="flex justify-between text-sm">
            <span className="text-muted-foreground">
              {item.productName}{" "}
                <span className="text-foreground font-medium">× {item.quantity}</span>
            </span>
                        <span className="font-medium">£{Number(item.subTotal ?? item.subtotal ?? 0).toFixed(2)}</span>
                    </div>
                ))}
            </div>
            <Separator />
            <div className="flex justify-between font-semibold text-base">
                <span>Total</span>
                <span>£{total.toFixed(2)}</span>
            </div>
        </div>
    );
}