package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.dto.response.ProductsResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.enums.ProductEventType;
import com.itc.funkart.product_service.exceptions.BadRequestException;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.ProductMapper;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ProductService}.
 * Handles business logic for product management, caching with Redis,
 * and event publishing via Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int MAX_IDS = 2000;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductProducer productProducer;

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating product with name: {}", request.name()); // Fixed: record syntax

        Product product = ProductMapper.toEntity(request);

        if (request.categoryId() != null) { // Fixed: record syntax
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);

        productProducer.sendMessage(ProductMapper.toEvent(savedProduct, ProductEventType.CREATE, savedProduct.getId()));
        return ProductMapper.toResponse(savedProduct);
    }

    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Partial update logic using Record accessors
        if (request.name() != null) {
            product.setName(request.name());
            product.setSlug(request.name().toLowerCase().replace(" ", "-"));
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.stockQuantity() != null) product.setStockQuantity(request.stockQuantity());
        if (request.active() != null) product.setActive(request.active());
        if (request.brand() != null) product.setBrand(request.brand());

        Product updatedProduct = productRepository.save(product);
        productProducer.sendMessage(ProductMapper.toEvent(updatedProduct, ProductEventType.UPDATE, updatedProduct.getId()));
        return ProductMapper.toResponse(updatedProduct);
    }


    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(ProductMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product-service:products")
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products from database");
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ProductMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        // Sending null for the product object on DELETE is standard
        productProducer.sendMessage(ProductMapper.toEvent(null, ProductEventType.DELETE, id));
    }

    @Override
    public ProductsResponse getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("ID list cannot be empty");
        }

        if (ids.size() > MAX_IDS) {
            throw new BadRequestException("Too many IDs requested. Max allowed: " + MAX_IDS);
        }

        List<Long> uniqueIds = ids.stream().distinct().toList();
        List<Product> products = productRepository.findAllById(uniqueIds);

        Set<Long> foundIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = uniqueIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        // Fixed: Records must use the constructor, they don't have setters
        return ProductsResponse.builder()
                .found(products)
                .missing(missingIds)
                .build();
    }
}