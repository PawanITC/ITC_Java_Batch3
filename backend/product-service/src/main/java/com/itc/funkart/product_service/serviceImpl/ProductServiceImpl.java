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
import com.itc.funkart.product_service.kafka.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductProducer productProducer;

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating product: {}", request.name());

        Product product = ProductMapper.toEntity(request);

        if (request.categoryId() != null) {
            product.setCategory(fetchCategoryProxy(request.categoryId()));
        }

        Product savedProduct = productRepository.save(product);
        registerEvent(savedProduct, ProductEventType.CREATE, savedProduct.getId());

        return ProductMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        applyPartialUpdates(product, request);
        Product updatedProduct = productRepository.save(product);

        registerEvent(updatedProduct, ProductEventType.UPDATE, updatedProduct.getId());
        return ProductMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return productRepository.findById(id)
                .map(ProductMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product-service:products")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllWithImages().stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        productRepository.delete(product);
        registerEvent(null, ProductEventType.DELETE, id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductsResponse getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("ID list cannot be empty");
        }

        List<Long> uniqueIds = ids.stream().distinct().toList();
        List<Product> products = productRepository.findAllById(uniqueIds);

        Set<Long> foundIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = uniqueIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        return ProductsResponse.builder()
                .found(products.stream().map(ProductMapper::toResponse).toList())
                .missing(missingIds)
                .build();
    }

    private void applyPartialUpdates(Product product, ProductUpdateRequest request) {
        if (request.name() != null) {
            product.setName(request.name());
            product.setSlug(request.name().toLowerCase().replace(" ", "-"));
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.stockQuantity() != null) product.setStockQuantity(request.stockQuantity());
        if (request.active() != null) product.setActive(request.active());
        if (request.brand() != null) product.setBrand(request.brand());
        if (request.categoryId() != null) {
            product.setCategory(fetchCategoryProxy(request.categoryId()));
        }
    }

    private Category fetchCategoryProxy(Long categoryId) {
        try {
            // Optimization: getReferenceById creates a Proxy (no SELECT query)
            return categoryRepository.getReferenceById(categoryId);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
    }

    private void registerEvent(Product product, ProductEventType type, Long id) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    productProducer.sendMessage(ProductMapper.toEvent(product, type, id));
                }
            });
        }
    }
}