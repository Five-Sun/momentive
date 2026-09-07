package com.momentive.backend.product.domain;

/**
 * 상품의 판매 상태. 관리자의 "의도"를 표현하며, 품절 여부(사실)는
 * {@link Product#isSoldOut()}이 variant 재고 합에서 파생 판정한다.
 */
public enum ProductStatus {

    /** 판매중. 고객 목록·검색·상세에 노출된다. */
    ON_SALE,

    /** 판매 중단. 고객 화면에서 사라지지만 재고는 남아 다시 ON_SALE로 되돌릴 수 있다. */
    HIDDEN,

    /** soft delete. 행은 지우지 않아 기존 주문 이력에는 계속 정상적으로 보인다. */
    DELETED
}
