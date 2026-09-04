package com.momentive.backend.product.service;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.dto.ProductDetailResponse;
import com.momentive.backend.product.dto.ProductListResponse;
import com.momentive.backend.product.dto.ProductSummaryResponse;
import com.momentive.backend.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductListResponse getProducts(int page, int size, Category category, ProductSort sort, String q) {
        Page<Product> products = productRepository.findOnSaleProducts(
                category, ProductSearchKeyword.normalize(q), PageRequest.of(page, size, toSort(sort)));
        return ProductListResponse.from(products.map(ProductSummaryResponse::from));
    }

    public ProductDetailResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        // HIDDEN/DELETED 상품은 고객에게 존재 자체를 노출하지 않는다(관리자 조회는 별도 API).
        if (!product.isOnSale()) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return ProductDetailResponse.from(product);
    }

    private Sort toSort(ProductSort sort) {
        return switch (sort) {
            case NEW, POPULAR -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
        };
    }
}
