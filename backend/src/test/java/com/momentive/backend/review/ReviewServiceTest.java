package com.momentive.backend.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.address.domain.Address;
import com.momentive.backend.address.repository.AddressRepository;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderItem;
import com.momentive.backend.order.repository.OrderRepository;
import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.repository.ProductRepository;
import com.momentive.backend.review.dto.MyReviewResponse;
import com.momentive.backend.review.dto.ReviewCreateRequest;
import com.momentive.backend.review.dto.ReviewCreateResponse;
import com.momentive.backend.review.dto.ReviewListResponse;
import com.momentive.backend.review.repository.ReviewRepository;
import com.momentive.backend.review.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // user_coupon이 users를 참조하므로, 남아 있으면 userRepository.deleteAll()이 FK 제약에 걸린다.
    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        userCouponRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userRepository.save(User.createUser(email, "hash", "몽이"));
    }

    private Product createProduct(String name) {
        return productRepository.save(new Product(name, "desc", 10000, null, false, Category.ACCESSORY, 10));
    }

    /**
     * 구매 확인(verified purchase) 조건을 충족시키기 위해 PAID 상태의 주문을 직접 만든다.
     */
    private void createPaidOrder(User user, Product product) {
        Address address = addressRepository.save(Address.create(user, "몽이", "010-1111-2222", "12345", "서울시 강남구", null, true));
        Order order = Order.createPending(user, address, product.getPrice());
        order.addItem(OrderItem.create(order, product, 1, null, product.getPrice()));
        order.markAsPaid("payment-key");
        orderRepository.save(order);
    }

    private ReviewCreateRequest newRequest(int rating, String text) {
        return new ReviewCreateRequest(rating, text);
    }

    @Test
    void createReview_updates_product_rating_summary() {
        User user = createUser("review1@momentive.com");
        Product product = createProduct("사료");
        createPaidOrder(user, product);

        ReviewCreateResponse response = reviewService.createReview(product.getId(), user.getId(),
                newRequest(5, "정말 좋은 상품이에요 강아지가 잘 먹어요"));

        assertThat(response.rating()).isEqualTo(5);
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getAverageRating()).isEqualTo(5.0);
        assertThat(reloaded.getReviewCount()).isEqualTo(1);
    }

    @Test
    void createReview_fails_without_purchase_history() {
        User user = createUser("review2@momentive.com");
        Product product = createProduct("사료");

        assertThatThrownBy(() -> reviewService.createReview(product.getId(), user.getId(),
                newRequest(5, "구매 안했는데 리뷰 써봅니다")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PURCHASE_NOT_VERIFIED);
    }

    @Test
    void createReview_fails_on_duplicate_review_for_same_product() {
        User user = createUser("review3@momentive.com");
        Product product = createProduct("사료");
        createPaidOrder(user, product);
        reviewService.createReview(product.getId(), user.getId(), newRequest(4, "괜찮은 상품이었습니다 만족해요"));

        assertThatThrownBy(() -> reviewService.createReview(product.getId(), user.getId(),
                newRequest(3, "두번째 리뷰를 작성해봅니다 이것도 통과되나요")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    void updateReview_and_deleteReview_fail_for_non_owner() {
        User owner = createUser("owner@momentive.com");
        User other = createUser("other@momentive.com");
        Product product = createProduct("사료");
        createPaidOrder(owner, product);
        ReviewCreateResponse review = reviewService.createReview(product.getId(), owner.getId(),
                newRequest(4, "괜찮은 상품이었습니다 만족해요"));

        assertThatThrownBy(() -> reviewService.updateReview(product.getId(), review.reviewId(), other.getId(),
                newRequest(1, "타인이 수정 시도하는 리뷰 내용입니다")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> reviewService.deleteReview(product.getId(), review.reviewId(), other.getId()))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void deleteReview_allows_rewriting_review_for_same_product() {
        User user = createUser("review4@momentive.com");
        Product product = createProduct("사료");
        createPaidOrder(user, product);
        ReviewCreateResponse review = reviewService.createReview(product.getId(), user.getId(),
                newRequest(4, "괜찮은 상품이었습니다 만족해요"));

        reviewService.deleteReview(product.getId(), review.reviewId(), user.getId());
        Product reloadedAfterDelete = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloadedAfterDelete.getReviewCount()).isEqualTo(0);
        assertThat(reloadedAfterDelete.getAverageRating()).isNull();

        ReviewCreateResponse rewritten = reviewService.createReview(product.getId(), user.getId(),
                newRequest(2, "다시 작성하는 리뷰 내용입니다 재구매 후기"));
        assertThat(rewritten.rating()).isEqualTo(2);
    }

    @Test
    void getReviews_returns_reviews_ordered_by_latest_with_pagination() throws InterruptedException {
        Product product = createProduct("사료");
        for (int i = 0; i < 3; i++) {
            User user = createUser("reviewer" + i + "@momentive.com");
            createPaidOrder(user, product);
            reviewService.createReview(product.getId(), user.getId(), newRequest(3, "리뷰 내용 " + i + " 번째 작성입니다"));
            Thread.sleep(5);
        }

        ReviewListResponse firstPage = reviewService.getReviews(product.getId(), 0, 2, null);
        assertThat(firstPage.reviews()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(firstPage.reviews().get(0).text()).contains("2");
        assertThat(firstPage.reviews().get(1).text()).contains("1");
        assertThat(firstPage.reviews()).allMatch(r -> !r.isMine());

        ReviewListResponse secondPage = reviewService.getReviews(product.getId(), 1, 2, null);
        assertThat(secondPage.reviews()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void getMyReview_returns_null_when_no_review_exists_but_purchase_verified() {
        User user = createUser("review5@momentive.com");
        Product product = createProduct("사료");
        createPaidOrder(user, product);

        MyReviewResponse response = reviewService.getMyReview(product.getId(), user.getId());

        assertThat(response).isNull();
    }
}
