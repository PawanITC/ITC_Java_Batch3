import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { ShoppingCart, Search, Loader2, RefreshCw, SlidersHorizontal, Star, MessageSquare } from "lucide-react";
import { productApi } from "@/lib/productApi";
import { useCart } from "@/context/CartContext";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "@/lib/utils";

function ProductCard({ product }) {
    const { addItem, loading } = useCart();
    const navigate = useNavigate();
    const [adding, setAdding] = useState(false);
    const [added, setAdded] = useState(false);

    const handleAdd = async () => {
        setAdding(true);
        await addItem(product.id, 1);
        setAdding(false);
        setAdded(true);
        setTimeout(() => setAdded(false), 1800);
    };

    return (
        <div className="bg-card border border-border rounded-xl overflow-hidden hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 flex flex-col group">
            <div
                className="relative overflow-hidden cursor-pointer"
                onClick={() => navigate(`/products/${product.id}/reviews`)}
                title="View product details & reviews"
            >
                {product.imageUrls?.[0] ? (
                    <img
                        src={product.imageUrls[0]}
                        alt={product.name}
                        className="w-full h-52 object-cover group-hover:scale-105 transition-transform duration-300"
                    />
                ) : (
                    <div className="w-full h-52 bg-gradient-to-br from-secondary to-muted flex items-center justify-center text-5xl">
                        🎨
                    </div>
                )}
                {product.categoryName && (
                    <div className="absolute top-3 left-3">
                        <Badge className="bg-white text-gray-900 border-0 shadow-sm text-xs font-medium">
                            {product.categoryName}
                        </Badge>
                    </div>
                )}
                {/* Hover overlay hint */}
                <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors duration-200 flex items-center justify-center">
                    <span className="opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-white text-xs font-semibold bg-black/50 px-3 py-1 rounded-full">
                        View Details
                    </span>
                </div>
            </div>

            <div className="p-4 flex flex-col flex-1 gap-2">
                <h3 className="font-semibold text-sm leading-snug line-clamp-2 text-foreground">{product.name}</h3>
                {product.description && (
                    <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">{product.description}</p>
                )}

                <div className="mt-auto pt-4 space-y-2">
                    <div className="flex items-center justify-between">
                        <span className="text-lg font-bold text-foreground">£{Number(product.price).toFixed(2)}</span>
                        <Button
                            size="sm"
                            onClick={handleAdd}
                            disabled={loading || adding}
                            className={cn(
                                "gap-1.5 transition-all duration-200",
                                added ? "bg-green-600 hover:bg-green-600" : ""
                            )}
                        >
                            {adding ? (
                                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                                <ShoppingCart className="w-3.5 h-3.5" />
                            )}
                            {added ? "Added!" : "Add to Cart"}
                        </Button>
                    </div>
                    <button
                        onClick={() => navigate(`/products/${product.id}/reviews`)}
                        className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <Star className="w-3 h-3 fill-accent text-accent" />
                        <Star className="w-3 h-3 fill-accent text-accent" />
                        <Star className="w-3 h-3 fill-accent text-accent" />
                        <Star className="w-3 h-3 fill-accent text-accent" />
                        <Star className="w-3 h-3 fill-accent text-accent" />
                        <span className="ml-1">4.6 · See reviews</span>
                        <MessageSquare className="w-3 h-3 ml-0.5" />
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function ProductsPage() {
    const [search, setSearch] = useState("");
    const [categoryId, setCategoryId] = useState("");
    const [page, setPage] = useState(0);

    const { data: categoryData } = useQuery({
        queryKey: ["categories"],
        queryFn: productApi.getCategories,
    });
    const categories = categoryData?.data ?? categoryData ?? [];

    const params = { page, size: 12, ...(search && { search }), ...(categoryId && { categoryId }) };

    const { data, isLoading, isError, refetch } = useQuery({
        queryKey: ["products", params],
        queryFn: () => productApi.getProducts(params),
    });

    const products = data?.content ?? data?.data ?? data ?? [];
    const totalPages = data?.totalPages ?? 1;

    const handleSearch = (e) => { setSearch(e.target.value); setPage(0); };
    const handleCategory = (val) => { setCategoryId(val === "all" ? "" : val); setPage(0); };

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">

            {/* Hero bar */}
            <div className="mb-5">
                <h1 className="text-2xl md:text-3xl font-extrabold text-foreground tracking-tight mb-1">
                    Browse Our Collection
                </h1>
                <p className="text-muted-foreground text-sm">
                    Handpicked art prints, posters &amp; collectibles for every taste.
                </p>
            </div>

            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-3 mb-5">
                <div className="relative flex-1 max-w-sm">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                        placeholder="Search products…"
                        value={search}
                        onChange={handleSearch}
                        className="pl-10 bg-background"
                    />
                </div>

                {categories.length > 0 && (
                    <Select onValueChange={handleCategory} defaultValue="all">
                        <SelectTrigger className="w-full sm:w-44 bg-background">
                            <SlidersHorizontal className="w-4 h-4 mr-2 text-muted-foreground" />
                            <SelectValue placeholder="All Categories" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">All Categories</SelectItem>
                            {categories.map((cat) => (
                                <SelectItem key={cat.id} value={String(cat.id)}>{cat.name}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                )}
            </div>

            {/* Loading */}
            {isLoading && (
                <div className="flex justify-center py-20">
                    <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
                </div>
            )}

            {/* Error */}
            {isError && (
                <div className="flex flex-col items-center gap-4 py-16 text-center">
                    <p className="text-destructive font-medium">Failed to load products.</p>
                    <Button variant="outline" onClick={refetch} className="gap-2">
                        <RefreshCw className="w-4 h-4" /> Try Again
                    </Button>
                </div>
            )}

            {/* Empty */}
            {!isLoading && !isError && products.length === 0 && (
                <div className="flex flex-col items-center justify-center py-20 gap-4 text-center">
                    <div className="w-20 h-20 rounded-full bg-muted flex items-center justify-center text-4xl">🎨</div>
                    <h2 className="text-xl font-bold">No products found</h2>
                    <p className="text-muted-foreground text-sm max-w-xs">Try adjusting your search or clearing the category filter.</p>
                </div>
            )}

            {/* Grid */}
            {products.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                    {products.map((p) => (
                        <ProductCard key={p.id} product={p} />
                    ))}
                </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
                <div className="flex justify-center items-center gap-3 mt-8 text-sm">
                    <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                        Previous
                    </Button>
                    <span className="text-muted-foreground">Page {page + 1} of {totalPages}</span>
                    <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                        Next
                    </Button>
                </div>
            )}
        </div>
    );
}
