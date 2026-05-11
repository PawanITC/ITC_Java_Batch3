import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
    Package, Plus, Pencil, Trash2, Loader2, ArrowLeft,
    Search, X, CheckCircle2, XCircle,
} from "lucide-react";
import { adminProductApi, adminCategoryApi } from "@/lib/adminApi.js";
import { productApi } from "@/lib/productApi.js";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useToast } from "@/components/ui/use-toast";

/* ─── Query keys ─── */
const PRODUCTS_KEY = ["admin", "products"];
const CATS_KEY     = ["admin", "categories"];

/* ─── Blank form state ─── */
const EMPTY_FORM = {
    name: "",
    description: "",
    price: "",
    stockQuantity: "",
    brand: "",
    categoryId: "",
    imageUrl: "",
    active: true,
};

/* ══════════════════════════════════════════════════════════════
   MAIN PAGE
══════════════════════════════════════════════════════════════ */
export default function AdminProducts() {
    const navigate = useNavigate();
    const qc       = useQueryClient();
    const { toast } = useToast();

    /* search / modal state */
    const [search,      setSearch]      = useState("");
    const [modalOpen,   setModalOpen]   = useState(false);
    const [editTarget,  setEditTarget]  = useState(null); // null = create
    const [deleteId,    setDeleteId]    = useState(null);
    const [form,        setForm]        = useState(EMPTY_FORM);

    /* ── data fetching ── */
    const { data: raw, isLoading, isError, refetch } = useQuery({
        queryKey: PRODUCTS_KEY,
        queryFn: () => productApi.getProducts({ size: 200 }).then((r) => {
            /* productApi may return ApiResponse<Page<Product>> or Page<Product> */
            const body = r?.data ?? r;
            return body?.content ?? body ?? [];
        }),
    });

    const { data: categories = [] } = useQuery({
        queryKey: CATS_KEY,
        queryFn: () => adminCategoryApi.getAllCategories().then((r) => r?.data ?? r),
    });

    const products = Array.isArray(raw) ? raw : [];

    const filtered = products.filter((p) => {
        const q = search.toLowerCase();
        return (
            !q ||
            (p.name  ?? "").toLowerCase().includes(q) ||
            (p.brand ?? "").toLowerCase().includes(q) ||
            (p.categoryName ?? "").toLowerCase().includes(q)
        );
    });

    /* ── mutations ── */
    const createMutation = useMutation({
        mutationFn: (data) => adminProductApi.createProduct(data),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: PRODUCTS_KEY });
            toast({ title: "Product created", description: form.name });
            closeModal();
        },
        onError: (err) => toast({ title: "Create failed", description: err.message, variant: "destructive" }),
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, data }) => adminProductApi.updateProduct(id, data),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: PRODUCTS_KEY });
            toast({ title: "Product updated" });
            closeModal();
        },
        onError: (err) => toast({ title: "Update failed", description: err.message, variant: "destructive" }),
    });

    const deleteMutation = useMutation({
        mutationFn: (id) => adminProductApi.deleteProduct(id),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: PRODUCTS_KEY });
            toast({ title: "Product deleted" });
            setDeleteId(null);
        },
        onError: (err) => toast({ title: "Delete failed", description: err.message, variant: "destructive" }),
    });

    /* ── modal helpers ── */
    const openCreate = () => {
        setEditTarget(null);
        setForm(EMPTY_FORM);
        setModalOpen(true);
    };

    const openEdit = (p) => {
        setEditTarget(p);
        setForm({
            name:          p.name          ?? "",
            description:   p.description   ?? "",
            price:         p.price         ?? "",
            stockQuantity: p.stockQuantity ?? "",
            brand:         p.brand         ?? "",
            categoryId:    p.categoryId    ?? "",
            imageUrl:      (p.imageUrls ?? [])[0] ?? "",
            active:        p.active        ?? true,
        });
        setModalOpen(true);
    };

    const closeModal = () => {
        setModalOpen(false);
        setEditTarget(null);
        setForm(EMPTY_FORM);
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        const payload = {
            name:          form.name,
            description:   form.description,
            price:         parseFloat(form.price),
            stockQuantity: parseInt(form.stockQuantity, 10),
            brand:         form.brand,
            categoryId:    form.categoryId ? parseInt(form.categoryId, 10) : undefined,
            imageUrls:     form.imageUrl ? [form.imageUrl] : [],
            active:        form.active,
        };

        if (editTarget) {
            updateMutation.mutate({ id: editTarget.id, data: payload });
        } else {
            createMutation.mutate(payload);
        }
    };

    const isSaving = createMutation.isPending || updateMutation.isPending;

    /* ══════════════ RENDER ══════════════ */
    return (
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">

            {/* Back */}
            <button
                onClick={() => navigate("/admin")}
                className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
                <ArrowLeft className="w-4 h-4" /> Back to Dashboard
            </button>

            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
                        <Package className="w-5 h-5 text-primary-foreground" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-extrabold">Product Management</h1>
                        <p className="text-sm text-muted-foreground">Create, edit, or remove catalog products.</p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    {products.length > 0 && (
                        <Badge variant="secondary" className="text-sm px-3 py-1">
                            {products.length} products
                        </Badge>
                    )}
                    <Button onClick={openCreate} className="gap-2">
                        <Plus className="w-4 h-4" /> New Product
                    </Button>
                </div>
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                    placeholder="Search by name, brand, or category…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="pl-10 bg-background"
                />
            </div>

            {/* Loading / Error */}
            {isLoading && (
                <div className="flex justify-center py-16">
                    <Loader2 className="w-7 h-7 animate-spin text-muted-foreground" />
                </div>
            )}
            {isError && (
                <div className="bg-destructive/10 text-destructive rounded-xl px-6 py-4 flex items-center justify-between">
                    <p>Failed to load products.</p>
                    <Button variant="outline" size="sm" onClick={refetch}>Retry</Button>
                </div>
            )}

            {/* Table */}
            {!isLoading && !isError && (
                <div className="bg-card border border-border rounded-xl overflow-hidden">
                    <table className="w-full text-sm">
                        <thead className="bg-secondary/60">
                        <tr>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground">Product</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground hidden md:table-cell">Category</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground hidden sm:table-cell">Price</th>
                            <th className="text-left px-5 py-3.5 font-semibold text-muted-foreground hidden lg:table-cell">Status</th>
                            <th className="text-right px-5 py-3.5 font-semibold text-muted-foreground">Actions</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-border">
                        {filtered.length === 0 && (
                            <tr>
                                <td colSpan={5} className="text-center py-12 text-muted-foreground text-sm">
                                    {search ? "No products match your search." : "No products yet — add your first one."}
                                </td>
                            </tr>
                        )}
                        {filtered.map((p) => (
                            <tr key={p.id} className="hover:bg-muted/20 transition-colors">
                                <td className="px-5 py-4">
                                    <div className="flex items-center gap-3">
                                        {(p.imageUrls ?? [])[0] ? (
                                            <img
                                                src={p.imageUrls[0]}
                                                alt={p.name}
                                                className="w-10 h-10 rounded-lg object-cover shrink-0 bg-secondary"
                                            />
                                        ) : (
                                            <div className="w-10 h-10 rounded-lg bg-secondary flex items-center justify-center shrink-0 text-lg">
                                                🎨
                                            </div>
                                        )}
                                        <div>
                                            <p className="font-semibold">{p.name}</p>
                                            <p className="text-xs text-muted-foreground">{p.brand}</p>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-5 py-4 text-muted-foreground hidden md:table-cell">
                                    {p.categoryName ?? "—"}
                                </td>
                                <td className="px-5 py-4 font-medium hidden sm:table-cell">
                                    £{Number(p.price ?? 0).toFixed(2)}
                                </td>
                                <td className="px-5 py-4 hidden lg:table-cell">
                                    {p.active !== false ? (
                                        <span className="inline-flex items-center gap-1.5 text-xs font-medium text-green-700 bg-green-50 px-2 py-0.5 rounded-full border border-green-200">
                                            <CheckCircle2 className="w-3 h-3" /> Active
                                        </span>
                                    ) : (
                                        <span className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground bg-secondary px-2 py-0.5 rounded-full border border-border">
                                            <XCircle className="w-3 h-3" /> Inactive
                                        </span>
                                    )}
                                </td>
                                <td className="px-5 py-4">
                                    <div className="flex items-center justify-end gap-2">
                                        <button
                                            onClick={() => openEdit(p)}
                                            className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                                            aria-label="Edit product"
                                        >
                                            <Pencil className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => setDeleteId(p.id)}
                                            className="p-1.5 rounded-lg text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                                            aria-label="Delete product"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Create / Edit Modal */}
            {modalOpen && (
                <ProductModal
                    form={form}
                    setForm={setForm}
                    categories={categories}
                    isEdit={!!editTarget}
                    isSaving={isSaving}
                    onSubmit={handleSubmit}
                    onClose={closeModal}
                />
            )}

            {/* Delete confirm */}
            {deleteId !== null && (
                <DeleteConfirm
                    onConfirm={() => deleteMutation.mutate(deleteId)}
                    onCancel={() => setDeleteId(null)}
                    isPending={deleteMutation.isPending}
                />
            )}
        </div>
    );
}

/* ══════════════════════════════════════════════════════════════
   PRODUCT MODAL (Create / Edit)
══════════════════════════════════════════════════════════════ */
function ProductModal({ form, setForm, categories, isEdit, isSaving, onSubmit, onClose }) {
    const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-foreground/40 backdrop-blur-sm">
            <div className="bg-card rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-5 border-b sticky top-0 bg-card rounded-t-2xl">
                    <h2 className="text-lg font-bold">{isEdit ? "Edit Product" : "New Product"}</h2>
                    <button
                        onClick={onClose}
                        className="p-1.5 rounded-lg hover:bg-secondary transition-colors text-muted-foreground"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={onSubmit} className="px-6 py-5 space-y-4">
                    {/* Name */}
                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">Name <span className="text-destructive">*</span></label>
                        <Input value={form.name} onChange={set("name")} placeholder="e.g. Street Art Tee" required minLength={3} />
                    </div>

                    {/* Description */}
                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">Description <span className="text-destructive">*</span></label>
                        <textarea
                            value={form.description}
                            onChange={set("description")}
                            placeholder="Describe the product…"
                            required
                            rows={3}
                            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 resize-none"
                        />
                    </div>

                    {/* Price + Stock row */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <label className="text-sm font-medium">Price (USD) <span className="text-destructive">*</span></label>
                            <Input
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={form.price}
                                onChange={set("price")}
                                placeholder="29.99"
                                required
                            />
                        </div>
                        <div className="space-y-1.5">
                            <label className="text-sm font-medium">Stock <span className="text-destructive">*</span></label>
                            <Input
                                type="number"
                                min="0"
                                step="1"
                                value={form.stockQuantity}
                                onChange={set("stockQuantity")}
                                placeholder="100"
                                required
                            />
                        </div>
                    </div>

                    {/* Brand + Category row */}
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1.5">
                            <label className="text-sm font-medium">Brand <span className="text-destructive">*</span></label>
                            <Input value={form.brand} onChange={set("brand")} placeholder="Funkart" required />
                        </div>
                        <div className="space-y-1.5">
                            <label className="text-sm font-medium">Category <span className="text-destructive">*</span></label>
                            <select
                                value={form.categoryId}
                                onChange={set("categoryId")}
                                required
                                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                            >
                                <option value="">Select…</option>
                                {(Array.isArray(categories) ? categories : []).map((c) => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Image URL */}
                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">Image URL</label>
                        <Input value={form.imageUrl} onChange={set("imageUrl")} placeholder="https://…" type="url" />
                    </div>

                    {/* Active toggle (edit only) */}
                    {isEdit && (
                        <div className="flex items-center gap-3 pt-1">
                            <input
                                type="checkbox"
                                id="active"
                                checked={form.active}
                                onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
                                className="w-4 h-4 rounded border-input"
                            />
                            <label htmlFor="active" className="text-sm font-medium">Active (visible in store)</label>
                        </div>
                    )}

                    {/* Footer */}
                    <div className="flex gap-3 pt-2">
                        <Button type="button" variant="outline" onClick={onClose} className="flex-1">
                            Cancel
                        </Button>
                        <Button type="submit" disabled={isSaving} className="flex-1 gap-2">
                            {isSaving && <Loader2 className="w-4 h-4 animate-spin" />}
                            {isEdit ? "Save Changes" : "Create Product"}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}

/* ══════════════════════════════════════════════════════════════
   DELETE CONFIRM DIALOG
══════════════════════════════════════════════════════════════ */
function DeleteConfirm({ onConfirm, onCancel, isPending }) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-foreground/40 backdrop-blur-sm">
            <div className="bg-card rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4">
                <div className="flex items-start gap-3">
                    <div className="w-10 h-10 rounded-full bg-destructive/10 flex items-center justify-center shrink-0">
                        <Trash2 className="w-5 h-5 text-destructive" />
                    </div>
                    <div>
                        <h3 className="font-bold">Delete product?</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                            This action is permanent and cannot be undone.
                        </p>
                    </div>
                </div>
                <div className="flex gap-3">
                    <Button variant="outline" onClick={onCancel} className="flex-1" disabled={isPending}>
                        Cancel
                    </Button>
                    <Button
                        variant="destructive"
                        onClick={onConfirm}
                        disabled={isPending}
                        className="flex-1 gap-2"
                    >
                        {isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                        Delete
                    </Button>
                </div>
            </div>
        </div>
    );
}