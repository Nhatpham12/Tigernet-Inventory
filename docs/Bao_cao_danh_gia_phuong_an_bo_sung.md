# BÁO CÁO ĐÁNH GIÁ CÁC PHƯƠNG ÁN BỔ SUNG CHO TIGERNET

- Kính gửi: Ban lãnh đạo
- Về việc: đánh giá 9 đề xuất bổ sung (Act-as, báo lỗi IT, phiếu lương PIN, chốt đơn và giao lại việc, chấm công, đăng nhập Microsoft, khách bom hàng, CRM, UCD)
- Ngày lập: 10/11/2026
- Tài liệu kèm theo: SRS v1.2, chương 9 (`docs/SRS_He_Thong_Quan_Ly_Kho_v1_2.docx`), sơ đồ trong `docs/so-do-bo-sung/`

## 1. Tóm tắt cho lãnh đạo

> **Vì sao có báo cáo này:** đã có 9 đề xuất bổ sung cho TIGERNET. Mỗi đề xuất đều có ích, nhưng dự án đang do **1 người** làm và còn khoảng **7 tuần** đến hạn nghiệm thu giai đoạn 1 (27/12/2026). Làm hết cùng lúc sẽ trễ hạn. Báo cáo này trả lời ba câu hỏi: **cái nào làm được, cái nào nên làm trước, và cần lãnh đạo quyết điều gì.**

**Kết luận chính**

1. **Cả 9 đề xuất đều làm được** với công nghệ hiện tại (Flutter, Spring Boot, MySQL). Không đề xuất nào cần đổi nền tảng.
2. **6 đề xuất nên làm, ít rủi ro:** báo lỗi IT, Act-as, đăng nhập Microsoft, khách bom hàng, chốt đơn và giao lại việc, UCD.
3. **2 đề xuất làm được nhưng cần lãnh đạo đồng ý mở rộng phạm vi sang nhân sự:** chấm công online và phiếu lương có PIN. Riêng phần **tự tính lương** (thuế thu nhập cá nhân, bảo hiểm xã hội) **không nên tự làm**; chỉ nhập bảng lương từ Excel hoặc phần mềm kế toán.
4. **CRM đầy đủ làm được nhưng lớn nhất** (khoảng 2 đến 3 tháng công), nên chia 3 đợt, khớp với kế hoạch CRM quý 2/2027 đã đề ra trước đó.
5. **Tổng công sức: khoảng 110 đến 140 ngày công** (5 đến 7 tháng nếu 1 người làm). Muốn xong trước tháng 6/2027 (hạn hoàn thành ghi trong SRS) thì phải **thêm 1 lập trình viên** hoặc **dời bớt một số việc ERP** (sổ quỹ, tuổi nợ) đã xếp vào quý 1/2027.
6. **Trước 27/12/2026 chỉ nên thêm 2 việc nhỏ** giúp chính việc nghiệm thu: nút báo lỗi bản tối giản và một vòng cho người dùng thật thử 3 màn hình chính (UCD). Mọi việc khác bắt đầu từ tháng 1/2027.

**Bảng tổng hợp**

| # | Đề xuất | Khả thi? | Công sức (ngày công) | Nên làm khi nào | Mức ưu tiên |
| --- | --- | --- | --- | --- | --- |
| 1 | Báo lỗi cho phòng IT (ticket, check-done/deferred/interrupted, hot update) | Khả thi | 5 (tối giản) + 6 (đầy đủ) + 6 đến 8 (quy trình phát hành tự động) | Tối giản: trước 27/12. Đầy đủ: tháng 2/2027 | Cao |
| 2 | UCD (thiết kế lấy người dùng làm trung tâm) | Khả thi, chủ yếu là cách làm | 3 (vòng đầu) + khoảng 10% thời gian mỗi đợt | Bắt đầu ngay | Cao |
| 3 | Act-as (xem với vai trò khác) | Khả thi | 5 (cần làm trước 3 đến 4 ngày kiểm tra quyền ở máy chủ, việc vốn bắt buộc của giai đoạn 1) | Tháng 1/2027 | Cao |
| 4 | Đăng nhập bằng tài khoản Microsoft của tổ chức | Khả thi | 6 đến 8; đồng bộ tự động danh sách nhân viên thêm 5 đến 7 (để sau) | Tháng 1/2027 | Trung bình, cao nếu khách đầu tiên dùng Microsoft 365 |
| 5 | Xử lý khách bom hàng | Khả thi | 7 đến 9 | Tháng 1/2027 | Cao nếu bán giao tận nơi |
| 6 | Chốt đơn, chuyển cấp trên, giao lại việc tự động | Khả thi | 8 đến 10 | Tháng 2/2027 | Cao |
| 7 | Hồ sơ nhân viên, chấm công online | Khả thi có điều kiện (mở rộng phạm vi) | 10 đến 12 | Tháng 3/2027 nếu được duyệt | Trung bình |
| 8 | Phiếu lương có mã PIN | Khả thi có điều kiện (mở rộng phạm vi; không tự tính lương) | 3 (PIN) + 5 đến 6 (nhập và xem phiếu lương) | Tháng 3/2027 nếu được duyệt | Trung bình |
| 9 | CRM đầy đủ | Khả thi, khối lượng lớn | 45 đến 60, chia 3 đợt | Quý 2/2027 | Cao về kinh doanh, nhưng sau các việc trên |

## 2. Hiện trạng dự án (để so sánh)

> **Vì sao cần mục này:** một đề xuất "khả thi" hay không phụ thuộc vào việc phần mềm hiện đã có gì để dùng lại và còn nợ gì chưa làm. Mục này tóm tắt điểm xuất phát.

**Đã có (nhánh `dev`, ngày 10/11/2026)**

- Nghiệp vụ kho: sản phẩm, nhà cung cấp, khách hàng, nhập, xuất, cho thuê, cho mượn, trả hàng, chuyển kho, kiểm kê, bảo hành, đổi trả hàng bán, trả hàng nhà cung cấp.
- Tiền: phiếu thu, phiếu chi, đối chiếu công nợ, in 11 biểu mẫu theo Thông tư 200.
- Người dùng: 5 vai trò, quản lý nhân viên, phân quyền từng trang, phân kho, nhật ký thao tác.
- Kiến trúc: ứng dụng web Flutter, lớp trung gian (BFF), máy chủ Spring Boot, MySQL; thiết kế sẵn mô hình mỗi khách hàng một cơ sở dữ liệu.

**Còn nợ của giai đoạn 1 (ảnh hưởng trực tiếp đến các đề xuất)**

| Việc còn thiếu | Ảnh hưởng đến đề xuất nào |
| --- | --- |
| Máy chủ chưa kiểm tra vai trò cho API nghiệp vụ (mới chặn trên giao diện) | Act-as (phải chặn ghi ở máy chủ), bảo mật nói chung |
| Chưa có API xác nhận phiếu nhập, phiếu xuất, hoàn tất giao | Chốt đơn, khách bom hàng |
| Chưa có quy trình phát hành tự động (CI/CD), chưa có máy chủ thử nghiệm | Bản vá nóng (hot update) |
| Chưa định tuyến theo từng khách (tenant) | Đăng nhập Microsoft theo tên miền khách |
| Chưa gửi được email OTP | Quên PIN, quên mật khẩu |
| Chưa có tác vụ chạy định kỳ | Giao lại việc tự động, nhắc hạn ticket, nhắc chấm công |

**Nguồn lực:** 1 người làm toàn bộ (cơ sở dữ liệu, giao diện, máy chủ, kiểm thử, triển khai). Từ nay đến 27/12/2026 còn khoảng 33 ngày làm việc.

## 3. Cách đánh giá

| Tiêu chí | Câu hỏi |
| --- | --- |
| Phù hợp | Có giải quyết vấn đề thật của khách dùng TIGERNET (doanh nghiệp có kho, bán, cho thuê)? |
| Khả thi kỹ thuật | Làm được với công nghệ đang dùng, không phải đổi nền tảng? |
| Dùng lại được | Phần mềm đã có bao nhiêu phần để dựa vào? |
| Công sức | Bao nhiêu ngày công cho 1 người, gồm cả kiểm thử? |
| Phụ thuộc | Cần việc nào khác làm xong trước? |
| Rủi ro | Pháp lý, bảo mật, phạm vi hợp đồng, người dùng có chấp nhận không? |

**Mức kết luận:** **Khả thi** = nên làm, rủi ro thấp. **Khả thi có điều kiện** = làm được nhưng cần lãnh đạo quyết một điều kiện trước. **Không nên làm** = làm được nhưng chi phí hoặc rủi ro lớn hơn lợi ích.

## 4. Đánh giá từng phương án

### 4.1 Báo lỗi cho phòng IT (ticket)

- **Lợi ích:** mỗi lỗi có số phiếu, có ảnh chụp và thời điểm; IT không phải hỏi lại; lãnh đạo thấy được bao nhiêu lỗi, sửa mất bao lâu. Đặc biệt có ích ngay trong đợt nghiệm thu tháng 12.
- **Đã có:** nhật ký thao tác, cơ chế cảnh báo.
- **Còn thiếu:** toàn bộ chức năng ticket; quy trình phát hành tự động cho bản vá nóng.
- **Công sức:** bản tối giản (nút báo lỗi, chụp màn hình, danh sách và đổi trạng thái) 5 ngày; bản đầy đủ (gián đoạn, tạm hoãn, nhắc hạn, người báo xác nhận, báo cáo) thêm 6 ngày; bản vá nóng cần dựng quy trình phát hành tự động 6 đến 8 ngày (việc này vốn có trong kế hoạch triển khai).
- **Rủi ro:** ảnh chụp có thể lộ giá nhập, công nợ, nên phải cho người dùng xem trước và xóa ảnh. Bản vá nóng phải giữ quy tắc: chỉ chủ dự án mới duyệt đưa lên nhánh `main`.
- **Kết luận: Khả thi.** Làm bản tối giản trước 27/12/2026.

### 4.2 UCD (thiết kế lấy người dùng làm trung tâm)

- **Lợi ích:** người dùng là thủ kho, bán hàng, kế toán, ít rành máy tính. Cho họ thử sớm giúp phát hiện chỗ dễ bấm nhầm trước khi phát hành, giảm sai tồn kho thật và giảm chi phí sửa về sau.
- **Đã có:** thiết kế Figma, bản demo offline bấm thử được, bảng kiểm tra giao diện, sơ đồ use case.
- **Còn thiếu:** chân dung người dùng, buổi thử với người dùng thật, số đo (thời gian làm xong, số lần bấm sai, điểm hài lòng SUS).
- **Công sức:** vòng đầu 3 ngày (soạn kịch bản, thử với 5 người, tổng hợp); về sau khoảng 10% thời gian mỗi đợt.
- **Rủi ro:** cần khách hoặc nhân viên thật dành thời gian; nếu không có người thử, UCD chỉ còn trên giấy.
- **Kết luận: Khả thi, nên áp dụng ngay**, gắn với đợt nghiệm thu tháng 12.

### 4.3 Act-as (xem với vai trò khác)

- **Lợi ích:** quản trị viên tự xem đúng màn hình của nhân viên khi có khiếu nại "không thấy nút", không phải hỏi mật khẩu. Có ích cho kiểm thử và hỗ trợ.
- **Đã có:** phân quyền trang, phân kho, nhật ký.
- **Còn thiếu:** máy chủ chưa kiểm tra vai trò cho API nghiệp vụ. Nếu chưa làm phần này thì chế độ "chỉ xem" chỉ là ẩn nút, không an toàn.
- **Công sức:** 5 ngày cho Act-as, sau khi đã có kiểm tra quyền ở máy chủ (3 đến 4 ngày, việc vốn phải làm của giai đoạn 1).
- **Rủi ro:** bị lạm dụng để xem dữ liệu riêng. Đã khống chế: bắt buộc lý do, chỉ xem, giới hạn 30 phút, ghi nhật ký, không mở được phiếu lương, nhân viên xem được ai đã xem thay mình.
- **Kết luận: Khả thi.** Làm tháng 1/2027. **Không** làm chế độ "thao tác thay" (sửa dữ liệu dưới tên người khác) vì rủi ro pháp lý và gian lận.

### 4.4 Đăng nhập bằng tài khoản Microsoft của tổ chức

- **Lợi ích:** với khách dùng Microsoft 365 (ví dụ tên miền theenest.edu.vn), Admin Microsoft của tổ chức đã có toàn quyền với tài khoản `aaa@theenest.edu.vn`: tạo, khóa, đặt lại mật khẩu, bắt xác thực hai lớp. Cho đăng nhập TIGERNET bằng chính tài khoản này thì nhân viên nghỉ việc chỉ cần khóa một chỗ, nhân viên không phải nhớ thêm mật khẩu, bảo mật dựa vào Microsoft.
- **Đã có:** SRS 1.1 đã ghi yêu cầu "đăng nhập bằng mail công ty"; máy chủ đã dùng token chuẩn; bảng quản lý khách hàng (tenants) đã có để khai báo tên miền.
- **Còn thiếu:** kết nối với Microsoft Entra ID, liên kết tài khoản, chọn đúng khách theo tên miền.
- **Công sức:** 6 đến 8 ngày. Đồng bộ tự động danh sách nhân viên, nhóm từ Microsoft (SCIM): thêm 5 đến 7 ngày, để sau.
- **Rủi ro:** cần một tài khoản thử thật của tổ chức và Admin Microsoft bấm "Đồng ý" một lần. Phải làm rõ: Admin Microsoft quản tài khoản, còn vai trò và kho trong TIGERNET vẫn do quản trị viên TIGERNET gán. Hai quyền này không tự động trùng nhau.
- **Kết luận: Khả thi.** Làm tháng 1/2027, sớm hơn nếu khách đầu tiên dùng Microsoft 365.

### 4.5 Xử lý khách bom hàng

- **Lợi ích:** thu hồi hàng về kho đúng số lượng, ghi đúng chi phí vận chuyển hai chiều, chặn trước khách hay bom bằng cọc hoặc danh sách đen.
- **Đã có:** tiền cọc trên phiếu xuất, phiếu gửi hàng, phiếu thu chi, trả hàng bán, trạng thái serial.
- **Còn thiếu:** trạng thái giao hàng sau khi xuất kho, lịch sử từng lần giao, đếm số lần bom, danh sách đen theo số điện thoại.
- **Công sức:** 7 đến 9 ngày (dùng lại luồng trả hàng bán cho hàng hoàn).
- **Rủi ro:** nếu đã xuất hóa đơn điện tử thì phải lập hóa đơn điều chỉnh; cần kế toán xác nhận cách hạch toán. Danh sách đen theo số điện thoại là dữ liệu cá nhân, chỉ dùng nội bộ.
- **Kết luận: Khả thi.** Làm tháng 1/2027 nếu khách đầu tiên bán hàng giao tận nơi, trả tiền khi nhận.

### 4.6 Chốt đơn, chuyển cấp trên và giao lại việc tự động

- **Lợi ích:** mọi đơn đã chốt đi qua cấp trên; đơn, khách, hàng giữ của người nghỉ không bị bỏ rơi; có lịch sử ai đã giao việc cho ai.
- **Đã có:** phiếu xuất 3 trạng thái, người tạo phiếu, lệnh giữ hàng, cảnh báo.
- **Còn thiếu:** người trên quyền của nhân viên, người phụ trách đơn, duyệt đơn, lịch sử giao lại, tác vụ chạy định kỳ. Giao lại tự động khi vắng mặt cần dữ liệu chấm công (4.7); nếu chưa có chấm công thì vẫn giao lại được theo ngày nghỉ việc và theo quá hạn không thao tác.
- **Công sức:** 8 đến 10 ngày.
- **Rủi ro:** luật giao lại sai có thể đẩy đơn cho người không quen khách, nên luôn báo người trên quyền và cho đổi lại bằng tay.
- **Kết luận: Khả thi.** Làm tháng 2/2027 cùng với báo giá, đơn bán (đã có trong kế hoạch ERP quý 1/2027).

### 4.7 Hồ sơ nhân viên và chấm công online

- **Lợi ích:** biết ai đang làm, ai vắng, ai đã nghỉ việc; có ngày công cho phiếu lương; là đầu vào cho giao lại việc tự động.
- **Đã có:** tài khoản người dùng, kho, cờ đang hoạt động.
- **Còn thiếu:** ngày vào làm, ngày nghỉ việc, ca làm, chấm công, đơn từ, bảng công.
- **Công sức:** 10 đến 12 ngày.
- **Rủi ro:**
  - **Phạm vi:** TIGERNET đang bán như phần mềm quản lý kho; hợp đồng mẫu (Phụ lục 01) chưa có phân hệ nhân sự. Thêm vào là mở rộng phạm vi, ảnh hưởng giá bán và hạn hoàn thành.
  - **Kỹ thuật:** ứng dụng đang chạy trên trình duyệt; định vị GPS trên trình duyệt kém chính xác và dễ giả. Nên xác minh bằng mạng Wi-Fi hoặc địa chỉ IP của công ty là chính.
  - **Pháp lý:** vị trí là dữ liệu cá nhân (Nghị định 13/2023/NĐ-CP), phải xin đồng ý và chỉ lấy lúc bấm chấm công.
  - **Cạnh tranh:** khách có thể đã dùng máy chấm công hoặc phần mềm nhân sự khác; khi đó chỉ cần nhập file chấm công vào.
- **Kết luận: Khả thi có điều kiện.** Lãnh đạo quyết có mở rộng sang nhân sự hay không. Nếu không, chỉ làm phần nhỏ: thêm ngày vào làm, ngày nghỉ việc, người trên quyền (2 ngày) để phục vụ giao lại việc.

### 4.8 Phiếu lương có mã PIN

- **Lợi ích:** nhân viên xem lương của mình trên app; lớp PIN chống người khác xem trộm trên máy dùng chung.
- **Đã có:** mã hóa bcrypt, khóa sau nhiều lần sai.
- **Còn thiếu:** dữ liệu lương, gửi email OTP, mã hóa số tiền.
- **Công sức:** PIN 3 ngày; nhập bảng lương từ Excel và xem phiếu lương 5 đến 6 ngày.
- **Rủi ro:** lương là dữ liệu nhạy cảm nhất; lộ một lần là mất uy tín. **Tự tính lương** (thuế thu nhập cá nhân, bảo hiểm, phụ cấp) đòi hỏi kiến thức luật và cập nhật thường xuyên: ước tính trên 30 ngày công và rủi ro sai luật cao.
- **Kết luận: Khả thi có điều kiện.** Đi cùng quyết định mở rộng nhân sự. Chỉ làm "nhập bảng lương có sẵn và cho xem bằng PIN"; **không nên tự tính lương**.

### 4.9 CRM đầy đủ

- **Lợi ích:** công ty nắm khách thay vì từng nhân viên nắm; biết đơn sắp tới từ đâu; chăm sóc khách cũ để bán lại; đây là phần giúp TIGERNET bán được giá cao hơn một phần mềm kho thuần.
- **Đã có:** nửa sau vòng đời khách: danh bạ chung, khách hàng, phiếu xuất, phiếu thuê, công nợ, bảo hành, đổi trả, cảnh báo hạn thuê.
- **Còn thiếu:** nửa đầu: khách tiềm năng, cơ hội, báo giá, lịch sử chăm sóc, người phụ trách, phân nhóm, khiếu nại, chương trình chăm sóc, báo cáo phễu.
- **Công sức:** 45 đến 60 ngày, chia 3 đợt:
  - Đợt CRM-1 (khoảng 20 ngày): khách tiềm năng, người phụ trách, người liên hệ, lịch sử chăm sóc, việc cần làm, hồ sơ khách 360 độ.
  - Đợt CRM-2 (15 đến 20 ngày): cơ hội bán hàng, báo giá, duyệt giảm giá, báo cáo phễu.
  - Đợt CRM-3 (12 đến 15 ngày): khiếu nại, phân nhóm tự động, chương trình chăm sóc qua email (Zalo để sau).
- **Rủi ro:** khối lượng lớn với 1 người; cần chốt khách hàng mục tiêu (bán lẻ hay doanh nghiệp) vì quy trình CRM khác nhau.
- **Kết luận: Khả thi.** Làm quý 2/2027 theo 3 đợt.

## 5. Lộ trình đề xuất

> **Vì sao sắp xếp như vậy:** việc nào giúp nghiệm thu thì làm trước; việc nào làm nền cho việc khác (kiểm tra quyền ở máy chủ, người trên quyền, tác vụ định kỳ) làm sớm; việc nào cần lãnh đạo quyết phạm vi thì để sau khi có quyết định.

| Đợt | Thời gian | Nội dung | Công sức |
| --- | --- | --- | --- |
| Đợt 0 | Từ nay đến 27/12/2026 | Ưu tiên hoàn thành giai đoạn 1 (kể cả kiểm tra quyền ở máy chủ, API xác nhận phiếu). Thêm: nút báo lỗi bản tối giản; một vòng UCD thử 3 màn hình nhập, xuất, kiểm kê với người dùng thật | 8 ngày |
| Đợt 1 | Tháng 1/2027 | Gửi email; Act-as; đăng nhập Microsoft; khách bom hàng | 20 đến 24 ngày |
| Đợt 2 | Tháng 2/2027 | Tác vụ định kỳ; người trên quyền, chốt đơn, giao lại việc; ticket bản đầy đủ; quy trình phát hành tự động và bản vá nóng | 24 đến 26 ngày |
| Đợt 3 | Tháng 3/2027 (nếu duyệt mở rộng nhân sự) | Hồ sơ nhân viên, chấm công; phiếu lương có PIN | 18 đến 19 ngày |
| Đợt 4 | Tháng 4 đến 6/2027 | CRM-1, CRM-2, CRM-3 | 47 đến 55 ngày |
| Liên tục | Mỗi đợt | UCD: thử với người dùng thật trước khi phát hành | Khoảng 10% |

**Lưu ý về sức chứa:** kế hoạch ERP ngày 06/11/2026 đã xếp vào quý 1/2027 các việc báo giá, đơn bán, bảng giá, tuổi nợ, hạn mức công nợ, sổ quỹ tiền mặt và ngân hàng. Báo giá và đơn bán đã gộp vào Đợt 2 và CRM-2 ở trên. Các việc còn lại (tuổi nợ, hạn mức, sổ quỹ) **không có chỗ** trong lộ trình này nếu chỉ có 1 người; cần lãnh đạo chọn dời chúng hay thêm người.

## 6. Danh sách việc cần triển khai

| Mã việc | Việc | Thuộc đề xuất | Đợt | Ngày công | Phụ thuộc |
| --- | --- | --- | --- | --- | --- |
| T-01 | Kiểm tra vai trò ở máy chủ cho mọi API nghiệp vụ | Nền tảng, Act-as | 0 | 3 đến 4 | Không (vốn thuộc giai đoạn 1) |
| T-02 | Nút báo lỗi, tự chụp màn hình, ghi thông tin tự động | Ticket | 0 | 3 | Không |
| T-03 | Danh sách ticket, đổi trạng thái 10 đến 90 | Ticket | 0 | 2 | T-02 |
| T-04 | Vòng UCD đầu: kịch bản, thử 5 người, báo cáo | UCD | 0 | 3 | Bản demo hiện có |
| T-05 | Tác vụ chạy định kỳ dùng chung (nhắc hạn, giao lại) | Nền tảng | 2 | 2 | Không |
| T-06 | Gửi email (OTP, thông báo) | Nền tảng | 1 | 2 | Không |
| T-07 | Act-as: token xem thay, chặn ghi ở máy chủ, dải cảnh báo, nhật ký | Act-as | 1 | 5 | T-01 |
| T-08 | Đăng nhập Microsoft: đăng ký ứng dụng, đăng nhập, liên kết tài khoản, tên miền theo khách | Microsoft | 1 | 6 đến 8 | Tài khoản thử của tổ chức |
| T-09 | Trạng thái giao hàng, lịch sử lần giao, nhận hàng hoàn, phiếu chi vận chuyển | Bom hàng | 1 | 5 đến 6 | API hoàn tất xuất kho |
| T-10 | Hồ sơ rủi ro khách, danh sách đen, cảnh báo khi tạo đơn, báo cáo | Bom hàng | 1 | 2 đến 3 | T-09 |
| T-11 | Người trên quyền, ngày vào làm, ngày nghỉ việc trong hồ sơ người dùng | Chốt đơn, nhân sự | 2 | 2 | Không |
| T-12 | Chốt đơn, duyệt, trả lại, giao người khác; lịch sử giao việc | Chốt đơn | 2 | 5 | T-11, API xác nhận phiếu xuất |
| T-13 | Giao lại tự động (nghỉ việc, vắng, quá hạn), luật chọn người | Chốt đơn | 2 | 3 | T-05, T-12 |
| T-14 | Ticket đầy đủ: gián đoạn, tạm hoãn, nhắc hạn, xác nhận check-done, báo cáo | Ticket | 2 | 6 | T-03, T-05 |
| T-15 | Quy trình phát hành tự động, máy chủ thử nghiệm, kiểm tra phiên bản để báo tải lại | Ticket (hot update) | 2 | 6 đến 8 | Máy chủ thuê |
| T-16 | Ca làm, chấm công, địa điểm hợp lệ, đơn từ, bảng công | Nhân sự | 3 | 10 | T-11, quyết định của lãnh đạo |
| T-17 | PIN: tạo, nhập, khóa, quên PIN qua OTP | Phiếu lương | 3 | 3 | T-06 |
| T-18 | Nhập bảng lương từ Excel, công bố, xem phiếu lương, mã hóa số tiền | Phiếu lương | 3 | 5 đến 6 | T-17, quyết định của lãnh đạo |
| T-19 | CRM-1: khách tiềm năng, người phụ trách, liên hệ, lịch sử chăm sóc, việc cần làm, hồ sơ 360 | CRM | 4 | 20 | T-05, T-11 |
| T-20 | CRM-2: cơ hội, báo giá, duyệt giảm giá, báo cáo phễu | CRM | 4 | 15 đến 20 | T-19, T-12 |
| T-21 | CRM-3: khiếu nại, phân nhóm, chương trình chăm sóc qua email | CRM | 4 | 12 đến 15 | T-19, T-06 |
| T-22 | Cập nhật thiết kế cơ sở dữ liệu, script SQL, tài liệu API, bộ kiểm thử cho từng đợt | Chung | Mỗi đợt | Đã tính trong từng việc | Không |

## 7. Những việc không nên làm

| Việc | Lý do |
| --- | --- |
| Tự tính lương, thuế thu nhập cá nhân, bảo hiểm xã hội | Luật thay đổi thường xuyên, sai là ảnh hưởng quyền lợi người lao động; nên nhập từ Excel hoặc phần mềm kế toán (MISA, Fast) |
| Act-as có quyền sửa dữ liệu thay người khác | Không còn biết ai thật sự đã làm; dễ bị dùng để gian lận |
| Tự làm trang quản trị tài khoản Microsoft trong TIGERNET | Admin Microsoft đã có công cụ của Microsoft; TIGERNET chỉ cần đăng nhập và đọc thông tin cơ bản |
| Chấm công bằng nhận diện khuôn mặt hoặc theo dõi vị trí liên tục | Chi phí cao, dữ liệu sinh trắc học và vị trí liên tục có rủi ro pháp lý lớn |
| Gửi Zalo tự động trong đợt CRM đầu | Cần đăng ký Zalo OA và chi phí theo tin nhắn; làm email trước |

## 8. Đề nghị lãnh đạo quyết định

1. **Có mở rộng TIGERNET sang nhân sự (chấm công, phiếu lương) không?** Đề xuất: chỉ làm khi có khách cụ thể yêu cầu; trước mắt chỉ thêm ngày vào làm, ngày nghỉ việc, người trên quyền (2 ngày).
2. **Thêm 1 lập trình viên từ tháng 1/2027, hay dời các việc ERP còn lại (tuổi nợ, hạn mức công nợ, sổ quỹ) sang sau tháng 6/2027?** Đề xuất: thêm người nếu muốn có CRM trước tháng 6/2027.
3. **Khách hàng đầu tiên có dùng Microsoft 365 không, có bán hàng giao tận nơi trả tiền khi nhận không?** Câu trả lời quyết định đăng nhập Microsoft và bom hàng làm ở Đợt 1 hay dời sau.
4. **Ticket báo lỗi do ai xử lý: IT của khách hay đội phát triển TIGERNET?** Đề xuất: lưu tập trung ở máy chủ quản lý của TIGERNET; khách có IT riêng thì cho IT của khách xem và tiếp nhận trước.
5. **Đồng ý cho thử với người dùng thật (UCD) trong đợt nghiệm thu tháng 12** và cử 1 người mỗi vai trò tham gia khoảng 1 giờ.

## 9. Rủi ro chung và cách giảm

| Rủi ro | Mức | Cách giảm |
| --- | --- | --- |
| Trễ hạn 27/12/2026 vì thêm việc | Cao | Chỉ thêm 8 ngày việc nhỏ ở Đợt 0; mọi thứ khác từ tháng 1/2027 |
| Một người làm, nghỉ ốm là dừng dự án | Cao | Thêm người hoặc giữ tài liệu, kiểm thử tự động đầy đủ để người khác tiếp được |
| Mở rộng phạm vi mà hợp đồng chưa có | Trung bình | Bổ sung phụ lục hợp đồng hoặc bán thành gói riêng |
| Lộ dữ liệu nhạy cảm (lương, ảnh chụp lỗi, vị trí) | Trung bình | PIN, mã hóa, cho xóa ảnh trước khi gửi, xin đồng ý trước khi lấy vị trí, nhật ký truy cập |
| Người dùng không chịu dùng chức năng mới | Trung bình | UCD: cho thử trước, sửa theo phản hồi |

## Phụ lục: tài liệu liên quan

| Tài liệu | Nội dung |
| --- | --- |
| `docs/SRS_He_Thong_Quan_Ly_Kho_v1_2.docx` | SRS bản 1.2, chương 9 mô tả chi tiết 9 nhóm yêu cầu |
| `docs/SRS_v1_2_Bo_sung.md` | Bản gốc dạng văn bản của chương 9 |
| `docs/so-do-bo-sung/` | 11 sơ đồ (nguồn PlantUML và ảnh PNG) |
| `docs/Database_Design_v7.md` | Thiết kế cơ sở dữ liệu hiện tại |
| `docs/BACKLOG.md` | Danh sách việc giai đoạn 1 |
