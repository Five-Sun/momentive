package com.momentive.backend.product.controller;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.dto.ProductDetailResponse;
import com.momentive.backend.product.dto.ProductListResponse;
import com.momentive.backend.product.service.ProductService;
import com.momentive.backend.product.service.ProductSort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "new") String sort
    ) {
        return productService.getProducts(page, size, category, ProductSort.from(sort));
    }

    @GetMapping("/products/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }
}
