package com.momentive.backend.product.repository;

import com.momentive.backend.product.domain.Category;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 고객용 상품 목록. 판매중(ON_SALE)이 아닌 상품은 노출하지 않는다.
     *
     * <p>{@code q}는 상품명 부분일치(대소문자 무시) 검색어이며 {@code null}이면 조건이 빠진다.
     * 검색을 DB에서 수행하므로 프론트가 앞쪽 N개만 받아 거르던 캡이 사라진다.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = com.momentive.backend.product.domain.ProductStatus.ON_SALE
              AND (:category IS NULL OR p.category = :category)
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Product> findOnSaleProducts(
            @Param("category") Category category, @Param("q") String q, Pageable pageable);

    /**
     * 관리자용 상품 목록. 상태 필터를 명시적으로 받아 {@code DELETED}까지 조회할 수 있다.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.status IN :statuses
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Product> findForAdmin(
            @Param("statuses") Collection<ProductStatus> statuses, @Param("q") String q, Pageable pageable);
}
