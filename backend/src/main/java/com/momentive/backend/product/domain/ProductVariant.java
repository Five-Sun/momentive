package com.momentive.backend.product.domain;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사이즈별 재고 단위. 재고 차감·복원과 낙관적 락은 모두 이 엔티티를 기준으로 수행한다.
 *
 * <p>사이즈가 없는 상품(간식·목줄 등)도 {@code size = null}인 단일 variant로 표현해
 * 재고 로직을 한 갈래로 유지한다. 덕분에 서로 다른 사이즈를 동시에 주문할 때
 * 불필요한 낙관적 락 충돌이 발생하지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 사이즈 이름. 사이즈가 없는 상품은 {@code null}이며 상품당 한 행만 존재한다. */
    @Column(length = 50)
    private String size;

    @Column(nullable = false)
    private Integer stock;

    @Version
    private Long version;

    ProductVariant(Product product, String size, Integer stock) {
        this.product = product;
        this.size = size;
        this.stock = stock;
    }

    /**
     * 관리자 수정 시 사이즈 이름과 재고를 갱신한다.
     * 재고 차감·복원(주문 흐름)과 달리 관리자가 값을 통째로 덮어쓰는 경로다.
     */
    public void update(String size, Integer stock) {
        this.size = size;
        this.stock = stock;
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

    public boolean hasEnoughStock(int quantity) {
        return this.stock >= quantity;
    }

    public boolean isSoldOut() {
        return this.stock <= 0;
    }

    /**
     * 주문 요청이 보낸 productId와 실제 소속 상품이 일치하는지 확인한다.
     * product는 LAZY 프록시여도 식별자 접근만으로는 초기화되지 않는다.
     */
    public boolean belongsTo(Long productId) {
        return this.product.getId().equals(productId);
    }
}
