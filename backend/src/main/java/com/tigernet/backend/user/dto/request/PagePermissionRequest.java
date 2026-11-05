package com.tigernet.backend.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagePermissionRequest {
    @NotNull(message = "Pages are required")
    private List<String> pages; // đường dẫn trang FE, vd "/app/products"
}
