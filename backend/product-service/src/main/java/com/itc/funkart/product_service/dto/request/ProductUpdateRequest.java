package com.itc.funkart.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {

    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    private String description;

    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Brand is required")
    private String brand;

    private Boolean active;
}
