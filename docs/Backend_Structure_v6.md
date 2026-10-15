# CẤU TRÚC BACKEND – TIGERNET (theo ERD v6 & Database_Design_v6)

> Stack hiện tại (`backend/pom.xml`): **Spring Boot 4.1, Java 21**, Spring Data JPA, Security, Validation, Mail, springdoc-openapi, MySQL, Lombok.
> Package gốc: `com.tigernet.backend`.
> Frontend là app **Flutter** riêng (`frontend/`) → backend chỉ là **REST API**.
> Dữ liệu: **ERD v6** (`Database_Design_v6.md`) trên **database riêng của từng tenant** + 1 database Control Plane (`database/control-plane/`). Redis giữ khóa đăng nhập, rate limit, refresh token, OTP. Phạm vi P1/P2 theo `BACKLOG.md`.

---

## 1. Cây cấu trúc thư mục

Tổ chức **theo tính năng (package-by-feature)**, bên trong mỗi tính năng chia lớp (`controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`). Lý do xem mục 2.

```
backend/
├── pom.xml
├── .env / .env.example
└── src/
    ├── main/
    │   ├── java/com/tigernet/backend/
    │   │   ├── BackendApplication.java
    │   │   │
    │   │   ├── common/                              # Dùng chung mọi module
    │   │   │   ├── config/
    │   │   │   │   ├── SecurityConfig.java           # JWT filter chain, phân quyền theo Role
    │   │   │   │   ├── OpenApiConfig.java            # springdoc / Swagger UI
    │   │   │   │   ├── RedisConfig.java              # login_fail, rate_limit (§3.1.1, §4.2)
    │   │   │   │   ├── StorageConfig.java            # MinIO/S3 cho ảnh SP, chứng từ NCC
    │   │   │   │   ├── JpaAuditingConfig.java        # tự điền Created_At / Created_By
    │   │   │   │   ├── TigernetProperties.java       # tigernet.rental.* (7 / 10 ngày), tigernet.auth.* (5 lần / 15 phút, OTP 10 phút)
    │   │   │   │   └── SchedulingConfig.java
    │   │   │   ├── security/
    │   │   │   │   ├── JwtTokenProvider.java         # claim: userId, role, tenantId
    │   │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   │   ├── RateLimitFilter.java          # Redis rate_limit:{user_id}:{yyyymmdd}
    │   │   │   │   ├── UserPrincipal.java            # kèm danh sách warehouseIds được phân công
    │   │   │   │   ├── CurrentUser.java              # annotation lấy user hiện tại
    │   │   │   │   └── WarehouseScopeGuard.java      # [v6] chặn thao tác ngoài User_Warehouses
    │   │   │   ├── entity/
    │   │   │   │   ├── BaseEntity.java               # ID, Created_At
    │   │   │   │   ├── AuditableEntity.java          # + Created_By
    │   │   │   │   └── SoftDeletableEntity.java      # Is_Deleted (@SQLRestriction)
    │   │   │   ├── exception/
    │   │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   │   ├── BusinessException.java
    │   │   │   │   ├── NotFoundException.java
    │   │   │   │   ├── InsufficientStockException.java
    │   │   │   │   └── ErrorCode.java
    │   │   │   ├── response/
    │   │   │   │   ├── ApiResponse.java
    │   │   │   │   └── PageResponse.java
    │   │   │   └── util/
    │   │   │       ├── ReceiptCodeGenerator.java     # PN-HN01-2026-0001 (Warehouse_Code làm tiền tố, SELECT … FOR UPDATE)
    │   │   │       └── MoneyUtils.java
    │   │   │
    │   │   ├── tenant/                               # Control Plane tối thiểu (P1 — BACKLOG G-11, OPS-01…04)
    │   │   │   ├── config/      ControlPlaneDataSourceConfig    # DataSource riêng tới DB control-plane
    │   │   │   │                TenantRoutingDataSource         # AbstractRoutingDataSource, cache pool theo tenant
    │   │   │   ├── context/     TenantContext (ThreadLocal), TenantResolverFilter (subdomain / claim tenantId)
    │   │   │   ├── service/     TenantService, TenantProvisioningService (tạo DB + Flyway V1–V3 + admin đầu tiên),
    │   │   │   │                TenantMigrationService (Flyway lần lượt từng tenant), SecretCipher (AES-GCM)
    │   │   │   ├── repository/  TenantRepository, TenantDatabaseRepository, TenantOperationRepository
    │   │   │   └── entity/      Tenant, TenantDatabase, TenantOperation   # bảng control-plane, không thuộc ERD v6
    │   │   │
    │   │   ├── auth/                                 # §3.1.1 đăng nhập, khóa 5 lần sai, OTP
    │   │   │   ├── controller/AuthController.java    # /identity/auth/*
    │   │   │   ├── service/
    │   │   │   │   ├── AuthService.java
    │   │   │   │   ├── LoginAttemptService.java      # Redis login_fail:{username}, TTL 15 phút
    │   │   │   │   ├── RefreshTokenService.java      # Redis refresh:{userId}:{tokenId}
    │   │   │   │   └── OtpService.java               # Redis otp:{email} TTL 10 phút, gửi mail qua spring-boot-starter-mail
    │   │   │   └── dto/  LoginRequest, LoginResponse, OtpVerifyRequest, ChangePasswordRequest
    │   │   │
    │   │   ├── user/                                 # Users, Roles
    │   │   │   ├── controller/  UserController, RoleController
    │   │   │   ├── service/     UserService, RoleService
    │   │   │   ├── repository/  UserRepository, RoleRepository
    │   │   │   ├── entity/      User, Role
    │   │   │   ├── enums/       RoleName (WAREHOUSE, ACCOUNTANT, SALES, MANAGER, ADMIN)
    │   │   │   ├── dto/         UserCreateRequest, UserUpdateRequest, UserResponse, RoleResponse
    │   │   │   └── mapper/      UserMapper
    │   │   │
    │   │   ├── warehouse/                            # [v6] Warehouses, User_Warehouses (P1 — QL-12, QT-07, CHUNG-07)
    │   │   │   ├── controller/  WarehouseController (/warehouses),
    │   │   │   │                WarehouseAssignmentController (/identity/users/{id}/warehouses)
    │   │   │   ├── service/     WarehouseService, UserWarehouseService
    │   │   │   ├── repository/  WarehouseRepository, UserWarehouseRepository
    │   │   │   ├── entity/      Warehouse, UserWarehouse, UserWarehouseId (@Embeddable)
    │   │   │   ├── dto/         WarehouseRequest, WarehouseResponse, AssignWarehouseRequest
    │   │   │   └── mapper/      WarehouseMapper
    │   │   │
    │   │   ├── catalog/                              # Danh mục & tham chiếu
    │   │   │   ├── controller/  ProductController, ProductGroupController, LookupController
    │   │   │   ├── service/     ProductService, ProductGroupService, LookupService
    │   │   │   ├── repository/  ProductRepository, ProductGroupRepository,
    │   │   │   │                VatOptionRepository, PaymentStatusRepository
    │   │   │   ├── entity/      Product, ProductGroup, VatOption, PaymentStatus
    │   │   │   ├── dto/         ProductRequest, ProductResponse, ProductFilter, GroupRequest ...
    │   │   │   └── mapper/      ProductMapper
    │   │   │
    │   │   ├── partner/                              # Parties + vai trò Supplier/Customer
    │   │   │   ├── controller/  PartyController, SupplierController, CustomerController,
    │   │   │   │                ReceiverController
    │   │   │   ├── service/     PartyService, SupplierService, CustomerService, ReceiverService
    │   │   │   ├── repository/  PartyRepository, SupplierRepository, SupplierBankAccountRepository,
    │   │   │   │                SupplierDocumentRepository, CustomerRepository, ReceiverRepository
    │   │   │   ├── entity/      Party, Supplier, SupplierBankAccount, SupplierDocument,
    │   │   │   │                Customer, Receiver
    │   │   │   ├── enums/       PartyType (INDIVIDUAL, ORGANIZATION)
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── inventory/                            # LÕI tồn kho — mọi phiếu đều gọi vào đây
    │   │   │   ├── controller/  InventoryController, StockSummaryController
    │   │   │   ├── service/
    │   │   │   │   ├── StockService.java             # API duy nhất để tăng/giảm/đổi trạng thái tồn
    │   │   │   │   ├── StockSummaryService.java      # cập nhật cache trong cùng transaction
    │   │   │   │   └── StockReconciliationJob.java   # @Scheduled đối soát Stock_Summary ↔ Inventory
    │   │   │   ├── repository/  InventoryRepository, StockSummaryRepository
    │   │   │   ├── entity/      Inventory, StockSummary, StockSummaryId (@Embeddable: product+warehouse)
    │   │   │   ├── enums/       InventoryStatus (IN_STOCK, SOLD, RENTED, LENT, DAMAGED,
    │   │   │   │                                 RESERVED, IN_TRANSIT)
    │   │   │   └── dto/         InventoryResponse, StockByWarehouseResponse, StockFilter
    │   │   │
    │   │   ├── inbound/                              # Phiếu nhập ("import" là từ khóa Java)
    │   │   │   ├── controller/  ImportReceiptController
    │   │   │   ├── service/     ImportReceiptService      # DRAFT→PENDING→CONFIRMED, ánh xạ ngược tồn khi sửa
    │   │   │   ├── repository/  ImportReceiptRepository, ImportReceiptDetailRepository
    │   │   │   ├── entity/      ImportReceipt, ImportReceiptDetail
    │   │   │   ├── enums/       ImportStatus
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── outbound/                             # Phiếu xuất / bán
    │   │   │   ├── controller/  ExportReceiptController
    │   │   │   ├── service/     ExportReceiptService      # kiểm tra tồn đúng kho, tính VAT/Remain/bảo hành,
    │   │   │   │                                          # ghi nhận thu tiền: cộng Paid + Audit_Logs PAYMENT (G-13)
    │   │   │   │                ExportPrintService        # in phiếu
    │   │   │   ├── repository/  ExportReceiptRepository, ExportReceiptDetailRepository
    │   │   │   ├── entity/      ExportReceipt, ExportReceiptDetail
    │   │   │   ├── enums/       ExportStatus (PENDING, CONFIRMED, EXPORTED)
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── rental/                               # Cho thuê/mượn + phiếu trả hàng thuê
    │   │   │   ├── controller/  RentalReceiptController, ReturnReceiptController
    │   │   │   ├── service/     RentalReceiptService, ReturnReceiptService,
    │   │   │   │                PenaltyCalculator         # quá hạn ≥10 ngày × giá/ngày
    │   │   │   ├── repository/  RentalReceiptRepository, RentalReceiptDetailRepository,
    │   │   │   │                ReturnReceiptRepository, ReturnReceiptDetailRepository
    │   │   │   ├── entity/      RentalReceipt, RentalReceiptDetail, ReturnReceipt, ReturnReceiptDetail
    │   │   │   ├── enums/       RentalType (RENT, LEND)   # phiếu có hiệu lực khi tạo, không có bước duyệt
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── salesreturn/                          # Đổi/trả hàng đã bán (P2)
    │   │   │   ├── controller/  SalesReturnController, ReturnFeePolicyController
    │   │   │   ├── service/     SalesReturnService        # REFUND/EXCHANGE/WARRANTY, sinh phiếu xuất mới
    │   │   │   │                ReturnFeeCalculator       # tra Return_Fee_Policy theo Days_Since_Delivery
    │   │   │   │                ReturnFeePolicyService
    │   │   │   ├── repository/  SalesReturnRepository, SalesReturnDetailRepository,
    │   │   │   │                ReturnFeePolicyRepository
    │   │   │   ├── entity/      SalesReturn, SalesReturnDetail, ReturnFeePolicy
    │   │   │   ├── enums/       ReturnType, ExchangeDirection, ReturnCondition,
    │   │   │   │                SalesReturnStatus, FeeActionType
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── transfer/                             # [v6] Chuyển kho (P2 — TK-17)
    │   │   │   ├── controller/  StockTransferController
    │   │   │   ├── service/     StockTransferService      # DRAFT→IN_TRANSIT→RECEIVED/CANCELLED
    │   │   │   ├── repository/  StockTransferRepository, StockTransferDetailRepository
    │   │   │   ├── entity/      StockTransfer, StockTransferDetail
    │   │   │   ├── enums/       TransferStatus
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── stocktake/                            # Kiểm kê + điều chỉnh tồn
    │   │   │   ├── controller/  StocktakeController
    │   │   │   ├── service/     StocktakeService          # chốt phiếu (= duyệt) → tự sinh Stock_Adjustments
    │   │   │   │                                          # và áp vào tồn; không có phiếu điều chỉnh độc lập
    │   │   │   ├── repository/  StocktakeRepository, StocktakeDetailRepository,
    │   │   │   │                StockAdjustmentRepository
    │   │   │   ├── entity/      Stocktake, StocktakeDetail, StockAdjustment
    │   │   │   ├── enums/       StocktakeStatus
    │   │   │   ├── dto/  ...
    │   │   │   └── mapper/  ...
    │   │   │
    │   │   ├── alert/                                # §3.8 cảnh báo
    │   │   │   ├── controller/  AlertController
    │   │   │   ├── service/     AlertService
    │   │   │   ├── job/         LowStockAlertJob, RentDueAlertJob (trước 7 ngày + quá hạn, RENT & LEND),
    │   │   │   │                PendingApprovalAlertJob, DebtOverdueAlertJob,
    │   │   │   │                TransferPendingAlertJob (P2)
    │   │   │   ├── repository/  AlertRepository
    │   │   │   ├── entity/      Alert
    │   │   │   └── enums/       AlertType
    │   │   │
    │   │   ├── audit/                                # Audit_Logs §3.1.2, §4.2
    │   │   │   ├── annotation/  Audited.java
    │   │   │   ├── aspect/      AuditAspect.java          # ghi log tự động bằng AOP
    │   │   │   ├── controller/  AuditLogController
    │   │   │   ├── service/     AuditLogService
    │   │   │   ├── repository/  AuditLogRepository
    │   │   │   └── entity/      AuditLog
    │   │   │
    │   │   ├── report/                               # §3.7, dashboard §7.1.2 (chỉ đọc)
    │   │   │   ├── controller/  ReportController, DashboardController
    │   │   │   ├── service/     InventoryReportService, DebtReportService, RevenueReportService
    │   │   │   ├── repository/  ReportQueryRepository     # native query trên các VIEW
    │   │   │   └── dto/         StockByWarehouseRow, CompanyStockRow, DebtRow ...
    │   │   │
    │   │   └── storage/                              # Ảnh SP, chứng từ NCC
    │   │       ├── controller/  FileController
    │   │       └── service/     FileStorageService, MinioFileStorageService
    │   │
    │   └── resources/
    │       ├── application.yaml
    │       ├── application-dev.yaml
    │       ├── application-prod.yaml
    │       ├── db/migration/                         # Flyway — chép từ database/ lúc build, KHÔNG sửa ở đây
    │       │   ├── tenant/
    │       │   │   ├── V1__init_schema.sql           # 34 bảng ERD v6
    │       │   │   ├── V2__seed_reference_data.sql   # Roles, Product_Groups, VAT, Payment_Statuses,
    │       │   │   │                                 # Return_Fee_Policy, kho MAIN
    │       │   │   └── V3__views.sql                 # View_Stock_By_Warehouse, View_Stock_Company_Total ...
    │       │   └── control-plane/
    │       │       └── V1__init_control_plane.sql    # tenants, tenant_databases, ...
    │       ├── static/                               # xem mục 3
    │       └── templates/                            # xem mục 3
    │
    └── test/java/com/tigernet/backend/
        ├── BackendApplicationTests.java
        ├── inventory/StockServiceTest.java
        ├── transfer/StockTransferServiceTest.java
        ├── salesreturn/ReturnFeeCalculatorTest.java
        └── outbound/ExportReceiptServiceTest.java
```

### Ghi chú về cách tổ chức
- **`inventory/StockService` là chỗ duy nhất được thay đổi tồn kho.** Các phiếu nhập, xuất, thuê, trả, đổi/trả, chuyển kho và kiểm kê đều gọi qua nó, nên `Inventory` và `Stock_Summary` luôn được cập nhật trong cùng một transaction (Database_Design_v6 mục 1.22).
- **Phân quyền theo kho [v6]** gom vào `WarehouseScopeGuard`, dùng danh sách kho của user trong `UserPrincipal` để lọc dữ liệu, thay vì mỗi service tự kiểm tra.
- **Không đặt tên package là `import`/`export`.** `import` là từ khóa Java nên không biên dịch được → dùng `inbound`/`outbound`.
- **Khóa ghép:** `UserWarehouse` và `StockSummary` dùng lớp `@Embeddable` làm ID.

- **Tên bảng/cột giữ đúng ERD** (`Import_Receipts`, `Receipt_Code`…). Đặt `spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl` và khai báo `@Table`/`@Column` đúng tên, vì MySQL trên Linux phân biệt hoa/thường tên bảng.
- **Nguồn migration là `database/`** (tenant + control-plane). `pom.xml` dùng `maven-resources-plugin` (copy-resources) chép vào `db/migration/` khi build; Flyway chạy `classpath:db/migration/tenant` cho từng tenant (do `TenantMigrationService` gọi), không chạy tự động lúc khởi động.

### Việc cần xử lý
1. **`pom.xml` còn thiếu:** `spring-boot-starter-data-redis`, `flyway-core` + `flyway-mysql`, thư viện JWT (`jjwt`), `spring-boot-starter-thymeleaf` + `openhtmltopdf` (in phiếu), Apache POI (xuất Excel). MinIO/S3 client để P2 (ảnh SP, tài liệu NCC).
2. **`application.yaml`:** bỏ `ddl-auto: update` → `validate`; datasource mặc định trỏ DB control-plane, DB tenant do `TenantRoutingDataSource` chọn theo request; thêm `spring.data.redis.*` và khối `tigernet.rental.*`, `tigernet.auth.*`.
3. **Schema đã thống nhất (06/10/2026):** `database/tenant/` viết lại theo ERD v6, cây trên khớp với nó.

---

## 2. Tổ chức theo lớp hay theo tính năng?

**Kết luận: theo tính năng**, bên trong mỗi tính năng vẫn chia lớp (`controller/`, `service/`, `repository/`, `entity/`, `dto/`).

### Vì sao hợp với TigerNet
1. **Dự án lớn.** v6 có 34 bảng (chưa kể Control Plane). Chia theo lớp thì `entity/` có gần 40 file, `service/` vài chục file nằm chung. Sửa nghiệp vụ "chuyển kho" phải mở 5–6 thư mục để tìm.
2. **Nghiệp vụ tách bạch rõ.** Nhập, xuất, thuê/trả, đổi/trả, chuyển kho, kiểm kê đều cùng khuôn: phiếu header, chi tiết, luồng trạng thái. Mỗi nghiệp vụ vừa vặn thành một package. Thêm nghiệp vụ mới (như chuyển kho ở v6) chỉ là thêm một thư mục.
3. **Làm một mình.** Quay lại một tính năng sau vài tuần, toàn bộ code của nó nằm ở một chỗ.
4. **Dễ kiểm soát phụ thuộc.** Các tính năng chỉ được thay đổi tồn kho qua `inventory/StockService`. Chia theo tính năng thì quy tắc này dễ nhìn và dễ giữ. Chia theo lớp thì service nào cũng gọi thẳng `InventoryRepository` được, dễ quên cập nhật `Stock_Summary`.
5. **Dễ tách ra sau này.** Nếu đi theo hướng SaaS (`database/control-plane/`), một module có thể tách thành service riêng mà không phải gỡ rối.

### Khi nào chia theo lớp vẫn ổn
Dự án nhỏ, khoảng dưới 10 entity, chủ yếu CRUD đơn giản. TigerNet đã vượt xa mức đó.

### Lưu ý khi chia theo tính năng
- Code dùng chung (security, exception, base entity, sinh mã phiếu) đặt trong `common/`, không để mỗi module tự viết lại.
- Module này không gọi thẳng repository của module khác, mà gọi qua service của module đó. Ví dụ: `outbound` gọi `StockService`, không gọi `InventoryRepository`.
- Không chia quá nhỏ. Bảng phụ như `Return_Fee_Policy` nằm chung với `salesreturn`, không cần module riêng.

---

## 3. Nội dung `static/` và `templates/`

Frontend là app Flutter riêng nên backend chỉ là REST API. Hai thư mục này chứa rất ít.

### `static/` — gần như để trống
Spring Boot phục vụ thẳng mọi file trong đây qua URL công khai, không cần đăng nhập.

**Có thể để:**
- `favicon.ico`
- Logo công ty, nếu email hoặc phiếu in cần nhúng qua URL

**Không được để:**
- Ảnh sản phẩm và chứng từ NCC. Theo v6, các file này lưu trên MinIO/S3, DB chỉ giữ đường dẫn. Đặt trong `static/` thì ai cũng tải được, và mỗi lần build lại có thể mất file.
- Bản build Flutter web. Nên deploy riêng (Nginx hoặc hosting tĩnh), không nhét vào jar backend.

### `templates/` — mẫu HTML sinh ở phía server
Chỉ dùng cho nội dung backend tự tạo ra, không phải màn hình của app:

```
templates/
├── mail/
│   ├── otp.html                  # mã OTP đăng nhập/quên mật khẩu (§3.1.1)
│   ├── account-locked.html       # thông báo khóa tài khoản 15 phút sau 5 lần sai
│   ├── welcome-user.html         # admin tạo tài khoản mới
│   └── alert-digest.html         # tổng hợp cảnh báo: tồn thấp, sắp hết hạn thuê (§3.8)
└── print/                        # phiếu in → render HTML → xuất PDF
    ├── import-receipt.html       # 1. phiếu nhập
    ├── export-receipt.html       # 2. phiếu xuất (§3.4.1 "in phiếu")
    ├── delivery-note.html        # 3. phiếu giao hàng (không giá, A5)
    ├── rental-receipt.html       # 4. phiếu thuê/mượn (RENT/LEND)
    ├── return-receipt.html       # 5. biên bản trả hàng thuê/mượn
    ├── stocktake.html            # 6. biên bản kiểm kê
    ├── stock-adjustment.html     # 7. phiếu điều chỉnh (sinh từ kiểm kê)
    ├── reconciliation.html       # 8. biên bản đối chiếu công nợ phải thu
    ├── sales-return.html         # 10. phiếu đổi/trả (P2)
    ├── stock-transfer.html       # 11. [v6] phiếu chuyển kho (P2)
    └── fragments/
        ├── header.html           # logo, tên công ty, Warehouse_Code + địa chỉ kho
        └── signature.html        # khu vực ký tên
```

### Cần làm thêm
- **Thêm thư viện vào `pom.xml`.** Hiện chưa có `spring-boot-starter-thymeleaf` nên `templates/` chưa dùng được. Muốn xuất phiếu ra PDF thì cần thêm `openhtmltopdf`.
- **Danh sách và bố cục biểu mẫu** theo `BIEU_MAU.md` (số thứ tự trong cây trên khớp số biểu mẫu; mẫu 9 phiếu thu/chi để P2).
- **Quyết định ai in phiếu.** Nếu để Flutter tự dựng phiếu in (ví dụ package `pdf`), có thể bỏ `templates/print/`, chỉ giữ `templates/mail/`. Khuyến nghị để backend in, vì phiếu là chứng từ: mẫu giữ ở một nơi và luôn đúng dữ liệu lúc lập phiếu, dù mở trên app di động, web hay Windows.
