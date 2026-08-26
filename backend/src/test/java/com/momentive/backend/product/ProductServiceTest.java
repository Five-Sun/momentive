package com.momentive.backend.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.dto.ProductDetailResponse;
import com.momentive.backend.product.dto.ProductListResponse;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.product.service.ProductService;
import com.momentive.backend.product.service.ProductSort;
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
    void getProducts_returns_paginated_list_ordered_by_latest() throws InterruptedException {
        Product older = new Product("older", "desc", 10000, null, false, Category.ACCESSORY);
        productRepository.save(older);
        Thread.sleep(20); // Windows 시스템 클록 해상도(~15ms) 때문에 createdAt이 같아질 수 있어 순서 보장을 위해 간격을 둠
        Product newer = new Product("newer", "desc", 20000, 15000, false, Category.ACCESSORY);
        productRepository.save(newer);

        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("newer");
        assertThat(response.content().get(0).discountPrice()).isEqualTo(15000);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void getProducts_returns_empty_list_when_no_products() {
        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW);

        assertThat(response.content()).isEmpty();
    }

    @Test
    void getProducts_filters_by_category() {
        Product outer = new Product("아우터", "desc", 10000, null, false, Category.OUTER);
        Product knit = new Product("니트", "desc", 20000, null, false, Category.KNIT);
        productRepository.save(outer);
        productRepository.save(knit);

        ProductListResponse response = productService.getProducts(0, 20, Category.OUTER, ProductSort.NEW);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("아우터");
    }

    @Test
    void getProducts_sorts_by_price_ascending_and_descending() {
        Product cheap = new Product("cheap", "desc", 5000, null, false, Category.ACCESSORY);
        Product expensive = new Product("expensive", "desc", 50000, null, false, Category.ACCESSORY);
        productRepository.save(expensive);
        productRepository.save(cheap);

        ProductListResponse ascending = productService.getProducts(0, 20, null, ProductSort.PRICE_ASC);
        ProductListResponse descending = productService.getProducts(0, 20, null, ProductSort.PRICE_DESC);

        assertThat(ascending.content().get(0).name()).isEqualTo("cheap");
        assertThat(descending.content().get(0).name()).isEqualTo("expensive");
    }

    @Test
    void getProducts_popular_sort_matches_new_sort() {
        Product older = new Product("older", "desc", 10000, null, false, Category.ACCESSORY);
        Product newer = new Product("newer", "desc", 20000, null, false, Category.ACCESSORY);
        productRepository.save(older);
        productRepository.save(newer);

        ProductListResponse popular = productService.getProducts(0, 20, null, ProductSort.POPULAR);
        ProductListResponse latest = productService.getProducts(0, 20, null, ProductSort.NEW);

        assertThat(popular.content()).extracting("id").isEqualTo(latest.content().stream().map(p -> p.id()).toList());
    }

    @Test
    void getProduct_returns_detail_with_images_in_order() {
        Product product = new Product("치즈맛 캔", "고양이 아님, 강아지용 캔입니다.", 5000, null, false, Category.ACCESSORY);
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

    @Test
    void productSort_from_parses_case_insensitively_and_rejects_unknown_value() {
        assertThat(ProductSort.from("price_asc")).isEqualTo(ProductSort.PRICE_ASC);
        assertThatThrownBy(() -> ProductSort.from("bogus"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SORT);
    }
}
