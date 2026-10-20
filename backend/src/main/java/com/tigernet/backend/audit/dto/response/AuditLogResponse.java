package com.tigernet.backend.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Integer userId;
    private String username;
    private String action;
    private String targetTable;
    private Integer targetId;
    private String detail; // chuỗi JSON
    private LocalDateTime createdAt;
}
