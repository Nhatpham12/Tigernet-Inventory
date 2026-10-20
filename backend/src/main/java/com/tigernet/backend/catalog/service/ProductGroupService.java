package com.tigernet.backend.catalog.service;

import com.tigernet.backend.catalog.dto.request.ProductGroupRequest;
import com.tigernet.backend.catalog.dto.response.ProductGroupResponse;
import com.tigernet.backend.catalog.entity.ProductGroupEntity;
import com.tigernet.backend.catalog.mapper.CatalogMapper;
import com.tigernet.backend.catalog.repository.ProductGroupRepository;
import com.tigernet.backend.catalog.repository.ProductRepository;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductGroupService {
    ProductGroupRepository productGroupRepository;
    ProductRepository productRepository;
    CatalogMapper catalogMapper;

    // Group_Name UNIQUE tính cả nhóm đã xóa mềm
    public ProductGroupResponse createGroup(ProductGroupRequest request) {
        if (productGroupRepository.existsByGroupName(request.getGroupName())) {
            throw new AppException(ErrorCode.PRODUCT_GROUP_EXISTED);
        }
        ProductGroupEntity group = new ProductGroupEntity();
        group.setGroupName(request.getGroupName());
        return catalogMapper.toProductGroupResponse(productGroupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<ProductGroupResponse> getGroups() {
        return catalogMapper.toProductGroupResponses(productGroupRepository.findByIsDeletedFalseOrderByGroupName());
    }

    @Transactional(readOnly = true)
    public ProductGroupResponse getGroup(Integer groupId) {
        return catalogMapper.toProductGroupResponse(findGroup(groupId));
    }

    public ProductGroupResponse updateGroup(Integer groupId, ProductGroupRequest request) {
        ProductGroupEntity group = findGroup(groupId);
        if (productGroupRepository.existsByGroupNameAndIdNot(request.getGroupName(), groupId)) {
            throw new AppException(ErrorCode.PRODUCT_GROUP_EXISTED);
        }
        group.setGroupName(request.getGroupName());
        return catalogMapper.toProductGroupResponse(productGroupRepository.save(group));
    }

    // xóa mềm; còn sản phẩm trong nhóm thì không cho xóa
    public void deleteGroup(Integer groupId) {
        ProductGroupEntity group = findGroup(groupId);
        if (productRepository.existsByGroupIdAndIsDeletedFalse(groupId)) {
            throw new AppException(ErrorCode.PRODUCT_GROUP_IN_USE);
        }
        group.setIsDeleted(true);
        productGroupRepository.save(group);
    }

    public ProductGroupEntity findGroup(Integer groupId) {
        return productGroupRepository.findByIdAndIsDeletedFalse(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_GROUP_NOT_FOUND));
    }
}
