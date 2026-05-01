package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.config.JwtConfig;
import com.itc.funkart.product_service.dto.jwt.JwtUserDto;
import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.kafka.producer.OrderProducer;
import com.itc.funkart.product_service.kafka.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.CategoryService;
import com.itc.funkart.product_service.service.JwtService;
import com.itc.funkart.product_service.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>Cross-Feature Integration Tests</h2>
 * <p>
 * Validates the deep integration between Product, Category, and Cart features.
 * This ensures that JPA mappings and business logic hold up across multiple service boundaries.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration: Cross-Feature Workflows")
class CrossFeatureIntegrationTest {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartService cartService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private OrderProducer orderProducer;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ProductProducer productProducer;
    @MockitoBean
    private JwtConfig jwtConfig;

    private void mockAuth(Long userId) {
        JwtUserDto principal = JwtUserDto.builder()
                .id(userId).name("CrossTestUser").email("cross@test.com").role("ROLE_USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    @DisplayName("Workflow: End-to-End User Journey (Catalog -> Cart -> Checkout)")
    void shouldCompleteFullECommerceWorkflow() {
        mockAuth(1000L);

        // 1. Catalog Setup
        var category = categoryService.createCategory(CategoryRequest.builder()
                .name("Electronics_" + System.nanoTime()).build());

        ProductResponse laptop = productService.createProduct(ProductCreateRequest.builder()
                .name("Laptop").price(BigDecimal.valueOf(999.99)).categoryId(category.id())
                .stockQuantity(5).brand("Dell").imageUrls(List.of()).build());

        // 2. Shopping Actions
        cartService.addItemToCart(AddToCartRequest.builder().productId(laptop.id()).quantity(1).build());

        entityManager.flush();
        entityManager.clear();

        // 3. Verification & Checkout
        assertThat(cartService.getCart().items()).hasSize(1);
        cartService.checkout();

        entityManager.flush();
        entityManager.clear();

        assertThat(cartService.getCart().items()).isEmpty();
    }

    @Test
    @DisplayName("Consistency: Verify DB Object Graph (Cart -> Product -> Category)")
    void shouldVerifyDataConsistency() {
        Long userId = 5000L;
        mockAuth(userId);

        var cat = categoryService.createCategory(CategoryRequest.builder().name("Consistency").build());
        var prod = productService.createProduct(ProductCreateRequest.builder()
                .name("Audit-Item").categoryId(cat.id()).price(BigDecimal.TEN)
                .brand("Generic").imageUrls(List.of()).build());

        cartService.addItemToCart(AddToCartRequest.builder().productId(prod.id()).quantity(5).build());

        entityManager.flush();
        entityManager.clear();

        // Deep verification of the JPA Object Graph
        var cartInDb = cartRepository.findByUserIdWithItems(userId).orElseThrow();
        var item = cartInDb.getItems().get(0);
        assertThat(item.getProduct().getCategory().getName()).isEqualTo("Consistency");
    }
}