package com.itc.catalogueservice.service;

import com.itc.catalogueservice.dto.ProductDTO;
import com.itc.catalogueservice.client.ProductApiClient;
import com.itc.catalogueservice.exception.catalogue.NoProductsException;
import com.itc.catalogueservice.exception.catalogue.NoTopSellingProductsException;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.annotation.PostConstruct;


import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class CatalogueService {
    private final ProductApiClient productApiClient;
    private final StringRedisTemplate redisTemplate;


    //Service to return products
    public CatalogueService(ProductApiClient productApiClient, StringRedisTemplate redisTemplate){

        this.productApiClient = productApiClient;
        this.redisTemplate = redisTemplate;
    }

    public CompletableFuture<List<ProductDTO>> getProducts(
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

    //Service to return Top Selling Products
    public CompletableFuture<List<ProductDTO>> getTopSellingProducts(
            Integer limit, String category) {
        String key = getRedisKey(category);
        List<String> productIds = getTopProductIdsFromRedis(key, limit);

        if (productIds != null && !productIds.isEmpty()) {
            List<ProductDTO> products = productIds.stream()
                    .map(this::getProductFromCache)
                    .filter(p -> p != null)
                    .toList();

            if (products.isEmpty()) {
                throw new NoTopSellingProductsException();
            }

            return CompletableFuture.completedFuture(products);
            //return productApiClient.getProductsByIds(productIds);
        }


        //Fallback to get products and give default value
        return productApiClient.getProducts()
                .thenApply(products -> {
                    //Try using a stream API to understand what's going on
                    List<ProductDTO> result = products.stream()
                            .filter(p -> category == null ||
                                    (p.getCategory() != null && p.getCategory().equalsIgnoreCase(category)))
                            .limit(limit)
                            .toList();

                    if (result.isEmpty()) {
                        throw new NoTopSellingProductsException();
                    }

                    return result;
                });
    }



    //Method for returning the key for either global or with category
    private String getRedisKey(String category) {
        if (category == null || category.isBlank()) {
            return "top_selling:global";
        }
        return "top_selling:category:" + category.toLowerCase();
    }

    //
    private List<String> getTopProductIdsFromRedis(String key, int limit) {
        return redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1)
                .stream()
                .toList();
    }

    //Method to save ProductDTO to Redis cache
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

    //Method to initially load into Redis cache by calling saveProductToCache
    public void loadProductsToCache() {
        productApiClient.getProducts()
                .thenAccept(products -> {
                    products.forEach(this::saveProductToCache);
                });
    }

    // Runs once when the application starts.
    // Loads all products from Product Service and stores them in Redis cache
    // so that product data can be retrieved locally without calling the service.
    @PostConstruct
    public void init() {
        loadProductsToCache();
    }


    // Retrieve product from Redis HASH and map to ProductDTO
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

    // Increment product sales score in Redis ZSET (top-selling ranking)
    public void handleOrderEvent(String productId, int quantitySold, String category) {
        // Always update global
        redisTemplate.opsForZSet()
                .incrementScore("top_selling:global", productId, quantitySold);

        // Update category-specific if category is provided
        if (category != null && !category.isBlank()) {
            String categoryKey = "top_selling:category:" + category.toLowerCase();
            redisTemplate.opsForZSet()
                    .incrementScore(categoryKey, productId, quantitySold);
        }
    }

}