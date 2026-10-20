package com.tigernet.backend.salesreturn.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.salesreturn.dto.request.ReturnFeePolicyRequest;
import com.tigernet.backend.salesreturn.dto.response.ReturnFeePolicyResponse;
import com.tigernet.backend.salesreturn.entity.ReturnFeePolicyEntity;
import com.tigernet.backend.salesreturn.enums.FeeActionType;
import com.tigernet.backend.salesreturn.mapper.SalesReturnMapper;
import com.tigernet.backend.salesreturn.repository.ReturnFeePolicyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ma trận phí đổi/trả: đổi chính sách chỉ cần sửa dữ liệu, không sửa code
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ReturnFeePolicyService {
    ReturnFeePolicyRepository policyRepository;
    SalesReturnMapper salesReturnMapper;

    @Transactional(readOnly = true)
    public List<ReturnFeePolicyResponse> getPolicies() {
        return salesReturnMapper.toPolicyResponses(policyRepository.findAllByOrderByMaxDaysAscActionTypeAsc());
    }

    public ReturnFeePolicyResponse createPolicy(ReturnFeePolicyRequest request) {
        ReturnFeePolicyEntity policy = salesReturnMapper.toPolicy(request);
        if (policy.getIsAllowed() == null) policy.setIsAllowed(true);
        return salesReturnMapper.toPolicyResponse(policyRepository.save(policy));
    }

    public ReturnFeePolicyResponse updatePolicy(Integer policyId, ReturnFeePolicyRequest request) {
        ReturnFeePolicyEntity policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_FEE_POLICY_NOT_FOUND));
        salesReturnMapper.updatePolicy(policy, request);
        if (policy.getIsAllowed() == null) policy.setIsAllowed(true);
        return salesReturnMapper.toPolicyResponse(policyRepository.save(policy));
    }

    // không có dòng nào khớp (VD quá 30 ngày) = không được đổi/trả
    @Transactional(readOnly = true)
    public Optional<ReturnFeePolicyEntity> findApplicablePolicy(FeeActionType actionType, int days, LocalDate onDate) {
        return policyRepository.findCandidates(actionType, days, onDate).stream().findFirst();
    }
}
