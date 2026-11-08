import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/status_chip.dart';
import 'page_permissions_page.dart' show usersLookupProvider;

const _actionLabels = <String, String>{
  'LOGIN': 'Đăng nhập',
  'CREATE': 'Tạo mới',
  'UPDATE': 'Cập nhật',
  'DELETE': 'Xóa',
  'CANCEL': 'Hủy phiếu',
  'PAYMENT': 'Ghi nhận thanh toán',
  'CONFIRM': 'Xác nhận',
  'APPROVE': 'Duyệt',
  'CREATE_USER': 'Tạo nhân viên',
  'UPDATE_USER': 'Sửa nhân viên',
  'DELETE_USER': 'Xóa nhân viên',
  'LOCK_USER': 'Khóa nhân viên',
  'SET_PERMISSIONS': 'Phân quyền trang',
  'RESET_PERMISSIONS': 'Bỏ phân quyền riêng',
};

const _targetLabels = <String, String>{
  'users': 'Nhân viên',
  'import-receipts': 'Phiếu nhập',
  'export-receipts': 'Phiếu xuất',
  'rental-receipts': 'Phiếu thuê/mượn',
  'return-receipts': 'Phiếu trả',
  'sales-returns': 'Đổi trả',
  'stock-transfers': 'Chuyển kho',
  'stocktakes': 'Kiểm kê',
  'stock-adjustments': 'Điều chỉnh tồn',
  'products': 'Sản phẩm',
  'product-groups': 'Danh mục sản phẩm',
  'suppliers': 'Nhà cung cấp',
  'customers': 'Khách hàng',
  'parties': 'Đối tác',
  'warehouses': 'Kho',
  'alerts': 'Cảnh báo',
  'config': 'Cấu hình',
  'inventory': 'Tồn kho',
};

String actionLabel(Object? action) =>
    _actionLabels['$action'] ?? '${action ?? '—'}';

String targetLabel(Object? table, Object? id) {
  if (table == null) return '—';
  final key = '$table'.toLowerCase().replaceAll('_', '-');
  final label = _targetLabels[key] ?? '$table';
  return id == null ? label : '$label #$id';
}

ChipTone _tone(String? action) => switch (action) {
  'LOGIN' => ChipTone.neutral,
  'DELETE' || 'DELETE_USER' || 'CANCEL' || 'LOCK_USER' => ChipTone.danger,
  'CREATE' || 'CREATE_USER' => ChipTone.success,
  'SET_PERMISSIONS' || 'RESET_PERMISSIONS' => ChipTone.purple,
  'PAYMENT' => ChipTone.warning,
  _ => ChipTone.info,
};

Object? _decode(Object? detail) {
  if (detail is! String || detail.isEmpty) return detail;
  try {
    return jsonDecode(detail);
  } on FormatException {
    return detail;
  }
}

/// Tóm tắt cột "Chi tiết": đường dẫn API hoặc các trường đã đổi.
String detailSummary(Object? detail) {
  final d = _decode(detail);
  if (d == null) return '—';
  if (d is Map) {
    if (d['path'] != null) return '${d['method'] ?? ''} ${d['path']}'.trim();
    final before = d['before'], after = d['after'];
    if (before is Map && after is Map) {
      final changed = [
        for (final k in after.keys)
          if ('${before[k]}' != '${after[k]}') '$k: ${before[k]} → ${after[k]}',
        if (d['passwordReset'] == true) 'đặt lại mật khẩu',
      ];
      return changed.isEmpty ? 'Không đổi' : changed.join('; ');
    }
    if (d['pages'] is List) return '${(d['pages'] as List).length} trang';
    return d.entries.map((e) => '${e.key}: ${e.value}').join('; ');
  }
  return '$d';
}

/// Lịch sử thao tác của nhân viên (QT-03) — GET /bff/audit-logs, chỉ Quản trị.
class AuditLogPage extends ConsumerWidget {
  const AuditLogPage({super.key, this.initialUserId});

  final int? initialUserId;

  void _showDetail(BuildContext context, RowData r) {
    final d = _decode(r['detail']);
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('${actionLabel(r['action'])} – ${r['username'] ?? '—'}'),
        content: SizedBox(
          width: 560,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Thời gian: ${Fmt.dateTime(r['createdAt'])}'),
                Text(
                  'Đối tượng: ${targetLabel(r['targetTable'], r['targetId'])}',
                ),
                const SizedBox(height: 12),
                SelectableText(
                  d == null
                      ? 'Không có chi tiết.'
                      : const JsonEncoder.withIndent('  ').convert(d),
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Đóng'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final users = ref.watch(usersLookupProvider);
    return users.when(
      loading: () => const LoadingView(),
      error: (e, _) => StateView.error(
        e,
        onRetry: () => ref.invalidate(usersLookupProvider),
      ),
      data: (list) {
        final sorted = [...list]
          ..sort(
            (a, b) => '${a['username']}'.toLowerCase().compareTo(
              '${b['username']}'.toLowerCase(),
            ),
          );
        return PagedListView(
          title: 'Lịch sử thao tác',
          subtitle:
              'Nhật ký đăng nhập và mọi thao tác thêm, sửa, xóa của nhân viên',
          crumbs: const [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Quản trị'),
            Crumb('Lịch sử thao tác'),
          ],
          endpoint: '/bff/audit-logs',
          useWarehouseScope: false,
          searchHint: 'Tìm nhân viên, thao tác, đường dẫn...',
          itemLabel: 'dòng nhật ký',
          defaultSort: 'createdAt,desc',
          filters: [
            ListFilter(
              key: 'userId',
              label: 'Nhân viên',
              options: [
                (null, 'Tất cả'),
                for (final u in sorted) ('${u['userId']}', '${u['username']}'),
              ],
            ),
            ListFilter(
              key: 'action',
              label: 'Thao tác',
              options: [
                (null, 'Tất cả'),
                for (final MapEntry(:key, :value) in _actionLabels.entries)
                  (key, value),
              ],
            ),
          ],
          initialFilters: {
            if (initialUserId != null) 'userId': '$initialUserId',
          },
          onRowTap: (r) => _showDetail(context, r),
          columns: [
            TableColumnDef.text(
              'Thời gian',
              (r) => Fmt.dateTime(r['createdAt']),
              minWidth: 140,
              sortKey: 'createdAt',
            ),
            TableColumnDef.text(
              'Nhân viên',
              (r) => '${r['username'] ?? '(đã xóa)'}',
              minWidth: 130,
              sortKey: 'username',
              bold: true,
            ),
            TableColumnDef(
              label: 'Thao tác',
              minWidth: 140,
              sortKey: 'action',
              cell: (r) => StatusChip(
                label: actionLabel(r['action']),
                tone: _tone(r['action'] as String?),
              ),
            ),
            TableColumnDef.text(
              'Đối tượng',
              (r) => targetLabel(r['targetTable'], r['targetId']),
              minWidth: 150,
            ),
            TableColumnDef.text(
              'Chi tiết',
              (r) => detailSummary(r['detail']),
              flex: 4,
              minWidth: 240,
              maxWidth: 480,
            ),
          ],
          banner: (data) => Text(
            'Bấm vào một dòng để xem đầy đủ chi tiết.',
            style: TextStyle(fontSize: 13, color: context.colors.muted),
          ),
        );
      },
    );
  }
}
