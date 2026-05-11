const STATUS_STYLES = {
    PENDING:    "bg-yellow-50 text-yellow-700 border border-yellow-200",
    CONFIRMED:  "bg-blue-50 text-blue-700 border border-blue-200",
    PROCESSING: "bg-purple-50 text-purple-700 border border-purple-200",
    PAID:       "bg-emerald-50 text-emerald-700 border border-emerald-200",
    SHIPPED:    "bg-sky-50 text-sky-700 border border-sky-200",
    DELIVERED:  "bg-green-50 text-green-700 border border-green-200",
    CANCELLED:  "bg-red-50 text-red-700 border border-red-200",
};

const DEFAULT = "bg-secondary text-muted-foreground border border-border";

export default function OrderStatusBadge({ status }) {
    const cls = STATUS_STYLES[status] ?? DEFAULT;
    return (
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${cls}`}>
      {status ?? "Unknown"}
    </span>
    );
}