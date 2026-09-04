package com.momentive.backend.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public ProductImage(Product product, String url, Integer displayOrder) {
        this.product = product;
        this.url = url;
        this.displayOrder = displayOrder;
    }

    /**
     * 노출 순서만 갱신한다. 같은 URL이 그대로 남는 수정에서 행을 다시 만들지 않고
     * 순서만 바꿔, 이미지 id가 수정 전후로 유지되게 하기 위한 진입점이다.
     */
    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
