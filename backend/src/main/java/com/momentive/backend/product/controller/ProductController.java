package com.momentive.backend.product.controller;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.dto.ProductDetailResponse;
import com.momentive.backend.product.dto.ProductListResponse;
import com.momentive.backend.product.service.ProductService;
import com.momentive.backend.product.service.ProductSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 조회")
    @GetMapping("/products")
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "new") String sort,
            @Parameter(description = "상품명 검색어(부분일치, 대소문자 무시). 생략하면 전체 목록")
            @RequestParam(required = false) String q
    ) {
        return productService.getProducts(page, size, category, ProductSort.from(sort), q);
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/products/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }
}
