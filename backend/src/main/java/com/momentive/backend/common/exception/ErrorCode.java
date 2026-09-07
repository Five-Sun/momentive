package com.momentive.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    INVALID_SORT(HttpStatus.BAD_REQUEST, "잘못된 정렬 옵션입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족한 상품이 있습니다."),
    STOCK_CONFLICT(HttpStatus.CONFLICT, "재고 처리 중 충돌이 발생했습니다. 다시 시도해주세요."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.PAYMENT_REQUIRED, "결제 승인에 실패했습니다."),
    ORDER_NOT_PENDING(HttpStatus.CONFLICT, "이미 처리된 주문입니다."),
    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT, "취소할 수 없는 주문 상태입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.PAYMENT_REQUIRED, "결제 금액이 일치하지 않습니다."),
    PURCHASE_NOT_VERIFIED(HttpStatus.FORBIDDEN, "구매 이력이 확인된 사용자만 리뷰를 작성할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 이 상품에 작성한 리뷰가 있습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려견을 찾을 수 없습니다."),
    COUPON_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 쿠폰 코드입니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "유효기간이 지난 쿠폰입니다."),
    COUPON_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "이미 등록한 쿠폰입니다."),
    USER_COUPON_NOT_FOUND(HttpStatus.BAD_REQUEST, "보유하지 않은 쿠폰입니다."),
    USER_COUPON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "사용할 수 없는 쿠폰입니다."),
    COUPON_MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "최소 주문금액을 충족하지 않는 쿠폰입니다."),
    VARIANT_REQUIRED(HttpStatus.BAD_REQUEST, "사이즈·재고를 최소 1개 이상 입력해야 합니다."),
    DUPLICATE_VARIANT_SIZE(HttpStatus.BAD_REQUEST, "같은 사이즈 이름을 중복해서 등록할 수 없습니다."),
    VARIANT_IN_USE(HttpStatus.BAD_REQUEST, "이미 주문에 사용된 사이즈는 삭제할 수 없습니다. 재고를 0으로 두세요."),
    VARIANT_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 상품에 존재하지 않는 사이즈입니다."),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 5장까지 등록할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
