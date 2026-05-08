import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Star, ThumbsUp, MessageSquare, Trash2, ShieldAlert } from "lucide-react";
import { productApi } from "../lib/productApi";
import { useAuth } from "@/context/AuthContext";
import { Button } from "@/components/ui/button";
import { useToast } from "@/components/ui/use-toast";
import { cn } from "@/lib/utils";

// ---------------------------------------------------------------------------
// Dummy fallback data shown when the review service is unavailable
// ---------------------------------------------------------------------------
const DUMMY_REVIEWS = [
    { id: 1, userId: 999, author: "Sofia M.", rating: 5, date: "April 22, 2026", title: "Absolutely stunning!", comment: "The colors are so vibrant and it looks even better in person. Shipped fast, packaging was excellent. Will definitely order again!", likes: 24 },
    { id: 2, userId: 998, author: "James T.", rating: 4, date: "April 10, 2026", title: "Great quality print", comment: "Really happy with this purchase. The detail is crisp and the paper feels premium. Knocked off one star only because delivery took a little longer than expected.", likes: 11 },
    { id: 3, userId: 997, author: "Priya K.", rating: 5, date: "March 30, 2026", title: "Perfect gift!", comment: "Bought this as a birthday gift and the recipient absolutely loved it. The frame-ready sizing is a huge bonus. Highly recommend FunkArt!", likes: 18 },
    { id: 4, userId: 996, author: "Daniel R.", rating: 3, date: "March 14, 2026", title: "Good but not perfect", comment: "Nice product overall. The colours are slightly darker than on the website, but still looks good on the wall. Customer service was very responsive.", likes: 6 },
    { id: 5, userId: 995, author: "Amelia W.", rating: 5, date: "February 28, 2026", title: "Exceeded expectations", comment: "I ordered this on a whim and I'm so glad I did. Museum-quality print, very true to the photo online. My living room looks incredible now.", likes: 31 },
];

// ---------------------------------------------------------------------------
// Role helpers
// ---------------------------------------------------------------------------
function hasRole(user, role) {
    if (!user) return false;
    if (Array.isArray(user.roles)) return user.roles.includes(role);
    return user.role === role;
}

function canModerate(user) {
    return hasRole(user, "ROLE_ADMIN") || hasRole(user, "ROLE_MODERATOR");
}

// ---------------------------------------------------------------------------
// Shared UI atoms
// ---------------------------------------------------------------------------
function StarRow({ rating, max = 5, size = "sm" }) {
    return (
        <div className="flex items-center gap-0.5">
            {Array.from({ length: max }).map((_, i) => (
                <Star
                    key={i}
                    className={cn(
                        size === "sm" ? "w-3.5 h-3.5" : "w-5 h-5",
                        i < rating ? "fill-accent text-accent" : "text-border fill-border"
                    )}
                />
            ))}
        </div>
    );
}

function RatingBar({ count, total, star }) {
    const pct = total ? Math.round((count / total) * 100) : 0;
    return (
        <div className="flex items-center gap-3 text-sm">
            <span className="w-10 text-right text-muted-foreground">{star}★</span>
            <div className="flex-1 h-2 bg-secondary rounded-full overflow-hidden">
                <div className="h-full bg-accent rounded-full transition-all" style={{ width: `${pct}%` }} />
            </div>
            <span className="w-8 text-muted-foreground text-xs">{pct}%</span>
        </div>
    );
}

// ---------------------------------------------------------------------------
// ReviewCard — shows moderator delete button for ROLE_ADMIN / ROLE_MODERATOR
// ---------------------------------------------------------------------------
function ReviewCard({ review, isModerator, onModeratorDelete }) {
    const [liked, setLiked] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

    const displayDate = review.createdAt
        ? new Date(review.createdAt).toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" })
        : review.date ?? "";

    const authorInitial = (review.author ?? review.userId?.toString() ?? "?")[0].toUpperCase();
    const authorName = review.author ?? `User #${review.userId}`;

    return (
        <div className="bg-white border border-border rounded-xl p-5 space-y-3">
            {/* Header row */}
            <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-sm font-bold text-primary-foreground shrink-0">
                        {authorInitial}
                    </div>
                    <div>
                        <p className="font-semibold text-sm">{authorName}</p>
                        <p className="text-xs text-muted-foreground">{displayDate}</p>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <StarRow rating={review.rating} />
                    {isModerator && (
                        <button
                            onClick={() => setShowDeleteConfirm((v) => !v)}
                            className="p-1 rounded text-muted-foreground hover:text-destructive transition-colors"
                            title="Moderator: delete review"
                        >
                            <Trash2 className="w-4 h-4" />
                        </button>
                    )}
                </div>
            </div>

            {/* Body */}
            <div>
                {review.title && <p className="font-semibold text-sm mb-1">{review.title}</p>}
                <p className="text-sm text-muted-foreground leading-relaxed">{review.comment ?? review.body}</p>
            </div>

            {/* Helpful button */}
            <button
                onClick={() => setLiked((v) => !v)}
                className={cn(
                    "flex items-center gap-1.5 text-xs rounded-lg px-3 py-1.5 border transition-colors",
                    liked
                        ? "border-accent bg-accent/10 text-accent-foreground font-semibold"
                        : "border-border text-muted-foreground hover:border-foreground hover:text-foreground"
                )}
            >
                <ThumbsUp className="w-3.5 h-3.5" />
                Helpful ({(review.likes ?? 0) + (liked ? 1 : 0)})
            </button>

            {/* Moderator delete confirmation inline */}
            {isModerator && showDeleteConfirm && (
                <div className="border border-destructive/40 bg-destructive/5 rounded-lg p-3 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2 text-sm text-destructive">
                        <ShieldAlert className="w-4 h-4 shrink-0" />
                        <span>Remove this review as moderator?</span>
                    </div>
                    <div className="flex gap-2 shrink-0">
                        <button
                            onClick={() => setShowDeleteConfirm(false)}
                            className="text-xs px-2 py-1 rounded border border-border hover:bg-secondary transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={() => {
                                setShowDeleteConfirm(false);
                                onModeratorDelete(review.id);
                            }}
                            className="text-xs px-2 py-1 rounded bg-destructive text-white hover:bg-destructive/90 transition-colors"
                        >
                            Delete
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------
async function fetchReviews(productId) {
    const res = await fetch(`/api/v1/reviews/${productId}?page=0&size=50`, {
        credentials: "include",
    });
    if (!res.ok) throw new Error("review-service unavailable");
    const json = await res.json();
    // Spring Page<ReviewResponse> returns { content: [...], ... }
    return Array.isArray(json.content) ? json.content : json;
}

async function moderatorDelete(reviewId) {
    const res = await fetch(`/api/v1/reviews/admin/${reviewId}`, {
        method: "DELETE",
        credentials: "include",
    });
    if (!res.ok) throw new Error("Delete failed");
}

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------
export default function ProductReviews() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const { toast } = useToast();
    const queryClient = useQueryClient();
    const isModerator = canModerate(user);

    const { data: product } = useQuery({
        queryKey: ["product", id],
        queryFn: () => productApi.getProductById(id),
        enabled: !!id,
    });

    // Attempt to load real reviews; fall back to dummy data on failure
    const { data: liveReviews, isError: reviewsError } = useQuery({
        queryKey: ["reviews", id],
        queryFn: () => fetchReviews(id),
        enabled: !!id,
        retry: 1,
    });

    const reviews = reviewsError || !liveReviews ? DUMMY_REVIEWS : liveReviews;
    const usingDummy = reviewsError || !liveReviews;

    const deleteMutation = useMutation({
        mutationFn: moderatorDelete,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["reviews", id] });
            toast({ title: "Review removed", description: "The review has been deleted." });
        },
        onError: () => {
            toast({ title: "Delete failed", description: "Could not remove the review.", variant: "destructive" });
        },
    });

    const avgRating = reviews.length
        ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
        : "0.0";
    const breakdown = [5, 4, 3, 2, 1].map((s) => ({
        star: s,
        count: reviews.filter((r) => r.rating === s).length,
    }));

    return (
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
            {/* Back */}
            <button
                onClick={() => navigate("/products")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Products
            </button>

            {/* Product header */}
            <div className="bg-white border border-border rounded-xl p-6">
                <div className="flex items-start gap-4">
                    <div className="w-16 h-16 rounded-lg bg-secondary flex items-center justify-center text-3xl shrink-0">
                        🎨
                    </div>
                    <div>
                        <h1 className="font-extrabold text-xl">{product?.name ?? `Product #${id}`}</h1>
                        <p className="text-muted-foreground text-sm mt-0.5">Customer Reviews</p>
                    </div>
                </div>
            </div>

            {/* Moderator banner */}
            {isModerator && (
                <div className="flex items-center gap-2 px-4 py-3 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-800">
                    <ShieldAlert className="w-4 h-4 shrink-0" />
                    <span>
                        <strong>Moderator view</strong> — click the trash icon on any review to remove it.
                        {usingDummy && " (showing sample data — review service may be offline)"}
                    </span>
                </div>
            )}

            {/* Fallback notice for regular users when review service is down */}
            {usingDummy && !isModerator && (
                <p className="text-xs text-muted-foreground text-center">
                    Showing sample reviews — live review service is currently unavailable.
                </p>
            )}

            {/* Rating summary */}
            <div className="bg-white border border-border rounded-xl p-6">
                <div className="flex flex-col sm:flex-row gap-8 items-start">
                    <div className="flex flex-col items-center gap-2 shrink-0">
                        <span className="text-6xl font-extrabold text-foreground">{avgRating}</span>
                        <StarRow rating={Math.round(Number(avgRating))} max={5} size="lg" />
                        <span className="text-sm text-muted-foreground">{reviews.length} reviews</span>
                    </div>
                    <div className="flex-1 w-full space-y-2">
                        {breakdown.map(({ star, count }) => (
                            <RatingBar key={star} star={star} count={count} total={reviews.length} />
                        ))}
                    </div>
                </div>
            </div>

            {/* Review list */}
            <div className="space-y-4">
                <div className="flex items-center gap-2">
                    <MessageSquare className="w-4 h-4 text-muted-foreground" />
                    <h2 className="font-bold text-lg">All Reviews</h2>
                </div>
                {reviews.length === 0 && (
                    <p className="text-sm text-muted-foreground text-center py-6">
                        No reviews yet for this product.
                    </p>
                )}
                {reviews.map((r) => (
                    <ReviewCard
                        key={r.id}
                        review={r}
                        isModerator={isModerator}
                        onModeratorDelete={(reviewId) => deleteMutation.mutate(reviewId)}
                    />
                ))}
            </div>

            {/* Write review placeholder */}
            <div className="bg-secondary/40 border border-dashed border-border rounded-xl p-6 text-center space-y-2">
                <p className="font-semibold">Have this product?</p>
                <p className="text-sm text-muted-foreground">Sign in to leave a review.</p>
                <Button variant="outline" size="sm" disabled>Write a Review</Button>
            </div>
        </div>
    );
}
