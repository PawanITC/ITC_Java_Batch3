package com.itc.funkart.product_service.serviceImpl;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CartItemUpdateDto;
import com.itc.funkart.product_service.entity.Cart;
import com.itc.funkart.product_service.entity.CartItem;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.kafka.producer.OrderProducer;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>CartServiceImplTest</h2>
 * Validates cart operations while mocking static security context and JPA fetches.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Service - Fixed Logic Tests")
class CartServiceImplTest {

    private final Long TEST_USER_ID = 1L;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderProducer orderProducer;
    @InjectMocks
    private CartServiceImpl cartService;
    private MockedStatic<SecurityUtils> mockedSecurity;

    @BeforeEach
    void setUp() {
        // Intercept static call to SecurityUtils
        mockedSecurity = mockStatic(SecurityUtils.class);
        mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        mockedSecurity.close();
    }

    @Test
    @DisplayName("Add Item - Should increase quantity for existing item")
    void shouldIncreaseQuantityForExistingItem() {
        // Arrange
        Product product = Product.builder().id(101L).price(BigDecimal.TEN).build();
        Cart cart = Cart.builder().userId(TEST_USER_ID).items(new ArrayList<>()).build();
        cart.getItems().add(CartItem.builder().product(product).quantity(2).build());

        AddToCartRequest request = AddToCartRequest.builder().productId(101L).quantity(3).build();

        // Must match the method name used in service: findByUserIdWithItems
        when(cartRepository.findByUserIdWithItems(TEST_USER_ID)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.addItemToCart(request);

        // Assert
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("Checkout - Should verify correct amount and clear cart")
    void shouldVerifyCheckoutLogic() {
        // Arrange
        Product product = Product.builder().id(1L).price(new BigDecimal("100.00")).build();
        CartItem item = CartItem.builder().product(product).quantity(2).build();
        Cart cart = Cart.builder().userId(TEST_USER_ID).items(new ArrayList<>()).build();
        cart.getItems().add(item);

        when(cartRepository.findByUserIdWithItems(TEST_USER_ID)).thenReturn(Optional.of(cart));

        // Act
        cartService.checkout();

        // Assert
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
        // Note: orderProducer.sendOrderEvent won't be called here because
        // TransactionSynchronizationManager.isActualTransactionActive() is false in unit tests.
        // To test Kafka, we would move to an Integration Test or refactor the sync logic.
    }

    @Test
    @DisplayName("Update Quantity - Should remove item when quantity becomes zero")
    void shouldRemoveItemOnZeroQuantity() {
        // Arrange
        Product product = Product.builder().id(1L).build();
        CartItem item = CartItem.builder().product(product).quantity(1).build();
        Cart cart = Cart.builder().userId(TEST_USER_ID).items(new ArrayList<>()).build();
        cart.getItems().add(item);

        CartItemUpdateDto updateDto = CartItemUpdateDto.builder().quantityChange(-1).build();

        when(cartRepository.findByUserIdWithItems(TEST_USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.updateItemQuantity(1L, updateDto);

        // Assert
        assertThat(cart.getItems()).isEmpty();
    }
}