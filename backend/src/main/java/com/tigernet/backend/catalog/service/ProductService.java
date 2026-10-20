package com.tigernet.backend.catalog.service;

import com.tigernet.backend.catalog.dto.request.ProductCreationRequest;
import com.tigernet.backend.catalog.dto.request.ProductUpdateRequest;
import com.tigernet.backend.catalog.dto.response.ProductResponse;
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.catalog.mapper.CatalogMapper;
import com.tigernet.backend.catalog.repository.ProductRepository;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductService {
    ProductRepository productRepository;
    ProductGroupService productGroupService;
    CatalogMapper catalogMapper;

    public ProductResponse createProduct(ProductCreationRequest request) {
        if (productRepository.existsByProductNameAndGroupId(request.getProductName(), request.getGroupId())) {
            throw new AppException(ErrorCode.PRODUCT_EXISTED);
        }
        ProductEntity product = catalogMapper.toProduct(request);
        product.setGroup(productGroupService.findGroup(request.getGroupId()));
        // giá trị mặc định giống DEFAULT trong schema
        if (product.getDefaultPriceIn() == null) product.setDefaultPriceIn(BigDecimal.ZERO);
        if (product.getDefaultPriceOut() == null) product.setDefaultPriceOut(BigDecimal.ZERO);
        if (product.getIsSerialized() == null) product.setIsSerialized(true);
        if (product.getMinStockLevel() == null) product.setMinStockLevel(0);
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(Integer groupId, String keyword) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return catalogMapper.toProductResponses(productRepository.search(groupId, normalizedKeyword));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Integer productId) {
        return catalogMapper.toProductResponse(findProduct(productId));
    }

    public ProductResponse updateProduct(Integer productId, ProductUpdateRequest request) {
        ProductEntity product = findProduct(productId);
        String newName = request.getProductName() != null ? request.getProductName() : product.getProductName();
        Integer newGroupId = request.getGroupId() != null ? request.getGroupId() : product.getGroup().getId();
        if (productRepository.existsByProductNameAndGroupIdAndIdNot(newName, newGroupId, productId)) {
            throw new AppException(ErrorCode.PRODUCT_EXISTED);
        }
        catalogMapper.updateProduct(product, request);
        if (request.getGroupId() != null) {
            product.setGroup(productGroupService.findGroup(request.getGroupId()));
        }
        return catalogMapper.toProductResponse(productRepository.save(product));
    }

    // xóa mềm: phiếu cũ vẫn tham chiếu được sản phẩm
    public void deleteProduct(Integer productId) {
        ProductEntity product = findProduct(productId);
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    // dùng cho module khác: chỉ trả sản phẩm chưa xóa
    public ProductEntity findProduct(Integer productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
