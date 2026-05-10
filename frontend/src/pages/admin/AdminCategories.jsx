import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Tag, Plus, Trash2, Loader2 } from "lucide-react";
import { adminCategoryApi } from "@/lib/adminApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";

const CATS_KEY = ["admin", "categories"];

export default function AdminCategories() {
    const qc = useQueryClient();
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [deleting, setDeleting] = useState(null);

    const { data: categories, isLoading } = useQuery({
        queryKey: CATS_KEY,
        queryFn: () => adminCategoryApi.getAllCategories().then((r) => r?.data ?? r),
    });

    const createMutation = useMutation({
        mutationFn: () => adminCategoryApi.createCategory({ name, description }),
        onSuccess: () => { qc.invalidateQueries({ queryKey: CATS_KEY }); setName(""); setDescription(""); },
    });

    const deleteMutation = useMutation({
        mutationFn: (id) => adminCategoryApi.deleteCategory(id),
        onSuccess: () => qc.invalidateQueries({ queryKey: CATS_KEY }),
        onSettled: () => setDeleting(null),
    });

    const handleCreate = (e) => {
        e.preventDefault();
        if (!name.trim()) return;
        createMutation.mutate();
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-3">
                <Tag className="w-6 h-6" />
                <h2 className="text-xl font-semibold">Categories</h2>
                {categories && <Badge variant="secondary">{categories.length}</Badge>}
            </div>

            {/* Create form */}
            <form onSubmit={handleCreate} className="bg-card border rounded-xl p-5 space-y-3">
                <h3 className="font-medium text-sm">New Category</h3>
                <div className="flex gap-3 flex-wrap">
                    <Input
                        placeholder="Category name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="flex-1 min-w-[160px]"
                        required
                    />
                    <Input
                        placeholder="Description (optional)"
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        className="flex-1 min-w-[200px]"
                    />
                    <Button type="submit" disabled={createMutation.isPending} className="gap-2 shrink-0">
                        {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                        Add
                    </Button>
                </div>
            </form>

            {/* List */}
            {isLoading && <div className="flex justify-center py-8"><Loader2 className="w-5 h-5 animate-spin text-muted-foreground" /></div>}

            {categories?.length > 0 && (
                <div className="bg-card border rounded-xl divide-y">
                    {categories.map((cat) => (
                        <div key={cat.id} className="flex items-center justify-between px-4 py-3">
                            <div>
                                <p className="font-medium text-sm">{cat.name}</p>
                                {cat.description && <p className="text-xs text-muted-foreground mt-0.5">{cat.description}</p>}
                            </div>
                            <button
                                onClick={() => { setDeleting(cat.id); deleteMutation.mutate(cat.id); }}
                                disabled={deleteMutation.isPending && deleting === cat.id}
                                className="text-destructive hover:text-destructive/70 transition-colors p-1.5 rounded"
                            >
                                {deleteMutation.isPending && deleting === cat.id
                                    ? <Loader2 className="w-4 h-4 animate-spin" />
                                    : <Trash2 className="w-4 h-4" />
                                }
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}