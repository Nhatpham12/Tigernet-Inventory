package com.tigernet.backend.inventory.repository;

import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockSummaryRepository extends JpaRepository<StockSummaryEntity, StockSummaryId> {
    // tạo dòng 0 cho (SP, kho) lần đầu có hàng; 2 transaction cùng tạo thì cái sau không làm gì.
    // Không dùng INSERT IGNORE vì nó nuốt cả lỗi khóa ngoại (id sai) thành cảnh báo.
    @Modifying
    @Query(value = """
            INSERT INTO Stock_Summary (Product_ID, Warehouse_ID, Last_Updated)
            VALUES (:productId, :warehouseId, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE Product_ID = Product_ID
            """, nativeQuery = true)
    void insertIfAbsent(Integer productId, Integer warehouseId);

    @Query("""
            select s from StockSummaryEntity s
            where (:warehouseId is null or s.id.warehouseId = :warehouseId)
              and (:productId is null or s.id.productId = :productId)
            order by s.id.warehouseId, s.id.productId
            """)
    List<StockSummaryEntity> search(Integer warehouseId, Integer productId);
}
