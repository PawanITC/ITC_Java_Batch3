package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.request.ProductUpdateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.ProductMapper;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating product with name: {}", request.getName());

        Product product = ProductMapper.toEntity(request);

        // Fetch and set Category
        if (request.getCategoryId() != null) {
            Category category = (Category) categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product savedProduct = productRepository.save(product);

        return ProductMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product-service:products", allEntries = true)
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        log.info("Updating product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
            product.setSlug(request.getName().toLowerCase().replace(" ", "-"));
        }
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getBrand() != null) product.setBrand(request.getBrand());

        productRepository.save(product);
        return ProductMapper.toResponse(product);
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
    }
}
