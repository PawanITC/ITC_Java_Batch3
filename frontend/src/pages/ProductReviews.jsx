import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Star, ThumbsUp, MessageSquare } from "lucide-react";
import { productApi } from "../lib/productApi";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const DUMMY_REVIEWS = [
    { id: 1, author: "Sofia M.", rating: 5, date: "April 22, 2026", title: "Absolutely stunning!", body: "The colors are so vibrant and it looks even better in person. Shipped fast, packaging was excellent. Will definitely order again!", likes: 24 },
    { id: 2, author: "James T.", rating: 4, date: "April 10, 2026", title: "Great quality print", body: "Really happy with this purchase. The detail is crisp and the paper feels premium. Knocked off one star only because delivery took a little longer than expected.", likes: 11 },
    { id: 3, author: "Priya K.", rating: 5, date: "March 30, 2026", title: "Perfect gift!", body: "Bought this as a birthday gift and the recipient absolutely loved it. The frame-ready sizing is a huge bonus. Highly recommend FunkArt!", likes: 18 },
    { id: 4, author: "Daniel R.", rating: 3, date: "March 14, 2026", title: "Good but not perfect", body: "Nice product overall. The colours are slightly darker than on the website, but still looks good on the wall. Customer service was very responsive.", likes: 6 },
    { id: 5, author: "Amelia W.", rating: 5, date: "February 28, 2026", title: "Exceeded expectations", body: "I ordered this on a whim and I'm so glad I did. Museum-quality print, very true to the photo online. My living room looks incredible now.", likes: 31 },
];

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

function ReviewCard({ review }) {
    const [liked, setLiked] = useState(false);
    return (
        <div className="bg-white border border-border rounded-xl p-5 space-y-3">
            <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-primary flex items-center justify-center text-sm font-bold text-primary-foreground">
                        {review.author[0]}
                    </div>
                    <div>
                        <p className="font-semibold text-sm">{review.author}</p>
                        <p className="text-xs text-muted-foreground">{review.date}</p>
                    </div>
                </div>
                <StarRow rating={review.rating} />
            </div>
            <div>
                <p className="font-semibold text-sm mb-1">{review.title}</p>
                <p className="text-sm text-muted-foreground leading-relaxed">{review.body}</p>
            </div>
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
                Helpful ({review.likes + (liked ? 1 : 0)})
            </button>
        </div>
    );
}

export default function ProductReviews() {
    const { id } = useParams();
    const navigate = useNavigate();

    const { data: product } = useQuery({
        queryKey: ["product", id],
        queryFn: () => productApi.getProductById(id),
        enabled: !!id,
    });

    const avgRating = (DUMMY_REVIEWS.reduce((s, r) => s + r.rating, 0) / DUMMY_REVIEWS.length).toFixed(1);
    const breakdown = [5, 4, 3, 2, 1].map((s) => ({
        star: s,
        count: DUMMY_REVIEWS.filter((r) => r.rating === s).length,
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

            {/* Rating summary */}
            <div className="bg-white border border-border rounded-xl p-6">
                <div className="flex flex-col sm:flex-row gap-8 items-start">
                    {/* Big number */}
                    <div className="flex flex-col items-center gap-2 shrink-0">
                        <span className="text-6xl font-extrabold text-foreground">{avgRating}</span>
                        <StarRow rating={Math.round(Number(avgRating))} max={5} size="lg" />
                        <span className="text-sm text-muted-foreground">{DUMMY_REVIEWS.length} reviews</span>
                    </div>
                    {/* Bars */}
                    <div className="flex-1 w-full space-y-2">
                        {breakdown.map(({ star, count }) => (
                            <RatingBar key={star} star={star} count={count} total={DUMMY_REVIEWS.length} />
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
                {DUMMY_REVIEWS.map((r) => (
                    <ReviewCard key={r.id} review={r} />
                ))}
            </div>

            {/* Placeholder write review */}
            <div className="bg-secondary/40 border border-dashed border-border rounded-xl p-6 text-center space-y-2">
                <p className="font-semibold">Have this product?</p>
                <p className="text-sm text-muted-foreground">Sign in to leave a review.</p>
                <Button variant="outline" size="sm" disabled>Write a Review</Button>
            </div>
        </div>
    );
}