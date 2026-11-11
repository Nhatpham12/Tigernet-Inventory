import 'package:flutter_test/flutter_test.dart';
import 'package:tigernet_app/features/admin/audit_log_page.dart';

void main() {
  group('Chi tiết lịch sử thao tác', () {
    test('thao tác qua API được đọc thành câu, không còn JSON', () {
      const detail = '{"method":"PUT","path":"/suppliers/5"}';
      expect(detailSummary(detail), 'Cập nhật Nhà cung cấp #5');
      expect(detailRows(detail).first, (
        'Thao tác',
        null,
        'Cập nhật Nhà cung cấp #5',
      ));
    });

    test('sửa nhân viên chỉ liệt kê trường đã đổi, nhãn tiếng Việt', () {
      final detail = {
        'before': {'fullName': 'An', 'isActive': true, 'role': 'SALES'},
        'after': {'fullName': 'An', 'isActive': false, 'role': 'MANAGER'},
        'passwordReset': true,
      };
      expect(detailRows(detail), [
        ('Đang hoạt động', 'Có', 'Không'),
        ('Vai trò', 'SALES', 'MANAGER'),
        ('Mật khẩu', null, 'Đã đặt lại'),
      ]);
    });

    test('thanh toán hiện số tiền định dạng; chuỗi thường giữ nguyên', () {
      final rows = detailRows({'code': 'PC-001', 'amount': 1500000});
      expect(rows.first, ('Mã phiếu', null, 'PC-001'));
      expect(rows.last.$3, contains('1.500.000'));
      expect(detailRows('Khách hủy đơn'), [('Nội dung', null, 'Khách hủy đơn')]);
      expect(detailSummary(null), '—');
    });
  });
}
