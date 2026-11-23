import 'package:flutter_test/flutter_test.dart';
import 'package:tigernet_app/core/auth/role.dart';
import 'package:tigernet_app/core/auth/session.dart';
import 'package:tigernet_app/core/router/menu.dart';

Session _session(AppRole role, {Set<String>? effective}) => Session(
  userId: 5,
  username: 'u',
  fullName: '',
  email: null,
  role: role,
  canViewAllWarehouses: false,
  warehouses: const [],
  defaultWarehouseId: null,
  unreadAlerts: 0,
  effectivePages: effective,
);

void main() {
  test('pageKeyFor tách quyền tạo phiếu thu và phiếu chi theo query', () {
    expect(pageKeyFor('/app/payments/new?direction=OUT'), kPaymentsNewOut);
    expect(
      pageKeyFor('/app/payments/new?direction=OUT&refType=Import_Receipts'),
      kPaymentsNewOut,
    );
    expect(pageKeyFor('/app/payments/new?direction=IN'), kPaymentsNewIn);
    expect(pageKeyFor('/app/payments'), '/app/payments');
    expect(pageKeyFor('/app/products/5/edit'), '/app/products/new');
  });

  test('canOpenPage theo effectivePages do backend tính', () {
    final s = _session(
      AppRole.warehouse,
      effective: {'/app/dashboard', '/app/warehouses'},
    );
    expect(canOpenPage('/app/warehouses', s), isTrue); // cấp thêm ngoài vai trò
    expect(canOpenPage('/app/import-receipts/new', s), isFalse); // bị thu hẹp
    expect(canAccessPath('/app/payments/new?direction=OUT', s), isFalse);
  });

  test('menu phân quyền dùng page key không có query', () {
    final keys = kAssignablePages.map((p) => p.path).toSet();
    expect(keys, containsAll([kPaymentsNewIn, kPaymentsNewOut]));
    expect(keys.where((k) => k.contains('?')), isEmpty);
  });
}
