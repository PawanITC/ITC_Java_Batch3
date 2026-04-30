package com.itc.catalogueservice.service;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.client.ProductApiClient;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CatalogueService {

    private final ProductApiClient productApiClient;
    private final StringRedisTemplate redisTemplate;

    public CatalogueService(ProductApiClient productApiClient, StringRedisTemplate redisTemplate){
        this.productApiClient = productApiClient;
        this.redisTemplate = redisTemplate;
    }

    public CompletableFuture<List<ProductDTO>> getProducts(String q,
                                                           Integer page, Integer size, String category,
                                                           BigDecimal minPrice, BigDecimal maxPrice, Double rating) {

        return productApiClient.getProducts()
                .thenApply(products -> {
                    if (products.isEmpty()) {
                        throw new NoProductsException();
                    }
                    return products;
                });
    }

    public CompletableFuture<List<ProductDTO>> getTopSellingProducts(
            Integer limit, String category) {

        String key = getRedisKey(category);
        List<String> productIds = getTopProductIdsFromRedis(key, limit);

        if (productIds != null && !productIds.isEmpty()) {
            List<ProductDTO> products = productIds.stream()
                    .map(this::getProductFromCache)
                    .filter(p -> p != null)
                    .toList();

            if (!products.isEmpty()) {
                return CompletableFuture.completedFuture(products);
            }
        }

        return productApiClient.getProducts()
                .thenApply(products -> products.stream()
                        .filter(p -> category == null ||
                                (p.getCategory() != null &&
                                        p.getCategory().equalsIgnoreCase(category)))
                        .limit(limit)
                        .toList());
    }

    private String getRedisKey(String category) {
        if (category == null || category.isBlank()) {
            return "top_selling:global";
        }
        return "top_selling:category:" + category.toLowerCase();
    }

    private List<String> getTopProductIdsFromRedis(String key, int limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1)
                .stream()
                .toList();
    }

    public void saveProductToCache(ProductDTO product) {
        String key = "product:" + product.getId();
        redisTemplate.opsForHash().put(key, "id", product.getId().toString());
        redisTemplate.opsForHash().put(key, "name", product.getName());
        redisTemplate.opsForHash().put(key, "description", product.getDescription());
        redisTemplate.opsForHash().put(key, "price", product.getPrice().toString());
        redisTemplate.opsForHash().put(key, "imageUrl", product.getImageUrl());
        redisTemplate.opsForHash().put(key, "rating", String.valueOf(product.getRating()));
        redisTemplate.opsForHash().put(key, "quantity", String.valueOf(product.getQuantity()));
        redisTemplate.opsForHash().put(key, "category", product.getCategory());
    }

    public void deleteProductFromCache(String productId, String category) {
        String key = "product:" + productId;
        redisTemplate.delete(key);

        redisTemplate.opsForZSet().remove("top_selling:global", productId);

        if (category != null && !category.isBlank()) {
            String categoryKey = "top_selling:category:" + category.toLowerCase();
            redisTemplate.opsForZSet().remove(categoryKey, productId);
        }
    }

    public void loadProductsToCache() {
        productApiClient.getProducts()
                .thenAccept(products -> products.forEach(this::saveProductToCache));
    }

    @PostConstruct
    public void init() {
        loadProductsToCache();
    }

    public ProductDTO getProductFromCache(String productId) {
        String key = "product:" + productId;

        var map = redisTemplate.opsForHash().entries(key);

        if (map.isEmpty()) {
            return null;
        }

        return new ProductDTO(
                Long.valueOf((String) map.get("id")),
                (String) map.get("name"),
                (String) map.get("description"),
                new BigDecimal((String) map.get("price")),
                (String) map.get("imageUrl"),
                Double.valueOf((String) map.get("rating")),
                Integer.parseInt((String) map.get("quantity")),
                (String) map.get("category")
        );
    }

    // 🔥 FIXED METHOD
    public void handleOrderEvent(String productId, int quantitySold, String category) {

        // ensure product exists BEFORE ranking
        if (getProductFromCache(productId) == null) {
            List<ProductDTO> products = productApiClient.getProducts().join();

            products.stream()
                    .filter(p -> p.getId().toString().equals(productId))
                    .findFirst()
                    .ifPresent(this::saveProductToCache);
        }

        redisTemplate.opsForZSet()
                .incrementScore("top_selling:global", productId, quantitySold);

        if (category != null && !category.isBlank()) {
            String categoryKey = "top_selling:category:" + category.toLowerCase();
            redisTemplate.opsForZSet()
                    .incrementScore(categoryKey, productId, quantitySold);
        }
    }
}