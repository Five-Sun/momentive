package com.momentive.backend.product.controller;

import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.product.domain.ProductStatus;
import com.momentive.backend.product.dto.admin.AdminProductListResponse;
import com.momentive.backend.product.dto.admin.AdminProductRequest;
import com.momentive.backend.product.dto.admin.AdminProductResponse;
import com.momentive.backend.product.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 상품 API. {@code /admin/**}은 {@code SecurityConfig}에서 {@code hasRole("ADMIN")}으로
 * 막혀 있어, 권한 없는 호출은 이 컨트롤러에 도달하기 전에 403으로 끊긴다.
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class AdminProductController {

    private final AdminProductService adminProductService;

    @Operation(summary = "[관리자] 상품 목록 조회")
    @GetMapping
    public AdminProductListResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "조회할 판매 상태(복수 지정 가능). 기본값은 ON_SALE,HIDDEN — DELETED는 제외된다")
            @RequestParam(defaultValue = "ON_SALE,HIDDEN") List<ProductStatus> status,
            @Parameter(description = "상품명 검색어(부분일치, 대소문자 무시)")
            @RequestParam(required = false) String q
    ) {
        return adminProductService.getProducts(page, size, status, q);
    }

    @Operation(summary = "[관리자] 상품 상세 조회 (DELETED 포함)")
    @GetMapping("/{id}")
    public AdminProductResponse getProduct(@PathVariable Long id) {
        return adminProductService.getProduct(id);
    }

    @Operation(summary = "[관리자] 상품 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse createProduct(@Valid @RequestBody AdminProductRequest request) {
        return adminProductService.createProduct(request);
    }

    @Operation(summary = "[관리자] 상품 수정 (이미지·사이즈 전체 교체)")
    @PutMapping("/{id}")
    public AdminProductResponse updateProduct(
            @PathVariable Long id, @Valid @RequestBody AdminProductRequest request) {
        return adminProductService.updateProduct(id, request);
    }

    @Operation(summary = "[관리자] 상품 삭제 (status = DELETED로 전이, 행은 삭제하지 않음)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        adminProductService.deleteProduct(id);
    }
}
