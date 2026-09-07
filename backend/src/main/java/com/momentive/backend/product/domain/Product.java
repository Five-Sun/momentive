package com.momentive.backend.product.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    private Integer discountPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 이름·가격 등의 동시 수정을 막기 위해 유지한다.
     * 재고는 {@link ProductVariant}로 옮겨갔으므로 재고 재시도에는 더 이상 쓰이지 않는다.
     */
    @Version
    private Long version;

    private Double averageRating;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<ProductImage> images = new ArrayList<>();

    // 목록 조회에서 상품마다 variant를 한 건씩 조회하는 N+1을 피하기 위해 batch fetch로 묶는다.
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    @BatchSize(size = 100)
    private List<ProductVariant> variants = new ArrayList<>();

    public Product(String name, String description, Integer price, Integer discountPrice, Category category) {
        this(name, description, price, discountPrice, category, ProductStatus.ON_SALE);
    }

    public Product(String name, String description, Integer price, Integer discountPrice, Category category,
                   ProductStatus status) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.category = category;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public void addImage(String url, int displayOrder) {
        images.add(new ProductImage(this, url, displayOrder));
    }

    /**
     * 관리자 수정 시 기본 정보를 통째로 갱신한다.
     */
    public void update(String name, String description, Integer price, Integer discountPrice, Category category,
                       ProductStatus status) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.category = category;
        this.status = status;
    }

    /**
     * 이미지 목록을 요청받은 URL 배열로 맞춘다. 배열 순서가 그대로 {@code displayOrder}가 된다.
     *
     * <p>URL이 그대로 남은 이미지는 행을 다시 만들지 않고 재사용해 순서만 갱신한다.
     * 매번 전량 삭제 후 재생성하면 내용이 안 바뀐 수정에서도 이미지 id가 통째로 바뀌어,
     * 응답이 돌려주는 {@code ProductImageResponse.id}가 아무것도 식별하지 못하게 되기 때문이다.
     * 요청에 없는 기존 이미지만 목록에서 빠지며, {@code orphanRemoval}로 행까지 삭제된다.
     */
    public void replaceImages(List<String> urls) {
        // 같은 URL이 여러 번 들어와도 기존 행을 하나씩 소진하도록 URL별 큐로 모아 둔다.
        Map<String, Deque<ProductImage>> reusable = new HashMap<>();
        for (ProductImage image : images) {
            reusable.computeIfAbsent(image.getUrl(), key -> new ArrayDeque<>()).add(image);
        }

        List<ProductImage> added = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            Deque<ProductImage> candidates = reusable.get(urls.get(i));
            if (candidates == null || candidates.isEmpty()) {
                added.add(new ProductImage(this, urls.get(i), i));
            } else {
                candidates.poll().updateDisplayOrder(i);
            }
        }

        reusable.values().forEach(images::removeAll);
        images.addAll(added);
        // @OrderBy는 조회 시점에만 적용되므로, 같은 트랜잭션에서 조립되는 응답을 위해 직접 정렬해 둔다.
        images.sort(Comparator.comparing(ProductImage::getDisplayOrder));
    }

    /**
     * variant를 목록에서 제거한다. {@code orphanRemoval}로 실제 행까지 삭제되므로,
     * 주문에 사용된 적 있는 variant인지는 호출부(Service)가 먼저 확인해야 한다.
     */
    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
    }

    /**
     * soft delete. 행을 지우지 않아 기존 주문 이력에는 계속 정상적으로 보인다.
     */
    public void markDeleted() {
        this.status = ProductStatus.DELETED;
    }

    /**
     * 재고 단위인 variant를 추가한다. 모든 상품은 최소 1개의 variant를 가져야 하며,
     * 사이즈가 없는 상품은 {@code size = null}인 단일 variant로 표현한다.
     */
    public ProductVariant addVariant(String size, Integer stock) {
        ProductVariant variant = new ProductVariant(this, size, stock);
        variants.add(variant);
        return variant;
    }

    public String getThumbnailUrl() {
        return images.isEmpty() ? null : images.get(0).getUrl();
    }

    public int getTotalStock() {
        return variants.stream().mapToInt(ProductVariant::getStock).sum();
    }

    /**
     * 품절 여부는 컬럼이 아니라 variant 재고 합에서 파생 판정한다.
     * 관리자 의도({@link #status})와 사실(재고)을 분리하기 위함이다.
     */
    public boolean isSoldOut() {
        return getTotalStock() <= 0;
    }

    public boolean isOnSale() {
        return this.status == ProductStatus.ON_SALE;
    }

    /**
     * 리뷰 작성/수정/삭제 시점에 평점 집계를 동기로 재계산해 반영한다.
     */
    public void updateRatingSummary(Double averageRating, Integer reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }
}
