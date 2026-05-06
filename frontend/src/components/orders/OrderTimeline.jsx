import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

const STEPS = [
    { status: "PENDING",    label: "Order Placed",    desc: "Order received and awaiting confirmation." },
    { status: "CONFIRMED",  label: "Confirmed",        desc: "Order confirmed by the team." },
    { status: "PROCESSING", label: "Processing",       desc: "Items are being packed." },
    { status: "PAID",       label: "Payment Verified", desc: "Payment has been verified." },
    { status: "SHIPPED",    label: "Shipped",          desc: "Order is on its way." },
    { status: "DELIVERED",  label: "Delivered",        desc: "Order delivered to the customer." },
];

const STATUS_ORDER = STEPS.map((s) => s.status);

export default function OrderTimeline({ status }) {
    const currentIndex = STATUS_ORDER.indexOf(status);

    if (status === "CANCELLED") {
        return (
            <div className="flex items-center gap-3 text-destructive bg-destructive/10 rounded-xl px-5 py-4">
                <p className="font-semibold">Order Cancelled</p>
            </div>
        );
    }

    return (
        <ol className="relative border-l border-border ml-4 space-y-6">
            {STEPS.map((step, i) => {
                const done   = i < currentIndex;
                const active = i === currentIndex;
                return (
                    <li key={step.status} className="ml-6">
            <span className={cn(
                "absolute -left-3 flex h-6 w-6 items-center justify-center rounded-full ring-4 ring-background",
                done   && "bg-primary",
                active && "bg-accent",
                !done && !active && "bg-muted border border-border"
            )}>
              {done
                  ? <Check className="w-3 h-3 text-primary-foreground" />
                  : <span className={cn("w-2 h-2 rounded-full", active ? "bg-foreground" : "bg-muted-foreground/30")} />
              }
            </span>
                        <p className={cn("font-semibold text-sm", !done && !active && "text-muted-foreground")}>{step.label}</p>
                        <p className="text-xs text-muted-foreground">{step.desc}</p>
                    </li>
                );
            })}
        </ol>
    );
}