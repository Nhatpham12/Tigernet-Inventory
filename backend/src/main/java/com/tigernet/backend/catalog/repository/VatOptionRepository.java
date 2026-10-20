package com.tigernet.backend.catalog.repository;

import com.tigernet.backend.catalog.entity.VatOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VatOptionRepository extends JpaRepository<VatOptionEntity, Integer> {
    boolean existsByVat(String vat);

    boolean existsByVatAndIdNot(String vat, Integer id);

    // mức VAT đã có phiếu xuất dùng thì không được sửa Rate (bản in phiếu cũ sẽ sai)
    @Query("select count(e) > 0 from ExportReceiptEntity e where e.vat.id = :vatId")
    boolean isUsedByExportReceipts(Integer vatId);
}
