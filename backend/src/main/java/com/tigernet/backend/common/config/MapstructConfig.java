package com.tigernet.backend.common.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

// cấu hình chung cho mọi mapper: thiếu ánh xạ field nào là lỗi biên dịch, tránh quên cột
@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapstructConfig {
}
