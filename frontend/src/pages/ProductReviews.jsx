import { useState, useEffect, useCallback } from "react";
import { createPortal } from "react-dom";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Star, ThumbsUp, MessageSquare, Trash2, ShieldAlert, Send, ChevronLeft, ChevronRight, Package, Tag, ShoppingCart, Loader2, ZoomIn, X, CheckCircle2 } from "lucide-react";
import { productApi } from "../lib/productApi";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "@/context/CartContext";
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
        <div className="bg-card border border-border rounded-xl p-5 space-y-3">
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
    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message ?? `Delete failed (${res.status})`);
    }
}

async function submitReview(productId, payload) {
    const res = await fetch(`/api/v1/reviews/${productId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
    });
    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message ?? `Submission failed (${res.status})`);
    }
    return res.json();
}

// ---------------------------------------------------------------------------
// Interactive star picker for write-review form
// ---------------------------------------------------------------------------
function StarPicker({ value, onChange }) {
    const [hover, setHover] = useState(0);
    const display = hover || value;
    return (
        <div className="flex items-center gap-1">
            {[1, 2, 3, 4, 5].map((s) => (
                <button
                    key={s}
                    type="button"
                    onClick={() => onChange(s)}
                    onMouseEnter={() => setHover(s)}
                    onMouseLeave={() => setHover(0)}
                    className="p-0.5 focus:outline-none"
                >
                    <Star className={cn("w-7 h-7 transition-colors", s <= display ? "fill-accent text-accent" : "text-border fill-border")} />
                </button>
            ))}
            {value > 0 && <span className="ml-2 text-sm text-muted-foreground">{["", "Poor", "Fair", "Good", "Great", "Excellent"][value]}</span>}
        </div>
    );
}

// ---------------------------------------------------------------------------
// Lightbox — full-screen image viewer, portal-based (no shadcn Dialog quirks)
// Mobile-friendly: tap outside image closes, swipe-friendly layout
// ---------------------------------------------------------------------------
function Lightbox({ images, startIdx, open, onClose }) {
    const [idx, setIdx] = useState(startIdx ?? 0);

    const prev = useCallback(() => setIdx((i) => Math.max(0, i - 1)), []);
    const next = useCallback(() => setIdx((i) => Math.min(images.length - 1, i + 1)), [images.length]);

    // Sync startIdx when gallery thumbnail changes before opening
    useEffect(() => { if (open) setIdx(startIdx ?? 0); }, [open, startIdx]);

    // Keyboard navigation + body scroll lock
    useEffect(() => {
        if (!open) return;
        const handler = (e) => {
            if (e.key === "ArrowLeft") prev();
            if (e.key === "ArrowRight") next();
            if (e.key === "Escape") onClose();
        };
        document.addEventListener("keydown", handler);
        document.body.style.overflow = "hidden";
        return () => {
            document.removeEventListener("keydown", handler);
            document.body.style.overflow = "";
        };
    }, [open, prev, next, onClose]);

    if (!open) return null;

    return createPortal(
        <div
            className="fixed inset-0 z-[9999] bg-black/95 flex items-center justify-center"
            onClick={onClose}           // tap outside image → close
            role="dialog"
            aria-modal="true"
        >
            {/* Counter */}
            {images.length > 1 && (
                <span className="absolute top-4 left-4 text-white/50 text-xs font-mono pointer-events-none select-none">
                    {idx + 1} / {images.length}
                </span>
            )}

            {/* Close */}
            <button
                onClick={onClose}
                className="absolute top-4 right-4 w-9 h-9 rounded-full bg-white/10 hover:bg-white/25 transition-colors text-white flex items-center justify-center"
                aria-label="Close"
            >
                <X className="w-5 h-5" />
            </button>

            {/* Prev */}
            {images.length > 1 && (
                <button
                    onClick={(e) => { e.stopPropagation(); prev(); }}
                    disabled={idx === 0}
                    className="absolute left-3 sm:left-6 w-10 h-10 rounded-full bg-white/10 hover:bg-white/25 transition-colors text-white flex items-center justify-center disabled:opacity-20"
                    aria-label="Previous image"
                >
                    <ChevronLeft className="w-6 h-6" />
                </button>
            )}

            {/* Image — stopPropagation so clicking the image itself doesn't close */}
            <img
                src={images[idx]}
                alt={`View ${idx + 1}`}
                className="max-h-[88vh] max-w-[88vw] sm:max-w-[80vw] object-contain rounded-lg shadow-2xl select-none"
                draggable={false}
                onClick={(e) => e.stopPropagation()}
            />

            {/* Next */}
            {images.length > 1 && (
                <button
                    onClick={(e) => { e.stopPropagation(); next(); }}
                    disabled={idx === images.length - 1}
                    className="absolute right-3 sm:right-6 w-10 h-10 rounded-full bg-white/10 hover:bg-white/25 transition-colors text-white flex items-center justify-center disabled:opacity-20"
                    aria-label="Next image"
                >
                    <ChevronRight className="w-6 h-6" />
                </button>
            )}

            {/* Dot indicators */}
            {images.length > 1 && (
                <div className="absolute bottom-5 flex gap-2" onClick={(e) => e.stopPropagation()}>
                    {images.map((_, i) => (
                        <button
                            key={i}
                            onClick={() => setIdx(i)}
                            className={cn(
                                "w-2 h-2 rounded-full transition-all",
                                i === idx ? "bg-white scale-125" : "bg-white/40 hover:bg-white/70"
                            )}
                            aria-label={`Go to image ${i + 1}`}
                        />
                    ))}
                </div>
            )}
        </div>,
        document.body
    );
}

// ---------------------------------------------------------------------------
// Main page component
// ---------------------------------------------------------------------------
export default function ProductReviews() {
    const { id: productId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const { addItem } = useCart();
    const queryClient = useQueryClient();
    const { toast } = useToast();
    const isModerator = canModerate(user);

    // Product fetch
    const { data: productData, isLoading: productLoading } = useQuery({
        queryKey: ["product", productId],
        queryFn: () => productApi.getProduct(productId),
        enabled: !!productId,
    });
    const product = productData?.data ?? productData;

    // Reviews fetch
    const { data: reviewsRaw, isLoading: reviewsLoading, isError: reviewsError } = useQuery({
        queryKey: ["reviews", productId],
        queryFn: () => fetchReviews(productId),
        enabled: !!productId,
    });
    const reviews = reviewsError ? DUMMY_REVIEWS : (reviewsRaw ?? []);

    // Moderator delete mutation
    const deleteMutation = useMutation({
        mutationFn: moderatorDelete,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["reviews", productId] });
            toast({ title: "Review removed", description: "The review has been deleted." });
        },
        onError: (err) => {
            toast({ title: "Delete failed", description: err.message, variant: "destructive" });
        },
    });

    // Write-review form state
    const [rating, setRating] = useState(0);
    const [title, setTitle] = useState("");
    const [comment, setComment] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const submitMutation = useMutation({
        mutationFn: (payload) => submitReview(productId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["reviews", productId] });
            setRating(0); setTitle(""); setComment("");
            toast({ title: "Review submitted!", description: "Thanks for your feedback." });
        },
        onError: (err) => {
            toast({ title: "Submission failed", description: err.message, variant: "destructive" });
        },
    });

    const handleSubmitReview = async (e) => {
        e.preventDefault();
        if (rating === 0) { toast({ title: "Please select a rating", variant: "destructive" }); return; }
        if (!comment.trim()) { toast({ title: "Please write a comment", variant: "destructive" }); return; }
        setSubmitting(true);
        await submitMutation.mutateAsync({ rating, title, comment });
        setSubmitting(false);
    };

    // Lightbox state
    const [lightboxOpen, setLightboxOpen] = useState(false);
    const [lightboxStart, setLightboxStart] = useState(0);

    const openLightbox = (idx) => { setLightboxStart(idx); setLightboxOpen(true); };

    // Rating stats
    const totalReviews = reviews.length;
    const avgRating = totalReviews ? (reviews.reduce((s, r) => s + (r.rating ?? 0), 0) / totalReviews) : 0;
    const distribution = [5, 4, 3, 2, 1].map((s) => ({ star: s, count: reviews.filter((r) => r.rating === s).length }));

    const images = product?.imageUrls ?? (product?.imageUrl ? [product.imageUrl] : []);

    if (productLoading) {
        return (
            <div className="flex justify-center py-24">
                <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
        );
    }

    if (!product) {
        return (
            <div className="max-w-2xl mx-auto px-4 py-16 text-center space-y-4">
                <Package className="w-12 h-12 text-muted-foreground mx-auto" />
                <h2 className="text-xl font-semibold">Product not found</h2>
                <Button onClick={() => navigate("/products")}>Back to Products</Button>
            </div>
        );
    }

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-10">
            {/* Back */}
            <button
                onClick={() => navigate("/products")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Products
            </button>

            {/* Product hero */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                {/* Gallery */}
                <div className="space-y-3">
                    {images.length > 0 ? (
                        <>
                            <div
                                className="relative rounded-xl overflow-hidden border border-border cursor-zoom-in group"
                                onClick={() => openLightbox(0)}
                            >
                                <img
                                    src={images[0]}
                                    alt={product.name}
                                    className="w-full h-72 object-cover group-hover:scale-105 transition-transform duration-300"
                                />
                                <div className="absolute bottom-3 right-3 bg-black/50 text-white rounded-full p-1.5">
                                    <ZoomIn className="w-4 h-4" />
                                </div>
                            </div>
                            {images.length > 1 && (
                                <div className="flex gap-2 overflow-x-auto pb-1">
                                    {images.map((url, i) => (
                                        <button
                                            key={i}
                                            onClick={() => openLightbox(i)}
                                            className="w-16 h-16 rounded-lg overflow-hidden border border-border hover:border-primary transition-colors shrink-0"
                                        >
                                            <img src={url} alt={`View ${i + 1}`} className="w-full h-full object-cover" />
                                        </button>
                                    ))}
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="rounded-xl border bg-muted h-72 flex items-center justify-center text-6xl">🎨</div>
                    )}
                </div>

                {/* Product info */}
                <div className="space-y-4">
                    {product.categoryName && (
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                            <Tag className="w-3.5 h-3.5" /> {product.categoryName}
                        </div>
                    )}
                    <h1 className="text-2xl font-extrabold leading-tight">{product.name}</h1>
                    {product.description && (
                        <p className="text-muted-foreground text-sm leading-relaxed">{product.description}</p>
                    )}
                    <div className="flex items-center gap-3">
                        <StarRow rating={Math.round(avgRating)} size="md" />
                        <span className="text-sm text-muted-foreground">{avgRating.toFixed(1)} ({totalReviews} review{totalReviews !== 1 ? "s" : ""})</span>
                    </div>
                    <p className="text-3xl font-extrabold">£{Number(product.price).toFixed(2)}</p>
                    <Button
                        size="lg"
                        className="w-full sm:w-auto gap-2"
                        onClick={() => addItem(product.id, 1)}
                    >
                        <ShoppingCart className="w-4 h-4" /> Add to Cart
                    </Button>
                </div>
            </div>

            {/* Rating breakdown */}
            {totalReviews > 0 && (
                <div className="bg-card border border-border rounded-xl p-6 space-y-4">
                    <h2 className="font-bold text-lg flex items-center gap-2">
                        <Star className="w-5 h-5 fill-accent text-accent" /> Customer Ratings
                    </h2>
                    <div className="flex flex-col sm:flex-row gap-6 items-start sm:items-center">
                        <div className="text-center">
                            <p className="text-5xl font-extrabold">{avgRating.toFixed(1)}</p>
                            <StarRow rating={Math.round(avgRating)} size="md" />
                            <p className="text-xs text-muted-foreground mt-1">{totalReviews} reviews</p>
                        </div>
                        <div className="flex-1 w-full space-y-1.5">
                            {distribution.map(({ star, count }) => (
                                <RatingBar key={star} star={star} count={count} total={totalReviews} />
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* Reviews list */}
            <div className="space-y-4">
                <h2 className="font-bold text-lg flex items-center gap-2">
                    <MessageSquare className="w-5 h-5" /> Reviews
                    {reviewsError && <span className="text-xs font-normal text-muted-foreground ml-2">(sample data — review service unavailable)</span>}
                </h2>

                {reviewsLoading && (
                    <div className="flex justify-center py-12">
                        <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
                    </div>
                )}

                {!reviewsLoading && reviews.length === 0 && (
                    <div className="bg-card border rounded-xl p-8 text-center text-muted-foreground text-sm">
                        No reviews yet. Be the first to leave one!
                    </div>
                )}

                {reviews.map((review) => (
                    <ReviewCard
                        key={review.id}
                        review={review}
                        isModerator={isModerator}
                        onModeratorDelete={(id) => deleteMutation.mutate(id)}
                    />
                ))}
            </div>

            {/* Write a review */}
            <div className="bg-card border border-border rounded-xl p-6 space-y-4">
                <h2 className="font-bold text-lg">Write a Review</h2>
                <form onSubmit={handleSubmitReview} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium mb-2">Your Rating</label>
                        <StarPicker value={rating} onChange={setRating} />
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Title <span className="text-muted-foreground font-normal">(optional)</span></label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="Summarise your experience…"
                            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                            maxLength={120}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Comment</label>
                        <textarea
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            placeholder="Tell others about your experience with this product…"
                            rows={4}
                            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary resize-none"
                            maxLength={1000}
                        />
                        <p className="text-xs text-muted-foreground text-right mt-1">{comment.length}/1000</p>
                    </div>
                    <Button type="submit" disabled={submitting || submitMutation.isPending} className="gap-2">
                        {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                        Submit Review
                    </Button>
                </form>
            </div>

            {/* Lightbox */}
            <Lightbox
                images={images}
                startIdx={lightboxStart}
                open={lightboxOpen}
                onClose={() => setLightboxOpen(false)}
            />
        </div>
    );
}
