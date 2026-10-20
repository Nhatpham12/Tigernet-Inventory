package com.tigernet.backend.partner.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.partner.dto.response.PartyResponse;
import com.tigernet.backend.partner.service.PartyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Party được tạo/sửa qua /suppliers và /customers; ở đây chỉ tra cứu
@RestController
@RequestMapping("/parties")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartyController {
    PartyService partyService;

    @GetMapping // ?keyword=
    ApiResponse<List<PartyResponse>> searchParties(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(partyService.searchParties(keyword));
    }

    @GetMapping("/{partyId}")
    ApiResponse<PartyResponse> getParty(@PathVariable Integer partyId) {
        return ApiResponse.success(partyService.getParty(partyId));
    }
}
