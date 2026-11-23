package com.tigernet.backend.payment.service;

import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.user.service.PageAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Quyền theo hướng phiếu (bổ sung cho @RequiresPage trên controller):
 * - tạo / huỷ phiếu thu cần trang "Tạo phiếu thu", phiếu chi cần "Tạo phiếu chi";
 * - xem phiếu chi cần "Tạo phiếu chi" hoặc "Phiếu nhập" (vd Bán hàng chỉ xem phiếu thu).
 */
@Component
@RequiredArgsConstructor
public class PaymentAccess {
    private static final String[] VIEW_OUT_PAGES = {Pages.PAYMENTS_NEW_OUT, Pages.IMPORTS};

    private final PageAccessPolicy pageAccessPolicy;

    public void requireWrite(PaymentDirection direction) {
        pageAccessPolicy.requireAnyPage(direction == PaymentDirection.OUT ? Pages.PAYMENTS_NEW_OUT : Pages.PAYMENTS_NEW_IN);
    }

    public void requireView(PaymentDirection direction) {
        if (direction == PaymentDirection.OUT) {
            pageAccessPolicy.requireAnyPage(VIEW_OUT_PAGES);
        }
    }

    /** hướng dùng để lọc danh sách: không chọn hướng mà không được xem phiếu chi thì chỉ trả phiếu thu */
    public PaymentDirection listDirection(PaymentDirection requested) {
        if (requested == null) {
            return pageAccessPolicy.currentUserCanOpenAny(VIEW_OUT_PAGES) ? null : PaymentDirection.IN;
        }
        requireView(requested);
        return requested;
    }
}
