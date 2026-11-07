package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.repository.PartyRepository;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintParty;
import com.tigernet.backend.printing.core.PrintResponses;
import com.tigernet.backend.printing.dto.DebtStatement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Biên bản đối chiếu công nợ (A4). */
@Service
@RequiredArgsConstructor
public class ReconciliationPrintService {
    private final PartyRepository partyRepository;
    private final DebtReconciliationService debtService;
    private final PdfRenderService renderer;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(Integer partyId, LocalDate from, LocalDate to, String format, boolean download) {
        if (partyId == null || from == null || to == null || to.isBefore(from)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }
        PartyEntity party = partyRepository.findById(partyId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTY_NOT_FOUND));
        DebtStatement s = debtService.compute(partyId, from, to);

        Map<String, Object> m = new HashMap<>();
        m.put("party", PrintParty.of(party));
        m.put("from", from);
        m.put("to", to);
        m.put("statement", s);
        return PrintResponses.of(renderer, "doi-chieu-cong-no", m,
                PrintResponses.fileName("Bien_ban_doi_chieu_cong_no", to), format, download);
    }
}
