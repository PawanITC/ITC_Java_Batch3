import { useState, useEffect, useCallback } from "react";
import { createPortal } from "react-dom";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Star, ThumbsUp, MessageSquare, Trash2, ShieldAlert, Send, ChevronLeft, ChevronRight, Package, Tag, DollarSign, ZoomIn, X } from "lucide-react";
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
                                "w-2.5 h-2.5 rounded-full transition-all",
                                i === idx ? "bg-white scale-125" : "bg-white/35 hover:bg-white/60"
                            )}
                        />
                    ))}
                </div>
            )}
        </div>,
        document.body
    );
}

// ---------------------------------------------------------------------------
// Product image gallery (shows multiple images if available)
// ---------------------------------------------------------------------------
function ProductGallery({ images, name }) {
    const [idx, setIdx] = useState(0);
    const [lightboxOpen, setLightboxOpen] = useState(false);

    if (!images?.length) {
        return (
            <div className="w-full h-48 bg-secondary rounded-lg flex items-center justify-center text-5xl">🎨</div>
        );
    }
    return (
        <>
            <div className="relative group">
                {/* Main image — click to open lightbox */}
                <button
                    onClick={() => setLightboxOpen(true)}
                    className="w-full block focus:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg overflow-hidden"
                    title="Click to zoom"
                >
                    <img
                        src={images[idx]}
                        alt={`${name} — view ${idx + 1}`}
                        className="w-full h-64 sm:h-80 object-cover rounded-lg bg-secondary transition-transform duration-300 group-hover:scale-[1.01]"
                        onError={(e) => { e.target.style.display = "none"; }}
                    />
                    {/* Zoom hint overlay */}
                    <div className="absolute inset-0 rounded-lg bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
                        <div className="opacity-0 group-hover:opacity-100 transition-opacity bg-black/60 rounded-full p-2.5">
                            <ZoomIn className="w-5 h-5 text-white" />
                        </div>
                    </div>
                </button>

                {/* Prev / Next arrows */}
                {images.length > 1 && (
                    <div className="absolute inset-0 flex items-center justify-between px-2 pointer-events-none">
                        <button
                            onClick={(e) => { e.stopPropagation(); setIdx((i) => Math.max(0, i - 1)); }}
                            disabled={idx === 0}
                            className="pointer-events-auto w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center disabled:opacity-30 hover:bg-black/70 transition"
                        >
                            <ChevronLeft className="w-4 h-4" />
                        </button>
                        <button
                            onClick={(e) => { e.stopPropagation(); setIdx((i) => Math.min(images.length - 1, i + 1)); }}
                            disabled={idx === images.length - 1}
                            className="pointer-events-auto w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center disabled:opacity-30 hover:bg-black/70 transition"
                        >
                            <ChevronRight className="w-4 h-4" />
                        </button>
                    </div>
                )}
            </div>

            {/* Dot indicators */}
            {images.length > 1 && (
                <div className="flex justify-center gap-1.5 mt-3">
                    {images.map((_, i) => (
                        <button key={i} onClick={() => setIdx(i)} className={cn("w-2 h-2 rounded-full transition-colors", i === idx ? "bg-primary" : "bg-border")} />
                    ))}
                </div>
            )}

            {/* Thumbnail strip for multi-image products */}
            {images.length > 1 && (
                <div className="flex gap-2 mt-3 overflow-x-auto pb-1">
                    {images.map((src, i) => (
                        <button
                            key={i}
                            onClick={() => setIdx(i)}
                            className={cn(
                                "shrink-0 w-14 h-14 rounded-md overflow-hidden border-2 transition-all",
                                i === idx ? "border-primary" : "border-transparent hover:border-border"
                            )}
                        >
                            <img src={src} alt="" className="w-full h-full object-cover" />
                        </button>
                    ))}
                </div>
            )}

            <Lightbox
                images={images}
                startIdx={idx}
                open={lightboxOpen}
                onClose={() => setLightboxOpen(false)}
            />
        </>
    );
}

// ---------------------------------------------------------------------------
// Write Review Form
// ---------------------------------------------------------------------------
function WriteReviewForm({ productId, onSuccess }) {
    const { toast } = useToast();
    const queryClient = useQueryClient();
    const [rating, setRating] = useState(0);
    const [title, setTitle] = useState("");
    const [comment, setComment] = useState("");

    const submitMutation = useMutation({
        mutationFn: (payload) => submitReview(productId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["reviews", productId] });
            toast({ title: "Review submitted!", description: "Thanks for sharing your experience." });
            setRating(0); setTitle(""); setComment("");
            if (onSuccess) onSuccess();
        },
        onError: (err) => {
            const msg = err?.message ?? "Something went wrong.";
            if (msg.includes("already reviewed") || msg.includes("403")) {
                toast({ title: "Already reviewed", description: "You've already submitted a review for this product.", variant: "destructive" });
            } else {
                toast({ title: "Submission failed", description: msg, variant: "destructive" });
            }
        },
    });

    const handleSubmit = (e) => {
        e.preventDefault();
        if (rating === 0) { toast({ title: "Rating required", description: "Please select a star rating.", variant: "destructive" }); return; }
        if (!comment.trim()) { toast({ title: "Comment required", description: "Please write a short review.", variant: "destructive" }); return; }
        submitMutation.mutate({ title: title.trim() || undefined, comment: comment.trim(), rating });
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label className="text-sm font-medium mb-2 block">Your Rating *</label>
                <StarPicker value={rating} onChange={setRating} />
            </div>
            <div>
                <label className="text-sm font-medium mb-1 block">Title <span className="text-muted-foreground font-normal">(optional)</span></label>
                <input
                    type="text"
                    maxLength={200}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Summarise your experience…"
                    className="w-full text-sm border border-border rounded-lg px-3 py-2 bg-white focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
            </div>
            <div>
                <label className="text-sm font-medium mb-1 block">Review *</label>
                <textarea
                    required
                    maxLength={2000}
                    rows={4}
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder="What did you think? Quality, delivery, overall impression…"
                    className="w-full text-sm border border-border rounded-lg px-3 py-2 bg-white resize-none focus:outline-none focus:ring-2 focus:ring-primary/30"
                />
                <p className="text-xs text-muted-foreground text-right mt-0.5">{comment.length}/2000</p>
            </div>
            <Button type="submit" disabled={submitMutation.isPending} className="gap-2">
                {submitMutation.isPending ? "Submitting…" : <><Send className="w-4 h-4" /> Submit Review</>}
            </Button>
        </form>
    );
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
    const [showForm, setShowForm] = useState(false);

    const { data: product } = useQuery({
        queryKey: ["product", id],
        queryFn: () => productApi.getProduct(id).then((r) => r?.data ?? r),
        enabled: !!id,
    });

    // Load live reviews; merge dummy ones in below real ones for MVP body
    const { data: liveReviews, isError: reviewsError } = useQuery({
        queryKey: ["reviews", id],
        queryFn: () => fetchReviews(id),
        enabled: !!id,
        retry: 1,
    });

    // When service is up: real reviews first, then dummy padding underneath
    // When service is down: show only dummy reviews with a notice
    const serviceDown = reviewsError || !liveReviews;
    const reviews = serviceDown
        ? DUMMY_REVIEWS
        : [...(liveReviews ?? []), ...DUMMY_REVIEWS];
    const usingDummy = serviceDown;

    const deleteMutation = useMutation({
        mutationFn: moderatorDelete,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["reviews", id] });
            toast({ title: "Review removed", description: "The review has been deleted." });
        },
        onError: (err) => {
            toast({ title: "Delete failed", description: err?.message ?? "Could not remove the review.", variant: "destructive" });
        },
    });

    const avgRating = reviews.length
        ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
        : "0.0";
    const breakdown = [5, 4, 3, 2, 1].map((s) => ({
        star: s,
        count: reviews.filter((r) => r.rating === s).length,
    }));

    const images = product?.imageUrls ?? (product?.imageUrl ? [product.imageUrl] : []);

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
            {/* Back */}
            <button
                onClick={() => navigate("/products")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Products
            </button>

            {/* ── Product Information Card — two-col on desktop ── */}
            <div className="bg-white border border-border rounded-xl overflow-hidden">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-0">
                    {/* Gallery — left column */}
                    {images.length > 0 ? (
                        <div className="p-5 border-b md:border-b-0 md:border-r border-border">
                            <ProductGallery images={images} name={product?.name ?? ""} />
                        </div>
                    ) : (
                        <div className="p-5 border-b md:border-b-0 md:border-r border-border flex items-center justify-center">
                            <div className="w-full h-64 bg-secondary rounded-lg flex items-center justify-center text-5xl">🎨</div>
                        </div>
                    )}

                    {/* Details — right column */}
                    <div className="p-6 flex flex-col justify-between space-y-4">
                        <div className="space-y-3">
                            <div>
                                <h1 className="font-extrabold text-2xl leading-tight">{product?.name ?? `Product #${id}`}</h1>
                                {product?.category?.name && (
                                    <span className="inline-flex items-center gap-1 mt-2 text-xs text-muted-foreground bg-secondary px-2.5 py-1 rounded-full">
                                        <Tag className="w-3 h-3" /> {product.category.name}
                                    </span>
                                )}
                            </div>

                            {product?.description && (
                                <p className="text-sm text-muted-foreground leading-relaxed">{product.description}</p>
                            )}
                        </div>

                        <div className="flex flex-wrap items-center gap-4 pt-2 border-t border-border">
                            {product?.price != null && (
                                <div className="flex items-center gap-1.5">
                                    <DollarSign className="w-4 h-4 text-primary" />
                                    <span className="text-2xl font-extrabold">£{Number(product.price).toFixed(2)}</span>
                                </div>
                            )}
                            {product?.stockQuantity != null && (
                                <div className={`flex items-center gap-1.5 text-sm px-2.5 py-1 rounded-full ${
                                    product.stockQuantity > 0
                                        ? "bg-green-50 text-green-700"
                                        : "bg-red-50 text-red-700"
                                }`}>
                                    <Package className="w-3.5 h-3.5" />
                                    {product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : "Out of stock"}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Moderator banner */}
            {isModerator && (
                <div className="flex items-center gap-2 px-4 py-3 bg-amber-50 border border-amber-200 rounded-xl text-sm text-amber-800">
                    <ShieldAlert className="w-4 h-4 shrink-0" />
                    <span>
                        <strong>Moderator view</strong> — click the trash icon on any review to remove it.
                        {usingDummy && " (review service may be offline — showing sample data only)"}
                    </span>
                </div>
            )}

            {/* Only show notice when service is completely down */}
            {usingDummy && !isModerator && (
                <p className="text-xs text-muted-foreground text-center italic">
                    Live reviews are temporarily unavailable — showing sample reviews.
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
                        No reviews yet — be the first!
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

            {/* ── Write a Review ── */}
            {user && !isModerator && !usingDummy && (
                <div className="bg-white border border-border rounded-xl p-6 space-y-4">
                    <div className="flex items-center justify-between">
                        <h2 className="font-bold text-lg">Write a Review</h2>
                        {!showForm && (
                            <Button size="sm" onClick={() => setShowForm(true)}>Leave a Review</Button>
                        )}
                    </div>
                    {showForm && (
                        <WriteReviewForm productId={id} onSuccess={() => setShowForm(false)} />
                    )}
                </div>
            )}

            {!user && (
                <div className="bg-secondary/40 border border-dashed border-border rounded-xl p-6 text-center space-y-2">
                    <p className="font-semibold">Have this product?</p>
                    <p className="text-sm text-muted-foreground">Sign in to leave a review.</p>
                    <Button variant="outline" size="sm" onClick={() => navigate("/login")}>Sign In</Button>
                </div>
            )}
        </div>
    );
}
