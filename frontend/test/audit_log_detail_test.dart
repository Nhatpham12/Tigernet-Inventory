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

    test('thêm mới có tóm tắt: số phiếu, đối tác, tổng tiền, dòng hàng', () {
      final detail = {
        'method': 'POST',
        'path': '/import-receipts',
        'summary': {
          'id': 12,
          'receiptCode': 'PN-KHO1-2026-0005',
          'warehouseName': 'Kho 1',
          'sourcePartyName': 'NCC A',
          'totalAmount': 12500000,
          'itemCount': 2,
          'items': ['Router X [SN1] × 1', 'Cáp × 5'],
        },
      };
      final summary = detailSummary(detail);
      expect(
        summary,
        startsWith('Thêm mới Phiếu nhập #12: PN-KHO1-2026-0005 · NCC A · '),
      );
      expect(summary, contains('12.500.000'));
      final rows = detailRows(detail);
      expect(rows.first, ('Thao tác', null, 'Thêm mới Phiếu nhập #12'));
      expect(rows, contains(('Số phiếu', null, 'PN-KHO1-2026-0005')));
      expect(rows, contains(('Kho', null, 'Kho 1')));
      expect(rows, contains(('Hàng', null, 'Router X [SN1] × 1\nCáp × 5')));
      expect(rows.last, ('Đường dẫn', null, '/import-receipts'));
    });

    test(
      'có số giấy tờ: đối tượng và thao tác hiện số phiếu, không lặp lại',
      () {
        expect(
          targetLabel('Import_Receipts', 1, 'PN-MAIN-2026-0001'),
          'Phiếu nhập PN-MAIN-2026-0001',
        );
        expect(targetLabel('stock-transfers', 3, null), 'Chuyển kho #3');
        expect(
          targetLabel('payments', 9, 'THU-MAIN-2026-0002'),
          'Phiếu thu/chi THU-MAIN-2026-0002',
        );
        final detail = {
          'method': 'PUT',
          'path': '/import-receipts/1',
          'summary': {
            'id': 1,
            'receiptCode': 'PN-MAIN-2026-0001',
            'sourcePartyName': 'NCC A',
          },
        };
        expect(
          detailSummary(detail, code: 'PN-MAIN-2026-0001'),
          'Cập nhật Phiếu nhập PN-MAIN-2026-0001: NCC A',
        );
        expect(detailRows(detail, code: 'PN-MAIN-2026-0001').first, (
          'Thao tác',
          null,
          'Cập nhật Phiếu nhập PN-MAIN-2026-0001',
        ));
        // hủy phiếu do service ghi: lý do hiện nhãn tiếng Việt
        expect(detailSummary({'reason': 'Nhập nhầm'}), 'Lý do: Nhập nhầm');
        expect(actionLabel('LOGIN_FAILED'), 'Đăng nhập sai');
        expect(actionLabel('COMPLETE'), 'Hoàn tất');
      },
    );

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
      expect(detailRows('Khách hủy đơn'), [
        ('Nội dung', null, 'Khách hủy đơn'),
      ]);
      expect(detailSummary(null), '—');
    });
  });
}
