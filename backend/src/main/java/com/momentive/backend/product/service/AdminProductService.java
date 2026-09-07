package com.momentive.backend.product.service;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.order.repository.OrderItemRepository;
import com.momentive.backend.product.domain.Product;
import com.momentive.backend.product.domain.ProductStatus;
import com.momentive.backend.product.domain.ProductVariant;
import com.momentive.backend.product.dto.admin.AdminProductListResponse;
import com.momentive.backend.product.dto.admin.AdminProductRequest;
import com.momentive.backend.product.dto.admin.AdminProductResponse;
import com.momentive.backend.product.dto.admin.AdminProductSummaryResponse;
import com.momentive.backend.product.dto.admin.AdminProductVariantRequest;
import com.momentive.backend.product.repository.ProductRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 상품 CRUD. 이미지와 variant는 요청 본문으로 전체 교체하며,
 * 삭제는 행을 지우지 않고 {@code status = DELETED}로 전이한다(주문 이력 보존).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    /** 이미지 최대 장수. 초과하면 {@code IMAGE_LIMIT_EXCEEDED}. */
    private static final int MAX_IMAGE_COUNT = 5;

    /** 목록 기본 필터: 삭제된 상품은 기본적으로 보이지 않는다. */
    public static final List<ProductStatus> DEFAULT_LIST_STATUSES =
            List.of(ProductStatus.ON_SALE, ProductStatus.HIDDEN);

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminProductListResponse getProducts(int page, int size, Collection<ProductStatus> statuses, String q) {
        Collection<ProductStatus> effectiveStatuses =
                (statuses == null || statuses.isEmpty()) ? DEFAULT_LIST_STATUSES : statuses;
        Page<Product> products = productRepository.findForAdmin(
                effectiveStatuses,
                ProductSearchKeyword.normalize(q),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return AdminProductListResponse.from(products.map(AdminProductSummaryResponse::from));
    }

    /**
     * 관리자 상세 조회. {@code DELETED} 상품도 조회할 수 있고, 없는 상품은
     * 빈 바디가 아니라 {@code PRODUCT_NOT_FOUND} 404로 명시 응답한다.
     */
    public AdminProductResponse getProduct(Long id) {
        return AdminProductResponse.from(findProduct(id));
    }

    @Transactional
    public AdminProductResponse createProduct(AdminProductRequest request) {
        List<NormalizedVariant> variants = normalizeVariants(request.variants());
        List<String> imageUrls = normalizeImageUrls(request.imageUrls());

        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.discountPrice(),
                request.category(),
                request.statusOrDefault());
        product.replaceImages(imageUrls);
        variants.forEach(variant -> product.addVariant(variant.size(), variant.stock()));

        // 이미지·variant는 IDENTITY PK라 flush 전에는 id가 비어 있다.
        // 응답이 id를 계약대로 채워 내보내도록 DTO 조립 전에 flush를 확정한다.
        productRepository.saveAndFlush(product);
        return AdminProductResponse.from(product);
    }

    @Transactional
    public AdminProductResponse updateProduct(Long id, AdminProductRequest request) {
        Product product = findProduct(id);
        List<NormalizedVariant> variants = normalizeVariants(request.variants());
        List<String> imageUrls = normalizeImageUrls(request.imageUrls());

        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.discountPrice(),
                request.category(),
                request.statusOrDefault());
        product.replaceImages(imageUrls);
        applyVariants(product, variants);

        // 이번 요청에서 새로 추가된 이미지·variant는 flush되어야 PK를 얻는다.
        // 이를 건너뛰면 수정 응답의 id가 null로 나가고, 관리자 폼이 그 응답을 그대로
        // 되돌려 보낼 때 기존 행이 삭제 후 재생성되어 VARIANT_IN_USE로 막힌다.
        productRepository.saveAndFlush(product);
        return AdminProductResponse.from(product);
    }

    /**
     * soft delete. 행을 지우지 않으므로 이 상품이 포함된 기존 주문 상세는 그대로 표시된다.
     */
    @Transactional
    public void deleteProduct(Long id) {
        findProduct(id).markDeleted();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    /**
     * 요청의 variant 목록으로 기존 variant를 전체 교체한다.
     * 요청에 없는 기존 variant는 삭제 대상이지만, 주문에 사용된 적이 있으면 이력이 끊기므로 거부한다.
     */
    private void applyVariants(Product product, List<NormalizedVariant> requested) {
        Map<Long, ProductVariant> existing = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Set<Long> keptIds = requested.stream()
                .map(NormalizedVariant::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (ProductVariant variant : List.copyOf(product.getVariants())) {
            if (keptIds.contains(variant.getId())) {
                continue;
            }
            if (orderItemRepository.existsByVariant_Id(variant.getId())) {
                throw new CustomException(ErrorCode.VARIANT_IN_USE);
            }
            product.removeVariant(variant);
        }

        for (NormalizedVariant variant : requested) {
            if (variant.id() == null) {
                product.addVariant(variant.size(), variant.stock());
                continue;
            }
            ProductVariant target = existing.get(variant.id());
            if (target == null) {
                throw new CustomException(ErrorCode.VARIANT_NOT_FOUND);
            }
            target.update(variant.size(), variant.stock());
        }
    }

    /**
     * variant 요청을 검증·정규화한다. 사이즈는 앞뒤 공백을 제거하고 빈 문자열은 {@code null}
     * ("사이즈 없음")로 맞춰, 클라이언트가 빈 칸을 어떻게 보내든 판정이 흔들리지 않게 한다.
     */
    private List<NormalizedVariant> normalizeVariants(List<AdminProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new CustomException(ErrorCode.VARIANT_REQUIRED);
        }

        List<NormalizedVariant> normalized = variants.stream()
                .map(variant -> new NormalizedVariant(variant.id(), blankToNull(variant.size()), variant.stock()))
                .toList();

        // HashSet은 null 원소를 허용하므로, "사이즈 없음"(null)도 한 번만 등장하는지 같은 규칙으로 검사된다.
        Set<String> seenSizes = new HashSet<>();
        for (NormalizedVariant variant : normalized) {
            if (!seenSizes.add(variant.size())) {
                throw new CustomException(ErrorCode.DUPLICATE_VARIANT_SIZE);
            }
        }
        return normalized;
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        List<String> urls = imageUrls.stream()
                .map(AdminProductService::blankToNull)
                .filter(Objects::nonNull)
                .toList();
        if (urls.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        return urls;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 검증을 통과해 정규화된 variant 입력. */
    private record NormalizedVariant(Long id, String size, Integer stock) {
    }
}
