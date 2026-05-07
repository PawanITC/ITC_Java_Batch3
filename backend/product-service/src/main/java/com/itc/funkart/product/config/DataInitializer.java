package com.itc.funkart.product.config;

import com.itc.funkart.product.entity.Category;
import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.entity.ProductImage;
import com.itc.funkart.product.repository.CategoryRepository;
import com.itc.funkart.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


/**
 * Seeds sample categories and products on first boot.
 * Uses picsum.photos deterministic seed URLs for realistic placeholder images.
 * Run: docker-compose down -v && docker-compose up --build  (to re-seed a fresh DB)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            // Patch broken via.placeholder.com URLs from previous seed runs
            patchBrokenImages();
            log.info("DataInitializer: existing data detected — skipping full seed (patched broken images if any).");
            return;
        }

        log.info("DataInitializer: seeding categories and products …");

        Category posters      = save(cat("Posters",      "Art prints and decorative posters"));
        Category collectibles = save(cat("Collectibles",  "Limited edition collectible figures"));
        Category apparel      = save(cat("Apparel",       "Branded clothing and accessories"));
        Category stationery   = save(cat("Stationery",    "Notebooks, pens, and desk accessories"));
        Category digital      = save(cat("Digital Art",   "High-res digital art downloads"));

        // ── Posters ────────────────────────────────────────────────────────
        product("Neon City Skyline Poster", "neon-city-skyline-poster",
                "Vibrant neon-lit cityscape print — perfect for modern interiors.",
                "24.99", 120, "Arthaus", posters,
                "https://picsum.photos/seed/neon-city/600/600",
                "https://picsum.photos/seed/neon-city-2/600/600");

        product("Abstract Geometry Print", "abstract-geometry-print",
                "Bold geometric shapes in a striking colour palette.",
                "19.99", 85, "Arthaus", posters,
                "https://picsum.photos/seed/abstract-geo/600/600");

        product("Vintage Travel Poster — Tokyo", "vintage-travel-tokyo",
                "Retro-style travel poster celebrating the spirit of Tokyo.",
                "29.99", 60, "RetroPress", posters,
                "https://picsum.photos/seed/vintage-tokyo/600/600",
                "https://picsum.photos/seed/vintage-tokyo-2/600/600");

        product("Minimalist Mountain Landscape", "minimalist-mountain-landscape",
                "Clean, minimal line-art of sweeping alpine peaks.",
                "22.99", 95, "NatureLines", posters,
                "https://picsum.photos/seed/mountain-minimal/600/600");

        // ── Collectibles ───────────────────────────────────────────────────
        product("Cosmic Dragon Resin Figure", "cosmic-dragon-resin-figure",
                "Hand-painted resin collectible — limited run of 500 units.",
                "89.99", 22, "LimitedEdge", collectibles,
                "https://picsum.photos/seed/dragon-figure/600/600",
                "https://picsum.photos/seed/dragon-figure-2/600/600");

        product("Retro Robot Enamel Pin Set", "retro-robot-enamel-pin-set",
                "Set of 4 hard-enamel robot pins with gold plating.",
                "14.99", 200, "PinMaster", collectibles,
                "https://picsum.photos/seed/robot-pins/600/600");

        product("Samurai Cat Vinyl Figure", "samurai-cat-vinyl-figure",
                "Adorable 15 cm vinyl figure of a samurai cat warrior.",
                "49.99", 38, "LimitedEdge", collectibles,
                "https://picsum.photos/seed/samurai-cat/600/600");

        // ── Apparel ────────────────────────────────────────────────────────
        product("Funkart Logo Tee — Black", "funkart-logo-tee-black",
                "Premium 100 % cotton unisex tee with embroidered Funkart logo.",
                "34.99", 150, "Funkart", apparel,
                "https://picsum.photos/seed/black-tee/600/600");

        product("Funkart Hoodie — Grey", "funkart-hoodie-grey",
                "Cozy heavyweight hoodie with kangaroo pocket.",
                "64.99", 75, "Funkart", apparel,
                "https://picsum.photos/seed/grey-hoodie/600/600",
                "https://picsum.photos/seed/grey-hoodie-2/600/600");

        product("Street Art Cap", "street-art-cap",
                "Adjustable snapback cap with embroidered street art motif.",
                "27.99", 90, "CapCo", apparel,
                "https://picsum.photos/seed/street-cap/600/600");

        // ── Stationery ─────────────────────────────────────────────────────
        product("Artist Hardcover Sketchbook A5", "artist-sketchbook-a5",
                "180-page cold-press paper sketchbook with elastic closure.",
                "18.99", 200, "PaperCraft", stationery,
                "https://picsum.photos/seed/sketchbook-a5/600/600");

        product("Metallic Fineliner Set (10 pens)", "metallic-fineliner-set-10",
                "10 metallic gel pens perfect for art journaling.",
                "12.99", 300, "PenPal", stationery,
                "https://picsum.photos/seed/metallic-pens/600/600");

        // ── Digital Art ────────────────────────────────────────────────────
        product("Cyberpunk City Pack (5 Wallpapers)", "cyberpunk-city-pack",
                "5 ultra-high-resolution cyberpunk wallpapers (4K, instant download).",
                "9.99", 9999, "PixelVault", digital,
                "https://picsum.photos/seed/cyberpunk-pack/600/600");

        product("Watercolour Botanicals Bundle", "watercolour-botanicals-bundle",
                "12 hand-painted botanical illustrations, PNG + SVG.",
                "15.99", 9999, "PixelVault", digital,
                "https://picsum.photos/seed/watercolour-botanicals/600/600");

        log.info("DataInitializer: seeding complete — {} categories, {} products.",
                categoryRepository.count(), productRepository.count());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Category cat(String name, String desc) {
        return Category.builder().name(name).description(desc).build();
    }

    private Category save(Category c) {
        return categoryRepository.findByName(c.getName()).orElseGet(() -> categoryRepository.save(c));
    }

    private void product(String name, String slug, String desc,
                         String price, int stock, String brand,
                         Category category, String... imageUrls) {
        Product p = Product.builder()
                .name(name).slug(slug).description(desc)
                .price(new BigDecimal(price))
                .stockQuantity(stock).brand(brand)
                .category(category).active(true)
                .build();

        for (int i = 0; i < imageUrls.length; i++) {
            p.addImage(ProductImage.builder()
                    .imageUrl(imageUrls[i])
                    .isPrimary(i == 0)
                    .build());
        }
        productRepository.save(p);
    }

    /**
     * Replaces broken via.placeholder.com URLs with working picsum equivalents
     * for databases seeded by the old initializer.
     */
    private void patchBrokenImages() {
        productRepository.findAll().forEach(product -> {
            boolean patched = false;
            for (ProductImage img : product.getImages()) {
                if (img.getImageUrl() != null && img.getImageUrl().contains("via.placeholder.com")) {
                    String slug = product.getSlug() != null ? product.getSlug() : String.valueOf(product.getId());
                    img.setImageUrl("https://picsum.photos/seed/" + slug + "/600/600");
                    patched = true;
                }
            }
            if (patched) {
                productRepository.save(product);
                log.info("DataInitializer: patched images for product '{}'", product.getName());
            }
        });
    }
}
