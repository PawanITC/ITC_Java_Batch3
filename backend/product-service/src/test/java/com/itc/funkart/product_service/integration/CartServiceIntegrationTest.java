package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

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

    @Autowired
    private ProductRepository productRepository;

    private Category createCategory() {
        return categoryRepository.save(Category.builder()
                .name("Test Category_" + System.nanoTime())
                .description("Test description")
                .build());
    }

    private ProductResponse createProduct(String name) {
        Category category = createCategory();
        return productService.createProduct(ProductCreateRequest.builder()
                .name(name + "_" + System.nanoTime()) // Make unique
                .description("Test product")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("TestBrand")
                .build());
    }

    @Test
    @DisplayName("Should create cart for user on first request")
    void shouldCreateCartForUserOnFirstRequest() {
        // Act
        CartResponse cart = cartService.getCartByUserId(1L);

        // Assert
        assertThat(cart).isNotNull();
        assertThat(cart.getUserId()).isEqualTo(1L);
        assertThat(cart.getItems()).isEmpty();

        // Verify in database
        Cart dbCart = cartRepository.findByUserId(1L).orElse(null);
        assertThat(dbCart).isNotNull();
        assertThat(dbCart.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get existing cart without creating new one")
    void shouldGetExistingCartWithoutCreatingNew() {
        // Arrange
        CartResponse firstCall = cartService.getCartByUserId(2L);

        // Act
        CartResponse secondCall = cartService.getCartByUserId(2L);

        // Assert
        assertThat(firstCall.getCartId()).isEqualTo(secondCall.getCartId());
    }

    @Test
    @DisplayName("Should add single item to cart and persist to database")
    void shouldAddSingleItemToCartAndPersist() {
        // Arrange
        ProductResponse product = createProduct("Laptop");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        // Act
        CartResponse cart = cartService.addItemToCart(3L, request);

        // Assert
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProductId()).isEqualTo(product.getId());
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(1);

        // Verify in database
        Cart dbCart = cartRepository.findByUserId(3L).orElseThrow();
        assertThat(dbCart.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should add multiple different items to cart")
    void shouldAddMultipleDifferentItemsToCart() {
        // Arrange
        ProductResponse product1 = createProduct("Phone");
        ProductResponse product2 = createProduct("Tablet");

        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product1.getId());
        request1.setQuantity(1);

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product2.getId());
        request2.setQuantity(2);

        // Act
        cartService.addItemToCart(4L, request1);
        CartResponse cart = cartService.addItemToCart(4L, request2);

        // Assert
        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Should increase quantity when adding same item again")
    void shouldIncreaseQuantityWhenAddingSameItemAgain() {
        // Arrange
        ProductResponse product = createProduct("Monitor");
        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product.getId());
        request1.setQuantity(1);

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product.getId());
        request2.setQuantity(2);

        // Act
        cartService.addItemToCart(5L, request1);
        CartResponse cart = cartService.addItemToCart(5L, request2);

        // Assert
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3); // 1 + 2
    }

    @Test
    @DisplayName("Should remove item from cart")
    void shouldRemoveItemFromCart() {
        // Arrange
        ProductResponse product = createProduct("Keyboard");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        cartService.addItemToCart(6L, request);

        // Act
        CartResponse cart = cartService.removeItemsFromCart(6L, product.getId());

        // Assert
        assertThat(cart.getItems()).isEmpty();

        // Verify in database
        Cart dbCart = cartRepository.findByUserId(6L).orElseThrow();
        assertThat(dbCart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should update item quantity")
    void shouldUpdateItemQuantity() {
        // Arrange
        ProductResponse product = createProduct("Mouse");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(2);

        cartService.addItemToCart(7L, request);

        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(3); // Add 3 more

        // Act
        CartResponse cart = cartService.updateItemQuantity(7L, product.getId(), updateDto);

        // Assert
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    @DisplayName("Should remove item when quantity becomes zero")
    void shouldRemoveItemWhenQuantityBecomesZero() {
        // Arrange
        ProductResponse product = createProduct("Headphones");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(2);

        cartService.addItemToCart(8L, request);

        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(-2); // Subtract 2

        // Act
        CartResponse cart = cartService.updateItemQuantity(8L, product.getId(), updateDto);

        // Assert
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should clear all items from cart")
    void shouldClearAllItemsFromCart() {
        // Arrange
        ProductResponse product1 = createProduct("Speaker");
        ProductResponse product2 = createProduct("Charger");

        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product1.getId());
        request1.setQuantity(1);

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product2.getId());
        request2.setQuantity(1);

        cartService.addItemToCart(9L, request1);
        cartService.addItemToCart(9L, request2);

        // Act
        cartService.clearCart(9L);

        // Assert
        CartResponse cart = cartService.getCartByUserId(9L);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should complete checkout flow")
    void shouldCompleteCheckoutFlow() {
        // Arrange
        ProductResponse product = createProduct("Laptop Pro");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        cartService.addItemToCart(10L, request);

        // Act
        cartService.checkout(10L);

        // Assert
        CartResponse cart = cartService.getCartByUserId(10L);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should handle shopping workflow end-to-end")
    void shouldHandleShoppingWorkflowEndToEnd() {
        // Arrange - User starts shopping
        Long userId = 11L;
        CartResponse initialCart = cartService.getCartByUserId(userId);
        assertThat(initialCart.getItems()).isEmpty();

        // Act 1 - Add first item
        ProductResponse product1 = createProduct("Laptop");
        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product1.getId());
        request1.setQuantity(1);
        cartService.addItemToCart(userId, request1);

        // Act 2 - Add second item
        ProductResponse product2 = createProduct("Monitor");
        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product2.getId());
        request2.setQuantity(2);
        cartService.addItemToCart(userId, request2);

        // Act 3 - Update quantity
        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(1);
        cartService.updateItemQuantity(userId, product1.getId(), updateDto);

        // Act 4 - Checkout
        CartResponse beforeCheckout = cartService.getCartByUserId(userId);
        assertThat(beforeCheckout.getItems()).hasSize(2);

        cartService.checkout(userId);

        // Assert - Cart is empty after checkout
        CartResponse afterCheckout = cartService.getCartByUserId(userId);
        assertThat(afterCheckout.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple users with separate carts")
    void shouldHandleMultipleUsersWithSeparateCarts() {
        // Arrange
        ProductResponse product = createProduct("Wireless Mouse");
        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product.getId());
        request1.setQuantity(1);

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product.getId());
        request2.setQuantity(3);

        // Act
        cartService.addItemToCart(100L, request1);
        cartService.addItemToCart(101L, request2);

        CartResponse user1Cart = cartService.getCartByUserId(100L);
        CartResponse user2Cart = cartService.getCartByUserId(101L);

        // Assert
        assertThat(user1Cart.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(user2Cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @Transactional
    @DisplayName("Should verify cart persistence across requests")
    void shouldVerifyCartPersistenceAcrossRequests() {
        // Arrange
        Long userId = 12L;
        ProductResponse product = createProduct("USB Cable");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(5);

        // Act 1 - Add item
        cartService.addItemToCart(userId, request);

        // Act 2 - Verify persistence in database
        Cart dbCart = cartRepository.findByUserId(userId).orElseThrow();

        // Assert
        assertThat(dbCart.getItems()).hasSize(1);
        assertThat(dbCart.getItems().get(0).getProduct().getId()).isEqualTo(product.getId());
        assertThat(dbCart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle cart with maximum items")
    void shouldHandleCartWithMaximumItems() {
        // Arrange
        Long userId = 13L;
        AddToCartRequest request = new AddToCartRequest();
        request.setQuantity(1);

        // Act - Add 10 different items
        for (int i = 0; i < 10; i++) {
            ProductResponse product = createProduct("Product " + i);
            request.setProductId(product.getId());
            cartService.addItemToCart(userId, request);
        }

        // Assert
        CartResponse cart = cartService.getCartByUserId(userId);
        assertThat(cart.getItems()).hasSize(10);
    }
}

