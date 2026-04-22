package com.itc.funkart.product_service.config;

import com.itc.funkart.product_service.entity.Category;
import com.itc.funkart.product_service.entity.Product;
import com.itc.funkart.product_service.entity.ProductImage;
import com.itc.funkart.product_service.repository.CategoryRepository;
import com.itc.funkart.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (categoryRepository.count() > 0) {
            log.info("Database already contains data. Skipping initialization.");
            return;
        }

        log.info("Initializing sample data for Funkart...");

        // 1. Create Categories
        Category electronics = Category.builder().name("Electronics").description("Gadgets and devices").build();
        Category fashion = Category.builder().name("Fashion").description("Clothing and accessories").build();
        categoryRepository.saveAll(Arrays.asList(electronics, fashion));

        // 2. Create 10 Products
        createProduct("UltraBook Pro", "High performance laptop", 1200.00, 15, electronics);
        createProduct("SmartWatch Series 5", "Stay connected on the go", 250.00, 30, electronics);
        createProduct("Noise Cancelling Headphones", "Pure sound experience", 180.00, 25, electronics);
        createProduct("4K Gaming Monitor", "Stunning visuals for gamers", 450.00, 10, electronics);
        createProduct("Mechanical Keyboard", "RGB backlit clicky keys", 90.00, 50, electronics);

        createProduct("Classic Leather Jacket", "Timeless style for men", 120.00, 20, fashion);
        createProduct("Cotton Slim Fit T-Shirt", "Breathable daily wear", 25.00, 100, fashion);
        createProduct("Running Sneakers", "Lightweight and durable", 85.00, 45, fashion);
        createProduct("Designer Sunglasses", "UV protection with style", 150.00, 15, fashion);
        createProduct("Canvas Backpack", "Perfect for daily commute", 45.00, 60, fashion);

        log.info("Successfully pre-loaded 2 Categories and 10 Products.");
    }

    private void createProduct(String name, String desc, double price, int stock, Category cat) {
        Product product = Product.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description(desc)
                .price(BigDecimal.valueOf(price))
                .stockQuantity(stock)
                .active(true)
                .category(cat)
                .build();

        // Adding a dummy image for each product
        ProductImage image = ProductImage.builder()
                .imageUrl("https://via.placeholder.com/400x400?text=" + name.replace(" ", "+"))
                .isPrimary(true)
                .product(product)
                .build();

        product.setImages(List.of(image));
        productRepository.save(product);
    }
}
