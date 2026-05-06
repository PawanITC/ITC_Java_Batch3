import { useLocation, useNavigate } from "react-router-dom";
import { CheckCircle2, ArrowRight, Package } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";

export default function PaymentSuccess() {
    const { state } = useLocation();
    const navigate = useNavigate();
    const orderData = state?.orderData;
    const cart = state?.cart;
    const items = cart?.items ?? orderData?.items ?? [];
    const total = cart?.totalAmount ?? orderData?.totalAmount ?? 0;

    return (
        <div className="max-w-lg mx-auto px-4 py-16 text-center">
            <div className="w-20 h-20 rounded-full bg-green-50 border-4 border-green-100 flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
            </div>

            <h1 className="text-2xl font-extrabold mb-2">Payment Successful!</h1>
            <p className="text-muted-foreground mb-8">
                Thank you for your order. We're preparing it now and will notify you when it ships.
            </p>

            {(items.length > 0 || orderData) && (
                <div className="bg-white border rounded-xl p-6 text-left mb-6">
                    <div className="flex items-center gap-2 mb-4">
                        <Package className="w-4 h-4 text-muted-foreground" />
                        <h2 className="font-semibold text-sm">Order Summary</h2>
                        {orderData?.id && (
                            <span className="ml-auto text-xs text-muted-foreground font-mono">#{orderData.id}</span>
                        )}
                    </div>

                    {items.length > 0 && (
                        <div className="space-y-2 text-sm">
                            {items.map((item, i) => (
                                <div key={i} className="flex justify-between text-muted-foreground">
                                    <span>{item.productName ?? `Product #${item.productId}`} × {item.quantity}</span>
                                    <span className="font-medium text-foreground">${Number(item.subTotal ?? 0).toFixed(2)}</span>
                                </div>
                            ))}
                            <Separator className="my-3" />
                            <div className="flex justify-between font-bold text-base">
                                <span>Total Paid</span>
                                <span>${Number(total).toFixed(2)}</span>
                            </div>
                        </div>
                    )}
                </div>
            )}

            <div className="flex flex-col gap-3">
                <Button onClick={() => navigate("/orders")} className="gap-2">
                    View My Orders <ArrowRight className="w-4 h-4" />
                </Button>
                <Button variant="outline" onClick={() => navigate("/products")}>
                    Continue Shopping
                </Button>
            </div>
        </div>
    );
}