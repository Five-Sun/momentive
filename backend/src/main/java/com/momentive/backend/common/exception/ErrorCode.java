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
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.PAYMENT_REQUIRED, "결제 금액이 일치하지 않습니다.");

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
