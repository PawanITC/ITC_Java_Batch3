package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.entity.ProductImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductImageRepositoryTest {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product createProduct() {
        return Product.builder()
                .name("Phone")
                .slug("phone-slug")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(10)
                .brand("TestBrand")
                .build();
    }

    @Test
    @DisplayName("Should save product image with product")
    void shouldSaveProductImageWithProduct() {
        Product product = productRepository.save(createProduct());

        ProductImage image = ProductImage.builder()
                .imageUrl("http://image.com/1.png")
                .isPrimary(true)
                .product(product)
                .build();

        ProductImage saved = productImageRepository.save(image);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProduct()).isNotNull();
        assertThat(saved.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("Should persist multiple images for one product")
    void shouldSaveMultipleImagesForProduct() {
        Product product = productRepository.save(createProduct());

        ProductImage img1 = ProductImage.builder()
                .imageUrl("img1.png")
                .product(product)
                .build();

        ProductImage img2 = ProductImage.builder()
                .imageUrl("img2.png")
                .product(product)
                .build();

        productImageRepository.save(img1);
        productImageRepository.save(img2);

        assertThat(productImageRepository.findAll()).hasSize(2);
    }
}
