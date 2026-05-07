import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

// Valid backend OrderStatus enum values (PROCESSING does not exist in the enum)
// Terminal statuses (DELIVERED, CONFIRMED, CANCELLED, FAILED, REFUNDED) cannot be
// transitioned away from, but can still be set as targets when the order is not yet terminal.
const ORDER_STATUSES = [
    "PENDING", "PAID", "SHIPPED", "DELIVERED",
    "CONFIRMED", "CANCELLED", "REFUNDED", "FAILED",
];

export default function StatusSelect({ value, onChange, disabled }) {
    return (
        <Select value={value} onValueChange={onChange} disabled={disabled}>
            <SelectTrigger className="w-36 h-8 text-xs">
                <SelectValue placeholder="Set status" />
            </SelectTrigger>
            <SelectContent>
                {ORDER_STATUSES.map((s) => (
                    <SelectItem key={s} value={s} className="text-xs">{s}</SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}