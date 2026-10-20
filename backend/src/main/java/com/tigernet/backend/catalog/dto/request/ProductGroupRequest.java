package com.tigernet.backend.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductGroupRequest {
    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name must be at most 100 characters")
    private String groupName;
}
