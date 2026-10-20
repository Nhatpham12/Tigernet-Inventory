package com.tigernet.backend.alert.mapper;

import com.tigernet.backend.alert.dto.response.AlertResponse;
import com.tigernet.backend.alert.entity.AlertEntity;
import com.tigernet.backend.common.config.MapstructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface AlertMapper {
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    AlertResponse toAlertResponse(AlertEntity alert);

    List<AlertResponse> toAlertResponses(List<AlertEntity> alerts);
}
