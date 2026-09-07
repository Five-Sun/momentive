package com.momentive.backend.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.address.dto.AddressRequest;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderItemRequest;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductStatus;
import com.momentive.backend.product.dto.ProductImageResponse;
import com.momentive.backend.product.dto.admin.AdminProductListResponse;
import com.momentive.backend.product.dto.admin.AdminProductRequest;
import com.momentive.backend.product.dto.admin.AdminProductResponse;
import com.momentive.backend.product.dto.admin.AdminProductVariantRequest;
import com.momentive.backend.product.dto.admin.AdminProductVariantResponse;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.product.service.AdminProductService;
import com.momentive.backend.product.service.ProductService;
import com.momentive.backend.product.service.ProductSort;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AdminProductServiceTest {

    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // user_coupon이 users를 참조하므로, 남아 있으면 userRepository.deleteAll()이 FK 제약에 걸린다.
    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        clearAll();
    }

    @AfterEach
    void tearDown() {
        clearAll();
    }

    private void clearAll() {
        orderRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        userCouponRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private AdminProductRequest request(
            String name, ProductStatus status, List<String> imageUrls, List<AdminProductVariantRequest> variants) {
        return new AdminProductRequest(name, "desc", 28000, 22400, Category.OUTER, status, imageUrls, variants);
    }

    private AdminProductVariantRequest newVariant(String size, int stock) {
        return new AdminProductVariantRequest(null, size, stock);
    }

    @Test
    void createProduct_saves_images_and_variants_and_exposes_product_to_customer_list() {
        AdminProductResponse created = adminProductService.createProduct(request(
                "겨울 패딩", ProductStatus.ON_SALE,
                List.of("https://res.cloudinary.com/a.jpg", "https://res.cloudinary.com/b.jpg"),
                List.of(newVariant("S", 10), newVariant("M", 5))));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(created.totalStock()).isEqualTo(15);
        assertThat(created.soldOut()).isFalse();
        assertThat(created.variants()).extracting(AdminProductVariantResponse::size).containsExactly("S", "M");
        // imageUrls 배열 순서가 그대로 displayOrder가 된다.
        assertThat(created.images()).extracting("displayOrder").containsExactly(0, 1);
        assertThat(created.images()).extracting("url")
                .containsExactly("https://res.cloudinary.com/a.jpg", "https://res.cloudinary.com/b.jpg");
        // 응답의 식별자는 폼이 그대로 되돌려 보내는 값이므로 항상 실제 PK로 채워져 있어야 한다.
        assertThat(created.variants()).extracting(AdminProductVariantResponse::id).doesNotContainNull();
        assertThat(created.images()).extracting(ProductImageResponse::id).doesNotContainNull();

        assertThat(productService.getProducts(0, 20, null, ProductSort.NEW, "겨울 패딩").content())
                .extracting("name").containsExactly("겨울 패딩");
    }

    @Test
    void createProduct_defaults_status_to_on_sale_when_omitted() {
        AdminProductResponse created = adminProductService.createProduct(
                request("사이즈 없는 간식", null, List.of(), List.of(newVariant(null, 3))));

        assertThat(created.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(created.variants()).hasSize(1);
        assertThat(created.variants().get(0).size()).isNull();
    }

    @Test
    void createProduct_allows_zero_images() {
        AdminProductResponse created = adminProductService.createProduct(
                request("이미지 없는 상품", ProductStatus.ON_SALE, null, List.of(newVariant(null, 1))));

        assertThat(created.images()).isEmpty();
    }

    @Test
    void createProduct_rejects_request_without_any_variant() {
        assertThatThrownBy(() -> adminProductService.createProduct(
                request("variant 없음", ProductStatus.ON_SALE, List.of(), List.of())))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_REQUIRED);

        assertThatThrownBy(() -> adminProductService.createProduct(
                request("variant null", ProductStatus.ON_SALE, List.of(), null)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_REQUIRED);
    }

    @Test
    void createProduct_rejects_duplicate_variant_size() {
        assertThatThrownBy(() -> adminProductService.createProduct(request(
                "사이즈 중복", ProductStatus.ON_SALE, List.of(),
                List.of(newVariant("M", 1), newVariant("M", 2)))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VARIANT_SIZE);
    }

    /**
     * "사이즈 없음"은 {@code size = null}인 단일 variant로만 표현되므로 두 개를 보내면 거부한다.
     * 빈 문자열도 null로 정규화되어 같은 판정을 받는다.
     */
    @Test
    void createProduct_rejects_two_sizeless_variants_including_blank_string() {
        assertThatThrownBy(() -> adminProductService.createProduct(request(
                "사이즈 없음 중복", ProductStatus.ON_SALE, List.of(),
                List.of(newVariant(null, 1), newVariant("   ", 2)))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_VARIANT_SIZE);
    }

    @Test
    void createProduct_rejects_more_than_five_images() {
        List<String> sixImages = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            sixImages.add("https://res.cloudinary.com/%d.jpg".formatted(i));
        }

        assertThatThrownBy(() -> adminProductService.createProduct(
                request("이미지 초과", ProductStatus.ON_SALE, sixImages, List.of(newVariant(null, 1)))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    void updateProduct_replaces_basic_fields_images_and_variants() {
        AdminProductResponse created = adminProductService.createProduct(request(
                "패딩", ProductStatus.ON_SALE, List.of("https://res.cloudinary.com/a.jpg"),
                List.of(newVariant("S", 10), newVariant("M", 5))));
        Long keptVariantId = created.variants().get(0).id();

        AdminProductResponse updated = adminProductService.updateProduct(created.id(), new AdminProductRequest(
                "패딩 리뉴얼", "새 설명", 30000, null, Category.KNIT, ProductStatus.HIDDEN,
                List.of("https://res.cloudinary.com/b.jpg", "https://res.cloudinary.com/c.jpg"),
                List.of(new AdminProductVariantRequest(keptVariantId, "S", 3), newVariant("L", 7))));

        assertThat(updated.name()).isEqualTo("패딩 리뉴얼");
        assertThat(updated.description()).isEqualTo("새 설명");
        assertThat(updated.price()).isEqualTo(30000);
        assertThat(updated.discountPrice()).isNull();
        assertThat(updated.category()).isEqualTo(Category.KNIT);
        assertThat(updated.status()).isEqualTo(ProductStatus.HIDDEN);
        assertThat(updated.images()).extracting("url")
                .containsExactly("https://res.cloudinary.com/b.jpg", "https://res.cloudinary.com/c.jpg");
        // 유지한 S는 id가 그대로이고 재고만 바뀌며, 요청에서 빠진 M은 삭제되고 L이 추가된다.
        assertThat(updated.variants()).extracting(AdminProductVariantResponse::size).containsExactly("S", "L");
        assertThat(updated.variants().get(0).id()).isEqualTo(keptVariantId);
        assertThat(updated.variants().get(0).stock()).isEqualTo(3);
        assertThat(updated.totalStock()).isEqualTo(10);
        // 새로 추가된 L과 교체된 이미지도 flush 후 PK를 갖는다(등록 응답과 같은 계약).
        assertThat(updated.variants()).extracting(AdminProductVariantResponse::id).doesNotContainNull();
        assertThat(updated.images()).extracting(ProductImageResponse::id).doesNotContainNull();
    }

    /**
     * 수정 응답을 그대로 폼 상태로 반영한 뒤 한 번 더 저장하는 관리자 흐름을 재현한다.
     * 응답의 variant id가 비어 있으면 재저장 시 기존 행이 삭제·재생성되어,
     * 그 사이 주문에 사용된 variant는 재고만 고쳐도 {@code VARIANT_IN_USE}로 막힌다.
     */
    @Test
    void updateProduct_response_ids_survive_a_second_save_of_the_same_payload() {
        AdminProductResponse created = adminProductService.createProduct(request(
                "재입고 대상", ProductStatus.ON_SALE, List.of("https://res.cloudinary.com/a.jpg"),
                List.of(newVariant("S", 10))));

        // 1차 수정: variant L과 이미지 b를 새로 추가한다.
        AdminProductResponse first = adminProductService.updateProduct(created.id(), request(
                "재입고 대상", ProductStatus.ON_SALE,
                List.of("https://res.cloudinary.com/a.jpg", "https://res.cloudinary.com/b.jpg"),
                List.of(new AdminProductVariantRequest(created.variants().get(0).id(), "S", 10),
                        newVariant("L", 7))));

        assertThat(first.variants()).extracting(AdminProductVariantResponse::id).doesNotContainNull();
        assertThat(first.images()).extracting(ProductImageResponse::id).doesNotContainNull();
        // URL이 그대로인 이미지는 행을 다시 만들지 않으므로 id가 유지된다.
        assertThat(first.images().get(0).id()).isEqualTo(created.images().get(0).id());

        // 새 variant가 주문에 사용된 뒤 재고만 고쳐 재저장해도 삭제 대상이 되면 안 된다.
        Long newVariantId = first.variants().get(1).id();
        placeOrder("variant-roundtrip@momentive.com", created.id(), newVariantId);

        AdminProductResponse second = adminProductService.updateProduct(created.id(), request(
                "재입고 대상", ProductStatus.ON_SALE,
                first.images().stream().map(ProductImageResponse::url).toList(),
                first.variants().stream()
                        .map(variant -> new AdminProductVariantRequest(variant.id(), variant.size(), 20))
                        .toList()));

        assertThat(second.variants()).extracting(AdminProductVariantResponse::id)
                .containsExactlyElementsOf(first.variants().stream()
                        .map(AdminProductVariantResponse::id).toList());
        assertThat(second.images()).extracting(ProductImageResponse::id)
                .containsExactlyElementsOf(first.images().stream().map(ProductImageResponse::id).toList());
        assertThat(second.variants()).extracting(AdminProductVariantResponse::stock).containsExactly(20, 20);
    }

    @Test
    void updateProduct_rejects_deleting_variant_that_is_used_by_an_order() {
        AdminProductResponse created = adminProductService.createProduct(request(
                "주문된 패딩", ProductStatus.ON_SALE, List.of(),
                List.of(newVariant("S", 10), newVariant("M", 5))));
        Long orderedVariantId = created.variants().get(0).id();
        Long keptVariantId = created.variants().get(1).id();
        placeOrder("variant-in-use@momentive.com", created.id(), orderedVariantId);

        // 주문에 사용된 S를 요청에서 빼면 삭제 대상이 되지만, 이력이 끊기므로 거부된다.
        assertThatThrownBy(() -> adminProductService.updateProduct(created.id(), request(
                "주문된 패딩", ProductStatus.ON_SALE, List.of(),
                List.of(new AdminProductVariantRequest(keptVariantId, "M", 5)))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_IN_USE);

        assertThat(adminProductService.getProduct(created.id()).variants())
                .extracting(AdminProductVariantResponse::id)
                .contains(orderedVariantId);
    }

    @Test
    void updateProduct_allows_setting_used_variant_stock_to_zero() {
        AdminProductResponse created = adminProductService.createProduct(request(
                "재고 0 처리", ProductStatus.ON_SALE, List.of(), List.of(newVariant("S", 10))));
        Long variantId = created.variants().get(0).id();
        placeOrder("variant-zero@momentive.com", created.id(), variantId);

        AdminProductResponse updated = adminProductService.updateProduct(created.id(), request(
                "재고 0 처리", ProductStatus.ON_SALE, List.of(),
                List.of(new AdminProductVariantRequest(variantId, "S", 0))));

        assertThat(updated.variants().get(0).stock()).isZero();
        assertThat(updated.soldOut()).isTrue();
    }

    @Test
    void updateProduct_rejects_variant_id_that_belongs_to_another_product() {
        AdminProductResponse target = adminProductService.createProduct(
                request("대상 상품", ProductStatus.ON_SALE, List.of(), List.of(newVariant("S", 1))));
        AdminProductResponse other = adminProductService.createProduct(
                request("다른 상품", ProductStatus.ON_SALE, List.of(), List.of(newVariant("S", 1))));
        Long foreignVariantId = other.variants().get(0).id();

        assertThatThrownBy(() -> adminProductService.updateProduct(target.id(), request(
                "대상 상품", ProductStatus.ON_SALE, List.of(),
                List.of(new AdminProductVariantRequest(foreignVariantId, "S", 1)))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VARIANT_NOT_FOUND);
    }

    @Test
    void updateProduct_throws_not_found_for_unknown_product() {
        assertThatThrownBy(() -> adminProductService.updateProduct(999999L,
                request("없음", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1)))))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void deleteProduct_marks_status_deleted_and_keeps_the_row() {
        AdminProductResponse created = adminProductService.createProduct(
                request("삭제될 상품", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1))));

        adminProductService.deleteProduct(created.id());

        Product row = productRepository.findById(created.id()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(ProductStatus.DELETED);
        // 관리자 상세는 DELETED도 계속 조회 가능하다.
        assertThat(adminProductService.getProduct(created.id()).status()).isEqualTo(ProductStatus.DELETED);
    }

    @Test
    void deleted_and_hidden_products_disappear_from_customer_list_search_and_detail() {
        AdminProductResponse hidden = adminProductService.createProduct(
                request("고객노출테스트 숨김", ProductStatus.HIDDEN, List.of(), List.of(newVariant(null, 1))));
        AdminProductResponse deleted = adminProductService.createProduct(
                request("고객노출테스트 삭제", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1))));
        adminProductService.deleteProduct(deleted.id());
        adminProductService.createProduct(
                request("고객노출테스트 판매중", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1))));

        assertThat(productService.getProducts(0, 20, null, ProductSort.NEW, null).content())
                .extracting("name").containsExactly("고객노출테스트 판매중");
        assertThat(productService.getProducts(0, 20, null, ProductSort.NEW, "고객노출테스트").content())
                .extracting("name").containsExactly("고객노출테스트 판매중");

        assertThatThrownBy(() -> productService.getProduct(hidden.id()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        assertThatThrownBy(() -> productService.getProduct(deleted.id()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getProducts_defaults_to_on_sale_and_hidden_and_can_include_deleted_explicitly() {
        adminProductService.createProduct(
                request("관리자목록 판매중", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1))));
        adminProductService.createProduct(
                request("관리자목록 숨김", ProductStatus.HIDDEN, List.of(), List.of(newVariant(null, 1))));
        AdminProductResponse deleted = adminProductService.createProduct(
                request("관리자목록 삭제", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1))));
        adminProductService.deleteProduct(deleted.id());

        AdminProductListResponse defaults =
                adminProductService.getProducts(0, 20, AdminProductService.DEFAULT_LIST_STATUSES, null);
        assertThat(defaults.content()).extracting("name")
                .containsExactlyInAnyOrder("관리자목록 판매중", "관리자목록 숨김");
        assertThat(defaults.totalElements()).isEqualTo(2);

        AdminProductListResponse withDeleted =
                adminProductService.getProducts(0, 20, List.of(ProductStatus.DELETED), null);
        assertThat(withDeleted.content()).extracting("name").containsExactly("관리자목록 삭제");
    }

    @Test
    void getProducts_filters_by_keyword_and_reports_total_stock_and_thumbnail() {
        adminProductService.createProduct(request(
                "관리자검색 패딩", ProductStatus.ON_SALE, List.of("https://res.cloudinary.com/thumb.jpg"),
                List.of(newVariant("S", 2), newVariant("M", 3))));
        adminProductService.createProduct(
                request("관리자검색 제외대상", ProductStatus.ON_SALE, List.of(), List.of(newVariant(null, 1))));

        AdminProductListResponse response = adminProductService.getProducts(0, 20, null, "패딩");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("관리자검색 패딩");
        assertThat(response.content().get(0).totalStock()).isEqualTo(5);
        assertThat(response.content().get(0).thumbnailUrl()).isEqualTo("https://res.cloudinary.com/thumb.jpg");
    }

    @Test
    void getProduct_throws_not_found_instead_of_returning_an_empty_body() {
        assertThatThrownBy(() -> adminProductService.getProduct(999999L))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    /**
     * variant 사용 이력을 만들기 위해 실제 주문을 1건 생성한다.
     */
    private void placeOrder(String email, Long productId, Long variantId) {
        User user = userRepository.save(User.createUser(email, "hash", "몽이"));
        orderService.createOrder(user.getId(), new OrderCreateRequest(
                List.of(new OrderItemRequest(productId, variantId, 1)),
                null,
                new AddressRequest("몽이", "010-1111-2222", "12345", "서울시 강남구", "101호", true),
                null));
    }
}
