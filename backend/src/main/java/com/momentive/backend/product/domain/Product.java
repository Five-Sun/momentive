package com.momentive.backend.product.domain;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
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
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private Boolean soldOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer stock;

    @Version
    private Long version;

    private Double averageRating;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<ProductImage> images = new ArrayList<>();

    public Product(String name, String description, Integer price, Integer discountPrice, Boolean soldOut, Category category) {
        this(name, description, price, discountPrice, soldOut, category, 0);
    }

    public Product(String name, String description, Integer price, Integer discountPrice, Boolean soldOut, Category category, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.soldOut = soldOut;
        this.category = category;
        this.createdAt = LocalDateTime.now();
        this.stock = stock;
    }

    public void addImage(String url, int displayOrder) {
        images.add(new ProductImage(this, url, displayOrder));
    }

    public String getThumbnailUrl() {
        return images.isEmpty() ? null : images.get(0).getUrl();
    }

    /**
     * 재고를 차감한다. 부족하면 {@link ErrorCode#OUT_OF_STOCK}을 던진다.
     * 동시성 제어는 {@code @Version} 낙관적 락 + Service 레벨 재시도로 처리한다.
     */
    public void deductStock(int quantity) {
        if (this.stock < quantity) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
    }

    public void restoreStock(int quantity) {
        this.stock += quantity;
    }

    /**
     * 리뷰 작성/수정/삭제 시점에 평점 집계를 동기로 재계산해 반영한다.
     */
    public void updateRatingSummary(Double averageRating, Integer reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }
}
