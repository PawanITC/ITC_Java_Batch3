package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.config.JwtConfig;
import com.itc.funkart.product_service.dto.jwt.JwtUserDto;
import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.kafka.producer.OrderProducer;
import com.itc.funkart.product_service.kafka.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.service.CartService;
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
 * <h2>CartServiceIntegrationTest</h2>
 * <p>
 * Validates the shopping cart lifecycle, ensuring business rules like item aggregation,
 * quantity adjustments, and user-based isolation are enforced.
 * </p>
 * <p>
 * Identity is handled via the {@code SecurityContext}, simulating a real-world
 * JWT-authenticated request flow.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration: Cart Service Workflows")
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtConfig jwtConfig;
    @MockitoBean
    private OrderProducer orderProducer;
    @MockitoBean
    private ProductProducer productProducer;

    /**
     * Helper to inject the required JwtUserDto into the SecurityContext.
     */
    private void mockAuth(Long userId) {
        JwtUserDto principal = JwtUserDto.builder()
                .id(userId)
                .name("IntegrationUser")
                .email("test@funkart.com")
                .role("ROLE_USER")
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private ProductResponse createProduct(String name) {
        Category category = categoryRepository.save(Category.builder()
                .name("Cat_" + System.nanoTime())
                .build());

        return productService.createProduct(ProductCreateRequest.builder()
                .name(name + "_" + System.nanoTime())
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(100)
                .categoryId(category.getId())
                .brand("TestBrand")
                .imageUrls(List.of())
                .build());
    }

    @Test
    @DisplayName("Workflow: Automatic cart initialization on first access")
    void shouldCreateCartOnFirstRequest() {
        Long userId = 1001L;
        mockAuth(userId);

        CartResponse cart = cartService.getCart();

        assertThat(cart).isNotNull();
        assertThat(cart.userId()).isEqualTo(userId);
        assertThat(cartRepository.findByUserIdWithItems(userId)).isPresent();
    }

    @Test
    @DisplayName("Workflow: Item aggregation for duplicate product additions")
    void shouldHandleItemAggregation() {
        mockAuth(1002L);
        ProductResponse product = createProduct("Monitor");

        cartService.addItemToCart(AddToCartRequest.builder()
                .productId(product.id()).quantity(1).build());

        CartResponse finalCart = cartService.addItemToCart(AddToCartRequest.builder()
                .productId(product.id()).quantity(2).build());

        assertThat(finalCart.items()).hasSize(1);
        assertThat(finalCart.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Workflow: Item removal when quantity drops to zero")
    void shouldRemoveItemOnNegativeUpdate() {
        mockAuth(1003L);
        ProductResponse product = createProduct("Keyboard");

        cartService.addItemToCart(AddToCartRequest.builder()
                .productId(product.id()).quantity(5).build());

        CartResponse updatedCart = cartService.updateItemQuantity(product.id(),
                CartItemUpdateDto.builder().quantityChange(-10).build());

        assertThat(updatedCart.items()).isEmpty();
    }

    @Test
    @DisplayName("End-to-End: Full journey from addition to checkout")
    void shouldHandleFullShoppingWorkflow() {
        mockAuth(1004L);
        ProductResponse p1 = createProduct("Mouse");
        ProductResponse p2 = createProduct("Pad");

        cartService.addItemToCart(AddToCartRequest.builder().productId(p1.id()).quantity(1).build());
        cartService.addItemToCart(AddToCartRequest.builder().productId(p2.id()).quantity(1).build());

        cartService.updateItemQuantity(p1.id(), CartItemUpdateDto.builder().quantityChange(2).build());

        cartService.checkout();

        entityManager.flush();
        entityManager.clear();

        assertThat(cartService.getCart().items()).isEmpty();
    }

    @Test
    @DisplayName("Security: Strict isolation between distinct users")
    void shouldMaintainIsolationBetweenUsers() {
        ProductResponse product = createProduct("Shared Product");

        // Setup User A
        mockAuth(2001L);
        cartService.addItemToCart(AddToCartRequest.builder().productId(product.id()).quantity(1).build());

        // Switch to User B
        mockAuth(2002L);
        cartService.addItemToCart(AddToCartRequest.builder().productId(product.id()).quantity(99).build());

        // Verify User B state
        assertThat(cartService.getCart().items().get(0).quantity()).isEqualTo(99);

        // Switch back to User A
        mockAuth(2001L);
        assertThat(cartService.getCart().items().get(0).quantity()).isEqualTo(1);
    }
}