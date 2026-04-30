package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.producer.ProductProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.JwtService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>CartServiceIntegrationTest</h2>
 * <p>
 * This suite validates the full lifecycle of a shopping cart, ensuring that
 * business rules like item aggregation, quantity updates, and cart isolation
 * work correctly when interacting with the real database schema.
 * </p>
 * <p>
 * Infrastructure dependencies like Kafka and JWT verification are mocked to ensure
 * the tests remain focused on the service and repository layer interactions.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cart Service Integration Tests")
class CartServiceIntegrationTest {

    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    // --- Mocks to prevent infrastructure hangs ---
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private OrderProducer orderProducer;
    @MockitoBean
    private ProductProducer productProducer;

    // --- Helpers using Records & Builders ---

    /**
     * Persists a new category to provide a valid relationship for product creation.
     */
    private Category createCategory() {
        return categoryRepository.save(Category.builder()
                .name("Category_" + System.nanoTime())
                .description("Integration Test Category")
                .build());
    }

    /**
     * Creates a product with unique identifiers to avoid collision during parallel runs.
     * Includes the required 'imageUrls' list for the Record constructor.
     */
    private ProductResponse createProduct(String name) {
        Category category = createCategory();
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name(name + "_" + System.nanoTime())
                .description("Description")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("Brand")
                .imageUrls(List.of()) // Vital for record instantiation
                .build();
        return productService.createProduct(request);
    }

    // --- Tests ---

    /**
     * Verifies the "Lazy Cart Creation" logic where a user record is initialized
     * in the database the moment they first access their cart.
     */
    @Test
    @DisplayName("Workflow - Should create cart automatically on first user access")
    void shouldCreateCartOnFirstRequest() {
        Long userId = 1001L;

        CartResponse cart = cartService.getCartByUserId(userId);

        assertThat(cart).isNotNull();
        assertThat(cart.userId()).isEqualTo(userId);
        assertThat(cart.items()).isEmpty();

        assertThat(cartRepository.findByUserId(userId)).isPresent();
    }

    /**
     * Tests the logic that merges multiple additions of the same product into
     * a single line item with an aggregated quantity.
     */
    @Test
    @DisplayName("Workflow - Should add items and aggregate quantities correctly")
    void shouldHandleItemAggregation() {
        Long userId = 1002L;
        ProductResponse product = createProduct("Monitor");

        // Add first time
        cartService.addItemToCart(userId, AddToCartRequest.builder()
                .productId(product.id())
                .quantity(1)
                .build());

        // Add second time (same product)
        CartResponse finalCart = cartService.addItemToCart(userId, AddToCartRequest.builder()
                .productId(product.id())
                .quantity(2)
                .build());

        assertThat(finalCart.items()).hasSize(1);
        assertThat(finalCart.items().get(0).quantity()).isEqualTo(3);
    }

    /**
     * Ensures that if an update results in zero or negative quantity, the
     * business logic removes the item from the cart entirely.
     */
    @Test
    @DisplayName("Workflow - Should remove item when quantity update results in <= 0")
    void shouldRemoveItemOnNegativeUpdate() {
        Long userId = 1003L;
        ProductResponse product = createProduct("Keyboard");

        cartService.addItemToCart(userId, AddToCartRequest.builder()
                .productId(product.id())
                .quantity(5)
                .build());

        CartResponse updatedCart = cartService.updateItemQuantity(userId, product.id(),
                CartItemUpdateDto.builder().quantityChange(-10).build());

        assertThat(updatedCart.items()).isEmpty();
    }

    /**
     * Comprehensive end-to-end test simulating a real user behavior:
     * searching, adding, adjusting, and finally checking out.
     */
    @Test
    @DisplayName("End-to-End - Full Shopping Journey: Add -> Update -> Checkout")
    void shouldHandleFullShoppingWorkflow() {
        Long userId = 1004L;
        ProductResponse p1 = createProduct("Mouse");
        ProductResponse p2 = createProduct("Pad");

        // 1. Add items
        cartService.addItemToCart(userId, AddToCartRequest.builder().productId(p1.id()).quantity(1).build());
        cartService.addItemToCart(userId, AddToCartRequest.builder().productId(p2.id()).quantity(1).build());

        // 2. Update quantity of Mouse
        cartService.updateItemQuantity(userId, p1.id(), CartItemUpdateDto.builder().quantityChange(2).build());

        // 3. Verify state before checkout
        CartResponse midCart = cartService.getCartByUserId(userId);
        assertThat(midCart.items()).hasSize(2);

        // 4. Checkout
        cartService.checkout(userId);

        // 5. Final Assertion (Cart should be wiped clean)
        CartResponse finalCart = cartService.getCartByUserId(userId);
        assertThat(finalCart.items()).isEmpty();
    }

    /**
     * Tests Cart Isolation. This ensures that user data is correctly partitioned
     * and that User A cannot see or modify User B's items.
     */
    @Test
    @DisplayName("Concurrency/Separation - Users should have isolated carts")
    void shouldMaintainIsolationBetweenUsers() {
        Long userA = 2001L;
        Long userB = 2002L;
        ProductResponse product = createProduct("Shared Product");

        cartService.addItemToCart(userA, AddToCartRequest.builder().productId(product.id()).quantity(1).build());
        cartService.addItemToCart(userB, AddToCartRequest.builder().productId(product.id()).quantity(99).build());

        assertThat(cartService.getCartByUserId(userA).items().get(0).quantity()).isEqualTo(1);
        assertThat(cartService.getCartByUserId(userB).items().get(0).quantity()).isEqualTo(99);
    }
}