package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.response.CartResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.producer.OrderProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>CartServiceImplTest</h2>
 * <p>
 * Validates complex cart logic including aggregate total calculations,
 * quantity adjustments, and Kafka event publishing on checkout.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Service Unit Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private CartServiceImpl cartService;

    // --- Helpers using updated Entity/Record patterns ---

    private Cart createCart(Long userId) {
        return Cart.builder()
                .id(1L)
                .userId(userId)
                .items(new ArrayList<>())
                .build();
    }

    private Product createProduct(Long id, String name) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(BigDecimal.valueOf(100.00))
                .build();
    }

    private CartItem createCartItem(Cart cart, Product product, int quantity) {
        return CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build();
    }

    // --- 1. ADD ITEM BRANCHES ---

    @Test
    @DisplayName("Add Item - Should increase quantity for existing product")
    void shouldIncreaseQuantityForExistingItem() {
        Cart cart = createCart(1L);
        Product product = createProduct(101L, "Laptop");
        cart.getItems().add(createCartItem(cart, product, 2));

        // Using Builder for Record
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(101L)
                .quantity(3)
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArguments()[0]);

        CartResponse response = cartService.addItemToCart(1L, request);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        // Record accessor syntax
        assertThat(response.userId()).isEqualTo(1L);
    }

    // --- 2. UPDATE QUANTITY BRANCHES ---

    @Test
    @DisplayName("Update Quantity - Should remove item if quantity drops <= 0 (Branch: Removal)")
    void shouldRemoveItemWhenQuantityBecomesZero() {
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Phone");
        cart.getItems().add(createCartItem(cart, product, 1));

        // Using Builder for Record
        CartItemUpdateDto updateDto = CartItemUpdateDto.builder().quantityChange(-1).build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.updateItemQuantity(1L, 1L, updateDto);

        assertThat(cart.getItems()).isEmpty();
    }

    // --- 3. CHECKOUT BRANCHES ---

    @Test
    @DisplayName("Checkout - Should clear cart and publish Kafka event (Happy Path)")
    void shouldPublishOrderEventOnCheckout() {
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Widget");
        cart.getItems().add(createCartItem(cart, product, 2));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.checkout(1L);

        assertThat(cart.getItems()).isEmpty();
        // Verify cross-service communication
        verify(orderProducer, times(1)).sendOrderEvent(any());
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("Checkout - Should throw exception (Branch: Empty Cart)")
    void shouldThrowExceptionForEmptyCartCheckout() {
        Cart cart = createCart(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.checkout(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty cart");

        verify(orderProducer, never()).sendOrderEvent(any());
    }

    // --- 4. PERSISTENCE BRANCHES ---

    @Test
    @DisplayName("Get Cart - Should auto-create cart (Branch: Missing Cart)")
    void shouldCreateNewCartIfNotFound() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArguments()[0]);

        CartResponse response = cartService.getCartByUserId(10L);

        assertThat(response.userId()).isEqualTo(10L);
        verify(cartRepository).save(any(Cart.class));
    }
}