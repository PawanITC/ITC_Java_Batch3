package com.itc.funkart.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductResponse implements Serializable {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private List<String> imageUrls;
    private Boolean active;
    private String brand;
}

