import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

const ORDER_STATUSES = [
    "PENDING", "PROCESSING", "CONFIRMED", "PAID",
    "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED", "FAILED",
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