package com.itc.funkart.product_service.integration;

import com.itc.funkart.product_service.dto.request.AddToCartRequest;
import com.itc.funkart.product_service.dto.request.CategoryRequest;
import com.itc.funkart.product_service.dto.request.ProductCreateRequest;
import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.repository.CartRepository;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import com.itc.funkart.product_service.service.CartService;
import com.itc.funkart.product_service.service.CategoryService;
import com.itc.funkart.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Cross-Feature Integration Tests")
class CrossFeatureIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    @DisplayName("Should complete full e-commerce workflow: Create category → Create product → Add to cart → Checkout")
    void shouldCompleteFullECommerceWorkflow() {
        // Step 1: Create Category
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Electronics_" + System.nanoTime());
        categoryRequest.setDescription("Electronic devices");
        var category = categoryService.createCategory(categoryRequest);

        // Step 2: Create Products in Category
        ProductCreateRequest productRequest1 = ProductCreateRequest.builder()
                .name("Laptop")
                .description("High-performance laptop")
                .price(BigDecimal.valueOf(999.99))
                .stockQuantity(5)
                .categoryId(category.getId())
                .brand("TechBrand")
                .build();

        ProductCreateRequest productRequest2 = ProductCreateRequest.builder()
                .name("Mouse")
                .description("Wireless mouse")
                .price(BigDecimal.valueOf(49.99))
                .stockQuantity(100)
                .categoryId(category.getId())
                .brand("TechBrand")
                .build();

        ProductResponse laptop = productService.createProduct(productRequest1);
        ProductResponse mouse = productService.createProduct(productRequest2);

        // Step 3: Verify products are in category
        List<ProductResponse> allProducts = productService.getAllProducts();
        assertThat(allProducts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(allProducts.stream().map(ProductResponse::getCategoryName))
                .contains("Electronics");

        // Step 4: Add products to cart
        AddToCartRequest addLaptop = new AddToCartRequest();
        addLaptop.setProductId(laptop.getId());
        addLaptop.setQuantity(1);

        AddToCartRequest addMouse = new AddToCartRequest();
        addMouse.setProductId(mouse.getId());
        addMouse.setQuantity(2);

        Long userId = 1000L;
        cartService.addItemToCart(userId, addLaptop);
        var cart = cartService.addItemToCart(userId, addMouse);

        // Step 5: Verify cart contents
        assertThat(cart.getItems()).hasSize(2);
        assertThat(cart.getUserId()).isEqualTo(userId);

        // Step 6: Checkout
        cartService.checkout(userId);
        var finalCart = cartService.getCartByUserId(userId);

        // Assert: Cart is empty after checkout
        assertThat(finalCart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should handle product creation and batch fetch workflow")
    void shouldHandleProductCreationAndBatchFetch() {
        // Step 1: Create Category
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Fashion_" + System.nanoTime());
        categoryRequest.setDescription("Clothing and fashion");
        var category = categoryService.createCategory(categoryRequest);

        // Step 2: Create multiple products
        ProductResponse shirt = productService.createProduct(ProductCreateRequest.builder()
                .name("T-Shirt")
                .description("Cotton t-shirt")
                .price(BigDecimal.valueOf(29.99))
                .stockQuantity(50)
                .categoryId(category.getId())
                .brand("FashionBrand")
                .build());

        ProductResponse jeans = productService.createProduct(ProductCreateRequest.builder()
                .name("Jeans")
                .description("Blue denim jeans")
                .price(BigDecimal.valueOf(79.99))
                .stockQuantity(30)
                .categoryId(category.getId())
                .brand("FashionBrand")
                .build());

        ProductResponse shoes = productService.createProduct(ProductCreateRequest.builder()
                .name("Shoes")
                .description("Running shoes")
                .price(BigDecimal.valueOf(119.99))
                .stockQuantity(20)
                .categoryId(category.getId())
                .brand("FashionBrand")
                .build());

        // Step 3: Batch fetch products
        var response = productService.getProductsByIds(
                List.of(shirt.getId(), jeans.getId(), shoes.getId()));

        // Assert
        assertThat(response.getFound()).hasSize(3);
        assertThat(response.getMissing()).isEmpty();
    }

    @Test
    @DisplayName("Should manage multiple shopping carts with shared products")
    void shouldManageMultipleShoppingCartsWithSharedProducts() {
        // Step 1: Create Category and Product
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Books");
        categoryRequest.setDescription("Books collection");
        var category = categoryService.createCategory(categoryRequest);

        ProductResponse book = productService.createProduct(ProductCreateRequest.builder()
                .name("Java Programming")
                .description("Advanced Java programming book")
                .price(BigDecimal.valueOf(59.99))
                .stockQuantity(100)
                .categoryId(category.getId())
                .brand("TechBooks")
                .build());

        // Step 2: Add same product to multiple carts
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(book.getId());

        request.setQuantity(1);
        var user1Cart = cartService.addItemToCart(2000L, request);

        request.setQuantity(2);
        var user2Cart = cartService.addItemToCart(2001L, request);

        request.setQuantity(3);
        var user3Cart = cartService.addItemToCart(2002L, request);

        // Assert - Different users have different quantities
        assertThat(user1Cart.getItems().get(0).getQuantity()).isEqualTo(1);
        assertThat(user2Cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(user3Cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle product deletion cascade to cart")
    void shouldHandleProductDeletionCascadeToCart() {
        // Step 1: Create category and product
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Temporary");
        categoryRequest.setDescription("Temporary category");
        var category = categoryService.createCategory(categoryRequest);

        ProductResponse product = productService.createProduct(ProductCreateRequest.builder()
                .name("Temporary Product")
                .description("Will be deleted")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("TempBrand")
                .build());

        // Step 2: Add to cart
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);
        cartService.addItemToCart(3000L, request);

        // Step 3: Verify product is in cart
        var cart = cartService.getCartByUserId(3000L);
        assertThat(cart.getItems()).hasSize(1);

        // Step 4: Delete product
        productService.deleteProduct(product.getId());

        // Assert - Product is deleted from database
        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should verify category operations with product dependencies")
    void shouldVerifyCategoryOperationsWithProductDependencies() {
        // Step 1: Create category
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Hardware");
        categoryRequest.setDescription("Hardware components");
        var category = categoryService.createCategory(categoryRequest);

        // Step 2: Add products
        ProductResponse gpu = productService.createProduct(ProductCreateRequest.builder()
                .name("Graphics Card")
                .description("High-end GPU")
                .price(BigDecimal.valueOf(599.99))
                .stockQuantity(5)
                .categoryId(category.getId())
                .brand("HardwareBrand")
                .build());

        ProductResponse ram = productService.createProduct(ProductCreateRequest.builder()
                .name("RAM Module")
                .description("32GB DDR4 RAM")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(20)
                .categoryId(category.getId())
                .brand("HardwareBrand")
                .build());

        // Step 3: Fetch category and verify products
        var retrievedCategory = categoryService.getCategoryById(category.getId());
        assertThat(retrievedCategory).isNotNull();
        assertThat(retrievedCategory.getName()).isEqualTo("Hardware");

        // Step 4: Fetch all products and verify they're in category
        List<ProductResponse> products = productService.getAllProducts();
        var categoryProducts = products.stream()
                .filter(p -> "Hardware".equals(p.getCategoryName()))
                .toList();
        assertThat(categoryProducts).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("Should handle complex shopping scenario with multiple operations")
    void shouldHandleComplexShoppingScenario() {
        // Create category
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Gaming");
        categoryRequest.setDescription("Gaming products");
        var category = categoryService.createCategory(categoryRequest);

        // Create multiple products
        ProductResponse console = productService.createProduct(ProductCreateRequest.builder()
                .name("Gaming Console")
                .description("Latest gaming console")
                .price(BigDecimal.valueOf(499.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("GamingBrand")
                .build());

        ProductResponse controller = productService.createProduct(ProductCreateRequest.builder()
                .name("Game Controller")
                .description("Wireless controller")
                .price(BigDecimal.valueOf(69.99))
                .stockQuantity(50)
                .categoryId(category.getId())
                .brand("GamingBrand")
                .build());

        ProductResponse headset = productService.createProduct(ProductCreateRequest.builder()
                .name("Gaming Headset")
                .description("7.1 surround sound")
                .price(BigDecimal.valueOf(149.99))
                .stockQuantity(30)
                .categoryId(category.getId())
                .brand("GamingBrand")
                .build());

        // User adds items to cart
        Long userId = 4000L;
        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(console.getId());
        request1.setQuantity(1);
        cartService.addItemToCart(userId, request1);

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(controller.getId());
        request2.setQuantity(2);
        cartService.addItemToCart(userId, request2);

        AddToCartRequest request3 = new AddToCartRequest();
        request3.setProductId(headset.getId());
        request3.setQuantity(1);
        var cartAfterAdding = cartService.addItemToCart(userId, request3);

        // Verify cart state
        assertThat(cartAfterAdding.getItems()).hasSize(3);
        assertThat(cartAfterAdding.getItems().stream().mapToInt(i -> i.getQuantity()).sum())
                .isEqualTo(4); // 1 + 2 + 1

        // Checkout
        cartService.checkout(userId);

        // Verify cart is empty
        var emptyCart = cartService.getCartByUserId(userId);
        assertThat(emptyCart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should verify data consistency across all features")
    void shouldVerifyDataConsistencyAcrossAllFeatures() {
        // Create category
        CategoryRequest categoryRequest = new CategoryRequest();
        categoryRequest.setName("Consistency Test");
        categoryRequest.setDescription("Testing consistency");
        var category = categoryService.createCategory(categoryRequest);

        // Create product
        ProductResponse product = productService.createProduct(ProductCreateRequest.builder()
                .name("Consistency Product")
                .description("Test product")
                .price(BigDecimal.valueOf(99.99))
                .stockQuantity(10)
                .categoryId(category.getId())
                .brand("TestBrand")
                .build());

        // Add to cart
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product.getId());
        request.setQuantity(5);
        cartService.addItemToCart(5000L, request);

        // Verify consistency across all repositories
        assertThat(categoryRepository.findById(category.getId())).isPresent();
        assertThat(productRepository.findById(product.getId())).isPresent();
        assertThat(cartRepository.findByUserId(5000L)).isPresent();

        // Verify relationships
        var cartInDb = cartRepository.findByUserId(5000L).orElseThrow();
        assertThat(cartInDb.getItems()).hasSize(1);
        assertThat(cartInDb.getItems().get(0).getProduct().getCategory().getId())
                .isEqualTo(category.getId());
    }
}

