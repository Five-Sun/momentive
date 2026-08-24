package com.momentive.backend.product.repository;

import com.momentive.backend.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
