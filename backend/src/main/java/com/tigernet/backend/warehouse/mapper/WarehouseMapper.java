package com.tigernet.backend.warehouse.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.warehouse.dto.request.WarehouseCreationRequest;
import com.tigernet.backend.warehouse.dto.request.WarehouseLocationRequest;
import com.tigernet.backend.warehouse.dto.request.WarehouseUpdateRequest;
import com.tigernet.backend.warehouse.dto.response.UserWarehouseResponse;
import com.tigernet.backend.warehouse.dto.response.WarehouseLocationResponse;
import com.tigernet.backend.warehouse.dto.response.WarehouseResponse;
import com.tigernet.backend.warehouse.entity.UserWarehouseEntity;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.entity.WarehouseLocationEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface WarehouseMapper {

    // manager, createdAt do service tự xử lý
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    WarehouseEntity toWarehouse(WarehouseCreationRequest request);

    @Mapping(target = "managerId", source = "manager.userId")
    @Mapping(target = "managerName", source = "manager.fullName")
    WarehouseResponse toWarehouseResponse(WarehouseEntity warehouse);

    List<WarehouseResponse> toWarehouseResponses(List<WarehouseEntity> warehouses);

    // trường nào request gửi null thì giữ nguyên giá trị cũ
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "warehouseCode", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateWarehouse(@MappingTarget WarehouseEntity warehouse, WarehouseUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "warehouse", ignore = true)
    WarehouseLocationEntity toLocation(WarehouseLocationRequest request);

    @Mapping(target = "warehouseId", source = "warehouse.id")
    WarehouseLocationResponse toLocationResponse(WarehouseLocationEntity location);

    List<WarehouseLocationResponse> toLocationResponses(List<WarehouseLocationEntity> locations);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "warehouse", ignore = true)
    void updateLocation(@MappingTarget WarehouseLocationEntity location, WarehouseLocationRequest request);

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "warehouseName", source = "warehouse.warehouseName")
    UserWarehouseResponse toUserWarehouseResponse(UserWarehouseEntity userWarehouse);

    List<UserWarehouseResponse> toUserWarehouseResponses(List<UserWarehouseEntity> userWarehouses);
}
