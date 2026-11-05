import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tigernet_app/core/auth/role.dart';
import 'package:tigernet_app/core/auth/session.dart';
import 'package:tigernet_app/core/network/page_data.dart';
import 'package:tigernet_app/core/router/menu.dart';
import 'package:tigernet_app/core/theme/app_theme.dart';
import 'package:tigernet_app/core/utils/formatters.dart';
import 'package:tigernet_app/shared/data_table/app_data_table.dart';
import 'package:tigernet_app/shared/widgets/status_chip.dart';

void main() {
  group('Fmt', () {
    test('định dạng tiền và mã sản phẩm như Figma', () {
      expect(Fmt.money(1250000), contains('1.250.000'));
      expect(Fmt.money(null), '—');
      expect(Fmt.productCode(124), 'SP-00124');
      expect(Fmt.apiDateTime(DateTime(2026, 10, 9)), '2026-10-09T00:00:00');
      expect(Fmt.date('2026-10-07T09:15:00'), '07/10/2026');
    });
  });

  test('Session.fromJson đọc đúng /bff/session và tạo chữ viết tắt', () {
    final s = Session.fromJson({
      'userId': 1,
      'username': 'admin',
      'fullName': 'Nguyễn Văn A',
      'role': 'ADMIN',
      'canViewAllWarehouses': true,
      'warehouses': [
        {'id': 2, 'code': 'HCM', 'name': 'Kho TP.HCM', 'isDefault': false},
      ],
      'defaultWarehouseId': null,
      'unreadAlerts': 6,
    });
    expect(s.initials, 'NA');
    expect(s.role, AppRole.admin);
    expect(s.warehouses.single.name, 'Kho TP.HCM');
    expect(s.unreadAlerts, 6);
  });

  test('PageData.fromJson khớp PageResponse của BFF', () {
    final page = PageData.fromJson({
      'items': [
        {'id': 1},
        {'id': 2},
      ],
      'page': 1,
      'size': 20,
      'totalItems': 24,
      'totalPages': 2,
    }, (e) => e);
    expect(page.items.length, 2);
    expect(page.totalPages, 2);
  });

  test('menu ẩn mục theo vai trò', () {
    final sales = kMainMenu
        .where((g) => canSee(g.roles, AppRole.sales))
        .map((g) => g.label);
    expect(sales, contains('Xuất kho'));
    expect(sales, isNot(contains('Nhập hàng')));
    expect(kMainMenu.every((g) => canSee(g.roles, AppRole.admin)), isTrue);
  });

  testWidgets('AppDataTable hiển thị header, dòng và badge trạng thái', (
    tester,
  ) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: AppTheme.light(),
          home: Scaffold(
            body: SizedBox(
              width: 1200,
              child: AppDataTable(
                columns: [
                  TableColumnDef.text('Mã phiếu', (r) => '${r['code']}'),
                  TableColumnDef(
                    label: 'Trạng thái',
                    cell: (r) => Statuses.receipt(r['status'] as String?),
                  ),
                ],
                rows: const [
                  {'code': 'PX-00087', 'status': 'EXPORTED'},
                  {'code': 'PX-00088', 'status': 'PENDING'},
                ],
              ),
            ),
          ),
        ),
      ),
    );
    expect(find.text('Mã phiếu'), findsOneWidget);
    expect(find.text('PX-00087'), findsOneWidget);
    expect(find.text('Đã xuất'), findsOneWidget);
    expect(find.text('Chờ duyệt'), findsOneWidget);
  });
}
