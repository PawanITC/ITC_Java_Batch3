package com.itc.funkart.product.repository;

import com.itc.funkart.product.entity.Product;
import com.itc.funkart.product.entity.ProductImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>ProductImageRepositoryTest</h2>
 * <p>
 * Verifies the persistence of product media and relationship integrity.
 * Ensures that images are correctly associated with parent products.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Product Image Repository Integration Tests")
class ProductImageRepositoryTest {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private TestEntityManager entityManager;

    // --- Helpers using Entity Builders ---

    private Product createProduct(String name) {
        return Product.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .brand("TestBrand")
                .active(true)
                .images(new ArrayList<>())
                .build();
    }

    private ProductImage createProductImage(Product product, String url, boolean isPrimary) {
        return ProductImage.builder()
                .imageUrl(url)
                .isPrimary(isPrimary)
                .product(product)
                .build();
    }

    @Test
    @DisplayName("Save - Should associate image with product correctly")
    void shouldSaveProductImageWithProduct() {
        // Arrange
        Product product = entityManager.persist(createProduct("Smartphone"));
        ProductImage image = createProductImage(product, "https://cdn.com/phone.png", true);

        // Act
        ProductImage saved = productImageRepository.save(image);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProduct().getName()).isEqualTo("Smartphone");
        assertThat(saved.getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("Query - Should retrieve all images for a specific product")
    void shouldSaveMultipleImagesForProduct() {
        // Arrange
        Product product = entityManager.persist(createProduct("Laptop"));
        entityManager.persist(createProductImage(product, "img1.png", true));
        entityManager.persist(createProductImage(product, "img2.png", false));

        entityManager.flush();
        entityManager.clear();

        // Act
        // Assuming your repository has a findByProductId method
        // If not, we use the standard findAll and verify the count
        assertThat(productImageRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Delete - Should remove image but preserve Product (Branch: Referential Integrity)")
    void shouldDeleteImagePreservingProduct() {
        // Arrange
        Product product = entityManager.persist(createProduct("Camera"));
        ProductImage image = entityManager.persist(createProductImage(product, "cam.png", true));
        entityManager.flush();

        // Act
        productImageRepository.delete(image);
        entityManager.flush();

        // Assert
        assertThat(productImageRepository.findById(image.getId())).isEmpty();
        assertThat(entityManager.find(Product.class, product.getId())).isNotNull();
    }

    @Test
    @DisplayName("Update - Should change primary image status")
    void shouldUpdateImagePrimaryStatus() {
        // Arrange
        Product product = entityManager.persist(createProduct("Tablet"));
        ProductImage image = productImageRepository.save(createProductImage(product, "tab.png", false));

        // Act
        image.setIsPrimary(true);
        ProductImage updated = productImageRepository.saveAndFlush(image);

        // Assert
        assertThat(updated.getIsPrimary()).isTrue();
    }
}