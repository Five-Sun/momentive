package com.momentive.backend.product.service;

/**
 * 상품 검색어(`q`) 정규화. 고객 목록과 관리자 목록이 같은 규칙을 쓰도록 한 곳에 모아둔다.
 */
final class ProductSearchKeyword {

    private ProductSearchKeyword() {
    }

    /**
     * 앞뒤 공백을 제거하고, 비어 있으면 {@code null}로 바꿔 "검색어 없음"과 같게 취급한다.
     * 쿼리는 {@code :q IS NULL}이면 검색 조건을 통째로 빼므로 기존 동작이 그대로 유지된다.
     */
    static String normalize(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
