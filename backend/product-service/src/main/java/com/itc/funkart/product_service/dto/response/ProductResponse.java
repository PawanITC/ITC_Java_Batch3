package com.itc.funkart.product_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private Boolean active;
}

