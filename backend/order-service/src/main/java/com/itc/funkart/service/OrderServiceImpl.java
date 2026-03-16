package com.itc.funkart.service;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.kafka.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderEventProducer producer;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        Order order = mapper.toEntity(request);

        // ✅ Convert customerId and productId strings to UUID
        order.setCustomerId(UUID.fromString(request.getCustomerId()));
        order.setProductId(UUID.fromString(request.getProductId()));

        order.setOrderStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());

        Order saved = repository.save(order);

        // Publish Kafka event
        producer.publishOrderCreated(saved);

        return mapper.toResponse(saved);
    }

    @Override
    public OrderResponse getOrder(UUID id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // ✅ Convert strings to UUID if they are updated
        if (request.getCustomerId() != null) {
            order.setCustomerId(UUID.fromString(request.getCustomerId()));
        }
        if (request.getProductId() != null) {
            order.setProductId(UUID.fromString(request.getProductId()));
        }

        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setUpdatedAt(LocalDateTime.now());

        repository.save(order);

        // Publish Kafka event
        producer.publishOrderUpdated(order);

        return mapper.toResponse(order);
    }

    @Override
    public void deleteOrder(UUID id) {
        repository.deleteById(id);
        producer.publishOrderCancelled(id);
    }
}