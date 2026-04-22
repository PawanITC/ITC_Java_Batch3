package com.itc.funkart.product_service.dto.response;

import com.itc.funkart.product_service.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ProductsResponse {
    private List<Product> found;
    private List<Long> missing;
}
