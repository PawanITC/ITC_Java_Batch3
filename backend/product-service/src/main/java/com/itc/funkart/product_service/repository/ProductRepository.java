package com.itc.funkart.product_service.repository;

import com.itc.funkart.product_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository  extends JpaRepository<Product,Long> {

    Optional<Product> findBySlug(String slug);

    List<Product> findAllByOrderByCreatedAtDesc();
}
