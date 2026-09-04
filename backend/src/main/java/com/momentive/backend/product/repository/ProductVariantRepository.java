package com.momentive.backend.product.repository;

import com.momentive.backend.product.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
}
