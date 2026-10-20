package com.tigernet.backend.partner.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.partner.dto.request.ReceiverRequest;
import com.tigernet.backend.partner.dto.response.ReceiverResponse;
import com.tigernet.backend.partner.entity.ReceiverEntity;
import com.tigernet.backend.partner.mapper.PartnerMapper;
import com.tigernet.backend.partner.repository.ReceiverRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// người nhận hàng đã lưu của khách
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ReceiverService {
    ReceiverRepository receiverRepository;
    CustomerService customerService;
    PartnerMapper partnerMapper;

    @Transactional(readOnly = true)
    public List<ReceiverResponse> getReceivers(Integer customerId) {
        customerService.findCustomer(customerId);
        return partnerMapper.toReceiverResponses(
                receiverRepository.findByCustomerIdAndIsDeletedFalseOrderByReceiverName(customerId));
    }

    public ReceiverResponse createReceiver(Integer customerId, ReceiverRequest request) {
        ReceiverEntity receiver = partnerMapper.toReceiver(request);
        receiver.setCustomer(customerService.findCustomer(customerId));
        return partnerMapper.toReceiverResponse(receiverRepository.save(receiver));
    }

    public ReceiverResponse updateReceiver(Integer receiverId, ReceiverRequest request) {
        ReceiverEntity receiver = findReceiver(receiverId);
        partnerMapper.updateReceiver(receiver, request);
        return partnerMapper.toReceiverResponse(receiverRepository.save(receiver));
    }

    // xóa mềm: phiếu cũ còn giữ snapshot người nhận
    public void deleteReceiver(Integer receiverId) {
        ReceiverEntity receiver = findReceiver(receiverId);
        receiver.setIsDeleted(true);
        receiverRepository.save(receiver);
    }

    public ReceiverEntity findReceiver(Integer receiverId) {
        return receiverRepository.findByIdAndIsDeletedFalse(receiverId)
                .orElseThrow(() -> new AppException(ErrorCode.RECEIVER_NOT_FOUND));
    }

    // người nhận được chọn trên phiếu phải thuộc đúng khách của phiếu
    public ReceiverEntity findReceiverOfCustomer(Integer receiverId, Integer customerId) {
        ReceiverEntity receiver = findReceiver(receiverId);
        if (!receiver.getCustomer().getId().equals(customerId)) {
            throw new AppException(ErrorCode.RECEIVER_NOT_OF_CUSTOMER);
        }
        return receiver;
    }
}
