package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.dto.request.CartResponse;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.exceptions.ResourceNotFoundException;
import com.itc.funkart.product_service.mapper.CartMapper;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private CartServiceImpl cartService;

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
                .slug(name.toLowerCase().replace(" ", "-"))
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .brand("TestBrand")
                .build();
    }

    private CartItem createCartItem(Cart cart, Product product, Integer quantity) {
        return CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .build();
    }

    @Test
    @DisplayName("Should get existing cart by userId")
    void shouldGetExistingCartByUserId() {
        // Arrange
        Cart cart = createCart(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // Act
        CartResponse response = cartService.getCartByUserId(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);

        verify(cartRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Should create cart if it doesn't exist")
    void shouldCreateCartIfItDoesNotExist() {
        // Arrange
        Cart newCart = createCart(2L);
        when(cartRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        // Act
        CartResponse response = cartService.getCartByUserId(2L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(2L);

        verify(cartRepository, times(1)).findByUserId(2L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should add item to cart")
    void shouldAddItemToCart() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.addItemToCart(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(productRepository, times(1)).findById(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should increase quantity if item already in cart")
    void shouldIncreaseQuantityIfItemAlreadyInCart() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        CartItem existingItem = createCartItem(cart, product, 2);
        cart.getItems().add(existingItem);

        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.addItemToCart(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(existingItem.getQuantity()).isEqualTo(5); // 2 + 3

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(productRepository, times(1)).findById(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found during add to cart")
    void shouldThrowExceptionWhenProductNotFoundDuringAddToCart() {
        // Arrange
        Cart cart = createCart(1L);
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(999L);
        request.setQuantity(1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(productRepository, times(1)).findById(999L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should remove item from cart by product id")
    void shouldRemoveItemFromCart() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        CartItem item = createCartItem(cart, product, 2);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.removeItemsFromCart(1L, 1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(cart.getItems()).isEmpty();

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should clear cart")
    void shouldClearCart() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        CartItem item = createCartItem(cart, product, 2);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.clearCart(1L);

        // Assert
        assertThat(cart.getItems()).isEmpty();

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should update item quantity in cart")
    void shouldUpdateItemQuantityInCart() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        CartItem item = createCartItem(cart, product, 2);
        cart.getItems().add(item);

        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(3);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.updateItemQuantity(1L, 1L, updateDto);

        // Assert
        assertThat(response).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(5); // 2 + 3

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should remove item if quantity becomes zero or negative")
    void shouldRemoveItemIfQuantityBecomesZeroOrNegative() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        CartItem item = createCartItem(cart, product, 2);
        cart.getItems().add(item);

        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(-3); // 2 - 3 = -1

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponse response = cartService.updateItemQuantity(1L, 1L, updateDto);

        // Assert
        assertThat(response).isNotNull();
        assertThat(cart.getItems()).isEmpty();

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception when cart not found during update")
    void shouldThrowExceptionWhenCartNotFoundDuringUpdate() {
        // Arrange
        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(1);

        when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateItemQuantity(999L, 1L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart not found");

        verify(cartRepository, times(1)).findByUserId(999L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when item not in cart during update")
    void shouldThrowExceptionWhenItemNotInCartDuringUpdate() {
        // Arrange
        Cart cart = createCart(1L);
        CartItemUpdateDto updateDto = new CartItemUpdateDto();
        updateDto.setQuantityChange(1);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThatThrownBy(() -> cartService.updateItemQuantity(1L, 999L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not in cart");

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should checkout successfully")
    void shouldCheckoutSuccessfully() {
        // Arrange
        Cart cart = createCart(1L);
        Product product = createProduct(1L, "Laptop");
        CartItem item = createCartItem(cart, product, 2);
        cart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.checkout(1L);

        // Assert
        assertThat(cart.getItems()).isEmpty();

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception when checkout with empty cart")
    void shouldThrowExceptionWhenCheckoutWithEmptyCart() {
        // Arrange
        Cart cart = createCart(1L);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThatThrownBy(() -> cartService.checkout(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot checkout an empty cart");

        verify(cartRepository, times(1)).findByUserId(1L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when checkout cart not found")
    void shouldThrowExceptionWhenCheckoutCartNotFound() {
        // Arrange
        when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.checkout(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart not found");

        verify(cartRepository, times(1)).findByUserId(999L);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add multiple items to cart")
    void shouldAddMultipleItemsToCart() {
        // Arrange
        Cart cart = createCart(1L);
        Product product1 = createProduct(1L, "Laptop");
        Product product2 = createProduct(2L, "Phone");

        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(1L);
        request1.setQuantity(1);

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(2L);
        request2.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.addItemToCart(1L, request1);
        cartService.addItemToCart(1L, request2);

        // Assert
        assertThat(cart.getItems()).hasSize(2);

        verify(cartRepository, times(2)).findByUserId(1L);
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).findById(2L);
        verify(cartRepository, times(2)).save(any(Cart.class));
    }
}

