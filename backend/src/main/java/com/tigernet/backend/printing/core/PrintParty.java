package com.tigernet.backend.printing.core;

import com.tigernet.backend.partner.entity.PartyEntity;

/** Đối tác in trên phiếu (khách, NCC, người nộp/nhận tiền). */
public record PrintParty(String name, String taxCode, String nationalId, String address, String phone, String email) {
    public static PrintParty of(PartyEntity p) {
        if (p == null) {
            return new PrintParty(null, null, null, null, null, null);
        }
        return new PrintParty(p.getFullName(), p.getTaxCode(), p.getNationalId(), p.getAddress(), p.getPhone(), p.getEmail());
    }
}
