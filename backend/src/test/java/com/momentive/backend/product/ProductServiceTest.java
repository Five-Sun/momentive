package com.momentive.backend.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductStatus;
import com.momentive.backend.product.dto.ProductDetailResponse;
import com.momentive.backend.product.dto.ProductListResponse;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.product.service.ProductService;
import com.momentive.backend.product.service.ProductSort;
import java.util.List;
import java.util.stream.IntStream;
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
        Product older = new Product("older", "desc", 10000, null, Category.ACCESSORY);
        productRepository.save(older);
        Thread.sleep(20); // Windows 시스템 클록 해상도(~15ms) 때문에 createdAt이 같아질 수 있어 순서 보장을 위해 간격을 둠
        Product newer = new Product("newer", "desc", 20000, 15000, Category.ACCESSORY);
        productRepository.save(newer);

        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW, null);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).name()).isEqualTo("newer");
        assertThat(response.content().get(0).discountPrice()).isEqualTo(15000);
        assertThat(response.totalElements()).isEqualTo(2);
    }

    @Test
    void getProducts_returns_empty_list_when_no_products() {
        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW, null);

        assertThat(response.content()).isEmpty();
    }

    @Test
    void getProducts_filters_by_category() {
        Product outer = new Product("아우터", "desc", 10000, null, Category.OUTER);
        Product knit = new Product("니트", "desc", 20000, null, Category.KNIT);
        productRepository.save(outer);
        productRepository.save(knit);

        ProductListResponse response = productService.getProducts(0, 20, Category.OUTER, ProductSort.NEW, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("아우터");
    }

    @Test
    void getProducts_sorts_by_price_ascending_and_descending() {
        Product cheap = new Product("cheap", "desc", 5000, null, Category.ACCESSORY);
        Product expensive = new Product("expensive", "desc", 50000, null, Category.ACCESSORY);
        productRepository.save(expensive);
        productRepository.save(cheap);

        ProductListResponse ascending = productService.getProducts(0, 20, null, ProductSort.PRICE_ASC, null);
        ProductListResponse descending = productService.getProducts(0, 20, null, ProductSort.PRICE_DESC, null);

        assertThat(ascending.content().get(0).name()).isEqualTo("cheap");
        assertThat(descending.content().get(0).name()).isEqualTo("expensive");
    }

    @Test
    void getProducts_popular_sort_matches_new_sort() {
        Product older = new Product("older", "desc", 10000, null, Category.ACCESSORY);
        Product newer = new Product("newer", "desc", 20000, null, Category.ACCESSORY);
        productRepository.save(older);
        productRepository.save(newer);

        ProductListResponse popular = productService.getProducts(0, 20, null, ProductSort.POPULAR, null);
        ProductListResponse latest = productService.getProducts(0, 20, null, ProductSort.NEW, null);

        assertThat(popular.content()).extracting("id").isEqualTo(latest.content().stream().map(p -> p.id()).toList());
    }

    @Test
    void getProduct_returns_detail_with_images_in_order() {
        Product product = new Product("치즈맛 캔", "고양이 아님, 강아지용 캔입니다.", 5000, null, Category.ACCESSORY);
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
    void getProduct_returns_variants_and_derives_sold_out_from_total_stock() {
        Product product = new Product("겨울 패딩", "desc", 28000, null, Category.OUTER);
        product.addVariant("S", 0);
        product.addVariant("M", 3);
        Product saved = productRepository.save(product);

        ProductDetailResponse response = productService.getProduct(saved.getId());

        assertThat(response.variants()).hasSize(2);
        assertThat(response.variants().get(0).size()).isEqualTo("S");
        assertThat(response.variants().get(0).soldOut()).isTrue();
        assertThat(response.variants().get(1).size()).isEqualTo("M");
        assertThat(response.variants().get(1).stock()).isEqualTo(3);
        // 재고 합이 3이므로 상품 전체는 품절이 아니다.
        assertThat(response.soldOut()).isFalse();
    }

    @Test
    void getProduct_derives_sold_out_when_every_variant_is_empty() {
        Product product = new Product("품절 상품", "desc", 1000, null, Category.ACCESSORY);
        product.addVariant(null, 0);
        Product saved = productRepository.save(product);

        assertThat(productService.getProduct(saved.getId()).soldOut()).isTrue();
    }

    @Test
    void getProducts_excludes_products_that_are_not_on_sale() {
        productRepository.save(new Product("판매중", "desc", 10000, null, Category.ACCESSORY));
        productRepository.save(
                new Product("숨김", "desc", 10000, null, Category.ACCESSORY, ProductStatus.HIDDEN));
        productRepository.save(
                new Product("삭제됨", "desc", 10000, null, Category.ACCESSORY, ProductStatus.DELETED));

        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("판매중");
    }

    @Test
    void getProduct_returns_not_found_when_product_is_not_on_sale() {
        Product hidden = productRepository.save(
                new Product("숨김", "desc", 10000, null, Category.ACCESSORY, ProductStatus.HIDDEN));

        assertThatThrownBy(() -> productService.getProduct(hidden.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getProduct_throws_when_id_is_missing() {
        assertThatThrownBy(() -> productService.getProduct(999999L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getProducts_filters_by_name_keyword_with_partial_and_case_insensitive_match() {
        productRepository.save(new Product("Winter 패딩 조끼", "desc", 10000, null, Category.OUTER));
        productRepository.save(new Product("여름 쿨매트", "desc", 20000, null, Category.ACCESSORY));

        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW, "winter");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("Winter 패딩 조끼");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void getProducts_applies_keyword_together_with_category_and_sort() {
        productRepository.save(new Product("패딩 조끼", "desc", 30000, null, Category.OUTER));
        productRepository.save(new Product("패딩 코트", "desc", 50000, null, Category.OUTER));
        productRepository.save(new Product("패딩 방석", "desc", 10000, null, Category.ACCESSORY));

        ProductListResponse response =
                productService.getProducts(0, 20, Category.OUTER, ProductSort.PRICE_DESC, "패딩");

        assertThat(response.content()).extracting("name").containsExactly("패딩 코트", "패딩 조끼");
    }

    @Test
    void getProducts_treats_blank_keyword_as_no_keyword() {
        productRepository.save(new Product("사료", "desc", 10000, null, Category.ACCESSORY));
        productRepository.save(new Product("간식", "desc", 20000, null, Category.ACCESSORY));

        assertThat(productService.getProducts(0, 20, null, ProductSort.NEW, "   ").content()).hasSize(2);
        assertThat(productService.getProducts(0, 20, null, ProductSort.NEW, "").content()).hasSize(2);
    }

    @Test
    void getProducts_excludes_hidden_and_deleted_products_from_keyword_search() {
        productRepository.save(new Product("검색 대상 판매중", "desc", 10000, null, Category.ACCESSORY));
        productRepository.save(
                new Product("검색 대상 숨김", "desc", 10000, null, Category.ACCESSORY, ProductStatus.HIDDEN));
        productRepository.save(
                new Product("검색 대상 삭제됨", "desc", 10000, null, Category.ACCESSORY, ProductStatus.DELETED));

        ProductListResponse response = productService.getProducts(0, 20, null, ProductSort.NEW, "검색 대상");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("검색 대상 판매중");
    }

    /**
     * 검색이 DB에서 수행되는지 확인한다. 프론트가 앞쪽 100개만 받아 클라이언트에서 거르던 시절에는
     * 101번째 이후에 등록된 상품이 영원히 검색되지 않았다(spec 목적 2번, 수용 기준 "검색").
     */
    @Test
    void getProducts_finds_products_beyond_the_first_hundred_rows() {
        List<Product> products = IntStream.rangeClosed(1, 105)
                .mapToObj(i -> new Product(
                        "검색회귀상품 %03d".formatted(i), "desc", 10000 + i, null, Category.ACCESSORY))
                .toList();
        productRepository.saveAll(products);

        ProductListResponse all = productService.getProducts(0, 20, null, ProductSort.NEW, "검색회귀상품");
        assertThat(all.totalElements()).isEqualTo(105);
        assertThat(all.totalPages()).isEqualTo(6);
        assertThat(all.content()).hasSize(20);

        // 105번째(= 100개 캡을 넘는) 상품이 이름으로 정확히 검색된다.
        ProductListResponse beyondCap = productService.getProducts(0, 20, null, ProductSort.NEW, "검색회귀상품 105");
        assertThat(beyondCap.totalElements()).isEqualTo(1);
        assertThat(beyondCap.content().get(0).name()).isEqualTo("검색회귀상품 105");
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
