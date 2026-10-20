package com.tigernet.backend.catalog.service;

import com.tigernet.backend.catalog.dto.request.VatOptionRequest;
import com.tigernet.backend.catalog.dto.response.PaymentStatusResponse;
import com.tigernet.backend.catalog.dto.response.VatOptionResponse;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.entity.VatOptionEntity;
import com.tigernet.backend.catalog.mapper.CatalogMapper;
import com.tigernet.backend.catalog.repository.PaymentStatusRepository;
import com.tigernet.backend.catalog.repository.VatOptionRepository;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// danh mục tham chiếu: mức VAT, trạng thái thanh toán
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class LookupService {
    VatOptionRepository vatOptionRepository;
    PaymentStatusRepository paymentStatusRepository;
    CatalogMapper catalogMapper;

    @Transactional(readOnly = true)
    public List<VatOptionResponse> getVatOptions() {
        return catalogMapper.toVatOptionResponses(vatOptionRepository.findAll());
    }

    public VatOptionResponse createVatOption(VatOptionRequest request) {
        if (vatOptionRepository.existsByVat(request.getVat())) {
            throw new AppException(ErrorCode.VAT_OPTION_EXISTED);
        }
        return catalogMapper.toVatOptionResponse(vatOptionRepository.save(catalogMapper.toVatOption(request)));
    }

    public VatOptionResponse updateVatOption(Integer vatId, VatOptionRequest request) {
        VatOptionEntity vatOption = findVatOption(vatId);
        if (vatOptionRepository.existsByVatAndIdNot(request.getVat(), vatId)) {
            throw new AppException(ErrorCode.VAT_OPTION_EXISTED);
        }
        // đổi Rate của mức đã dùng sẽ làm sai bản in phiếu cũ -> phải thêm mức mới
        boolean rateChanged = !sameRate(vatOption, request);
        if (rateChanged && vatOptionRepository.isUsedByExportReceipts(vatId)) {
            throw new AppException(ErrorCode.VAT_RATE_IN_USE);
        }
        vatOption.setVat(request.getVat());
        vatOption.setRate(request.getRate());
        return catalogMapper.toVatOptionResponse(vatOptionRepository.save(vatOption));
    }

    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPaymentStatuses() {
        return catalogMapper.toPaymentStatusResponses(paymentStatusRepository.findAll());
    }

    public VatOptionEntity findVatOption(Integer vatId) {
        return vatOptionRepository.findById(vatId)
                .orElseThrow(() -> new AppException(ErrorCode.VAT_OPTION_NOT_FOUND));
    }

    // tên trạng thái lấy từ hằng số trong PaymentStatusEntity
    public PaymentStatusEntity findPaymentStatus(String paymentStatus) {
        return paymentStatusRepository.findByPaymentStatus(paymentStatus)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_STATUS_NOT_FOUND));
    }

    private boolean sameRate(VatOptionEntity vatOption, VatOptionRequest request) {
        if (vatOption.getRate() == null || request.getRate() == null) {
            return Objects.equals(vatOption.getRate(), request.getRate());
        }
        return vatOption.getRate().compareTo(request.getRate()) == 0;
    }
}
