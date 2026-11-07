package com.tigernet.backend.printing.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thông tin doanh nghiệp (tenant) in ở đầu/cuối mọi biểu mẫu.
 * P1: lấy từ cấu hình (backend/.env → tigernet.company.*); P2: lấy từ tenant_branding của Control Plane.
 * Trường nào để trống thì biểu mẫu in dòng chấm để ghi tay.
 */
@ConfigurationProperties(prefix = "tigernet.company")
public record CompanyProperties(
        String name,            // tên pháp lý, VD "CÔNG TY TNHH ..."
        String englishName,     // tên giao dịch / tiếng Anh (biên bản bàn giao)
        String address,
        String taxCode,
        String phone,
        String email,
        String website,
        String logo,            // đường dẫn file ảnh logo (tuỳ chọn), VD file:/opt/tigernet/logo.png hoặc classpath:print/logo.png
        String representative,  // người đại diện (biên bản đối chiếu công nợ)
        String representativeTitle,
        String warrantyAddress, // địa chỉ tiếp nhận bảo hành
        String hotline,
        String slogan           // dòng chân trang biên bản bàn giao
) {
    public String warrantyAddressOrAddress() {
        return warrantyAddress != null && !warrantyAddress.isBlank() ? warrantyAddress : address;
    }
}
