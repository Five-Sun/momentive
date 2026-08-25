package com.momentive.backend.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.dto.ProductDetailResponse;
import com.momentive.backend.product.dto.ProductListResponse;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.product.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    void getProducts_returns_paginated_list_ordered_by_latest() {
        Product older = new Product("older", "desc", 10000, null, false);
        Product newer = new Product("newer", "desc", 20000, 15000, false);
        productRepository.save(older);
        productRepository.save(newer);

        ProductListResponse response = productService.getProducts(0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("newer");
        assertThat(response.content().get(0).discountPrice()).isEqualTo(15000);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void getProducts_returns_empty_list_when_no_products() {
        ProductListResponse response = productService.getProducts(0, 20);

        assertThat(response.content()).isEmpty();
    }

    @Test
    void getProduct_returns_detail_with_images_in_order() {
        Product product = new Product("치즈맛 캔", "고양이 아님, 강아지용 캔입니다.", 5000, null, false);
        product.addImage("https://example.com/1.jpg", 0);
        product.addImage("https://example.com/2.jpg", 1);
        Product saved = productRepository.save(product);

        ProductDetailResponse response = productService.getProduct(saved.getId());

        assertThat(response.name()).isEqualTo("치즈맛 캔");
        assertThat(response.images()).hasSize(2);
        assertThat(response.images().get(0).url()).isEqualTo("https://example.com/1.jpg");
        assertThat(response.images().get(1).url()).isEqualTo("https://example.com/2.jpg");
    }

    @Test
    void getProduct_throws_when_id_is_missing() {
        assertThatThrownBy(() -> productService.getProduct(999999L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}
