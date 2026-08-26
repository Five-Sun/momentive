package com.momentive.backend.product.service;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;

public enum ProductSort {
    NEW,
    POPULAR,
    PRICE_ASC,
    PRICE_DESC;

    public static ProductSort from(String raw) {
        try {
            return ProductSort.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_SORT);
        }
    }
}
