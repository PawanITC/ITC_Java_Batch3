
package com.itc.funkart.controller;

import com.itc.funkart.constants.ApiConstants;
import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.exception.OrderNotFound;
import com.itc.funkart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(service.createOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        return service.getAllOrders();
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable UUID id,
                                                     @RequestBody OrderRequest request) {
        return ResponseEntity.ok(service.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String>  deleteOrder(@PathVariable UUID id)
    {
        String response = service.deleteOrder(id);
        return ResponseEntity.ok(response);
    }
}