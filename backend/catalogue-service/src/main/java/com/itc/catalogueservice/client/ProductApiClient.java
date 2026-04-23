//Client API to interact with the ProductAPI

package com.itc.catalogueservice.client;

import com.itc.catalogueservice.dto.ProductDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Component
@Slf4j
public class ProductApiClient {

    private final Executor productExecutor;

    public ProductApiClient(@Qualifier("productExecutor") Executor productExecutor) {
        this.productExecutor = productExecutor;
    }

    @RateLimiter(name = "productService")
    //@Bulkhead  //Alternative to bulkhead is being used with executor
    @Retry(name = "productService")
    @TimeLimiter(name = "productService")
    @CircuitBreaker(name = "productService")

    public CompletableFuture<List<ProductDTO>> getProducts() {


        return CompletableFuture.supplyAsync(() -> {
            log.info("API CALL TRIGGERED");

            //Use this to test Bulkhead

            /*try {
                Thread.sleep(3000); // ADD THIS (force timeout > 2s)
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/


            List<ProductDTO> products = List.of(
                    new ProductDTO(1L,"iPhone 15","Apple smartphone",new BigDecimal("999.99"),"iphone.jpg",4.7,10,"Electronics"),
                    new ProductDTO(2L,"Samsung TV","4K Smart TV",new BigDecimal("799.99"),"tv.jpg",4.5,5,"Electronics"),
                    new ProductDTO(3L,"MacBook Air","Apple laptop",new BigDecimal("1199.99"),"macbook.jpg",4.8,7,"Electronics"),
                    new ProductDTO(4L,"Dell XPS 13","Ultrabook laptop",new BigDecimal("1099.99"),"dell.jpg",4.6,8,"Electronics"),
                    new ProductDTO(5L,"Sony Headphones","Noise cancelling headphones",new BigDecimal("349.99"),"sony.jpg",4.6,12,"Electronics"),
                    new ProductDTO(6L,"Apple Watch","Smart watch",new BigDecimal("429.99"),"watch.jpg",4.7,9,"Electronics"),
                    new ProductDTO(7L,"PlayStation 5","Gaming console",new BigDecimal("499.99"),"ps5.jpg",4.9,6,"Gaming"),
                    new ProductDTO(8L,"Xbox Series X","Microsoft gaming console",new BigDecimal("499.99"),"xbox.jpg",4.8,4,"Gaming"),
                    new ProductDTO(9L,"Nintendo Switch","Hybrid gaming console",new BigDecimal("299.99"),"switch.jpg",4.7,11,"Gaming"),
                    new ProductDTO(10L,"Logitech Mouse","Wireless mouse",new BigDecimal("49.99"),"mouse.jpg",4.5,20,"Accessories"),
                    new ProductDTO(11L,"Mechanical Keyboard","RGB gaming keyboard",new BigDecimal("129.99"),"keyboard.jpg",4.6,15,"Accessories"),
                    new ProductDTO(12L,"iPad Pro","Apple tablet",new BigDecimal("999.99"),"ipad.jpg",4.8,7,"Electronics"),
                    new ProductDTO(13L,"Samsung Galaxy Tab","Android tablet",new BigDecimal("649.99"),"tab.jpg",4.6,6,"Electronics"),
                    new ProductDTO(14L,"GoPro Hero","Action camera",new BigDecimal("399.99"),"gopro.jpg",4.7,8,"Cameras"),
                    new ProductDTO(15L,"Canon DSLR","Professional camera",new BigDecimal("899.99"),"canon.jpg",4.6,5,"Cameras"),
                    new ProductDTO(16L,"Smart Speaker","Voice assistant speaker",new BigDecimal("99.99"),"speaker.jpg",4.4,18,"Smart Home"),
                    new ProductDTO(17L,"Ring Doorbell","Smart security doorbell",new BigDecimal("199.99"),"ring.jpg",4.5,10,"Smart Home"),
                    new ProductDTO(18L,"Philips Hue Bulb","Smart light bulb",new BigDecimal("59.99"),"hue.jpg",4.6,25,"Smart Home"),
                    new ProductDTO(19L,"Fitness Tracker","Health monitoring device",new BigDecimal("149.99"),"fitness.jpg",4.5,14,"Wearables"),
                    new ProductDTO(20L,"VR Headset","Virtual reality headset",new BigDecimal("399.99"),"vr.jpg",4.6,9,"Gaming")
            );

            return products;
        }, productExecutor);
    }



}