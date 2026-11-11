package com.tigernet.backend.warranty.repository;

import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Tra dòng bán để tiếp nhận bảo hành (tách riêng để không sửa module outbound). Chỉ phiếu đã xác nhận/xuất kho. */
public interface ExportLineLookupRepository extends JpaRepository<ExportReceiptDetailEntity, Integer> {
    @Query("""
            select d from ExportReceiptDetailEntity d left join d.inventory i
            where (d.serial = :serial or i.serial = :serial)
              and d.exportReceipt.status in (com.tigernet.backend.outbound.enums.ExportStatus.CONFIRMED,
                                             com.tigernet.backend.outbound.enums.ExportStatus.EXPORTED)
            order by d.exportReceipt.dateOut desc, d.id desc
            """)
    List<ExportReceiptDetailEntity> findBySerial(String serial);

    @Query("""
            select d from ExportReceiptDetailEntity d
            where d.exportReceipt.id = :exportReceiptId
              and d.exportReceipt.status in (com.tigernet.backend.outbound.enums.ExportStatus.CONFIRMED,
                                             com.tigernet.backend.outbound.enums.ExportStatus.EXPORTED)
            order by d.id
            """)
    List<ExportReceiptDetailEntity> findByExportReceipt(Integer exportReceiptId);
}
