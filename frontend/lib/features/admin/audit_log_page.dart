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

/// Tên tiếng Việt của các trường hay gặp trong chi tiết nhật ký.
const _fieldLabels = <String, String>{
  'username': 'Tên đăng nhập',
  'fullName': 'Họ tên',
  'email': 'Email',
  'role': 'Vai trò',
  'isActive': 'Đang hoạt động',
  'code': 'Mã phiếu',
  'amount': 'Số tiền',
  'pages': 'Trang được phân quyền',
  'reason': 'Lý do',
  'method': 'Loại thao tác',
  'path': 'Đường dẫn',
};

const _methodLabels = <String, String>{
  'POST': 'Thêm mới',
  'PUT': 'Cập nhật',
  'PATCH': 'Cập nhật',
  'DELETE': 'Xóa / hủy',
};

String _fieldLabel(Object? key) => _fieldLabels['$key'] ?? '$key';

/// Giá trị hiển thị: true/false → Có/Không, danh sách → nối dấu phẩy, rỗng → —.
String _valueText(Object? key, Object? v) => switch (v) {
  null || '' => '—',
  true => 'Có',
  false => 'Không',
  List() => v.isEmpty ? '—' : v.join(', '),
  num() when key == 'amount' => Fmt.money(v),
  _ when key == 'method' => _methodLabels['$v'] ?? '$v',
  _ => '$v',
};

/// "PUT /suppliers/5" → "Cập nhật Nhà cung cấp #5".
String _requestText(Map d) {
  final segments = '${d['path'] ?? ''}'
      .split('/')
      .where((s) => s.isNotEmpty)
      .toList();
  if (segments.isEmpty) return _valueText('method', d['method']);
  final id = segments.length > 1 && int.tryParse(segments[1]) != null
      ? segments[1]
      : null;
  return '${_valueText('method', d['method'])} ${targetLabel(segments.first, id)}';
}

/// Các dòng (trường, trước, sau) của chi tiết; [before] null khi không có giá trị cũ.
List<(String field, String? before, String after)> detailRows(Object? detail) {
  final d = _decode(detail);
  if (d == null) return const [];
  if (d is! Map) return [('Nội dung', null, '$d')];
  final before = d['before'], after = d['after'];
  if (before is Map && after is Map) {
    return [
      for (final k in {...before.keys, ...after.keys})
        if ('${before[k]}' != '${after[k]}')
          (_fieldLabel(k), _valueText(k, before[k]), _valueText(k, after[k])),
      if (d['passwordReset'] == true) ('Mật khẩu', null, 'Đã đặt lại'),
    ];
  }
  if (d['path'] != null) {
    return [
      ('Thao tác', null, _requestText(d)),
      ('Đường dẫn', null, '${d['path']}'),
    ];
  }
  return [
    for (final e in d.entries)
      (_fieldLabel(e.key), null, _valueText(e.key, e.value)),
  ];
}

/// Tóm tắt cột "Chi tiết": các trường đã đổi hoặc mô tả thao tác.
String detailSummary(Object? detail) {
  final d = _decode(detail);
  if (d == null) return '—';
  if (d is Map && d['path'] != null) return _requestText(d);
  if (d is Map && d['pages'] is List) {
    return '${(d['pages'] as List).length} trang';
  }
  final rows = detailRows(d);
  if (rows.isEmpty) return 'Không đổi';
  return rows
      .map(
        (r) => r.$2 == null ? '${r.$1}: ${r.$3}' : '${r.$1}: ${r.$2} → ${r.$3}',
      )
      .join('; ');
}

/// Bảng chi tiết trong hộp thoại: 2 cột (trường, giá trị) hoặc 3 cột khi có giá trị trước/sau.
class _DetailTable extends StatelessWidget {
  const _DetailTable({required this.rows});

  final List<(String field, String? before, String after)> rows;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final hasBefore = rows.any((r) => r.$2 != null);
    final head = TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w700,
      color: c.text,
    );
    final cellStyle = TextStyle(fontSize: 13, color: c.text);
    Widget cell(String text, {TextStyle? style, Color? color}) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: SelectableText(
        text,
        style: (style ?? cellStyle).copyWith(color: color),
      ),
    );
    return Table(
      border: TableBorder(horizontalInside: BorderSide(color: c.divider)),
      columnWidths: const {0: IntrinsicColumnWidth(flex: 1)},
      defaultVerticalAlignment: TableCellVerticalAlignment.middle,
      children: [
        TableRow(
          decoration: BoxDecoration(color: c.surfaceMuted),
          children: [
            cell('Trường', style: head),
            if (hasBefore) cell('Trước', style: head),
            cell(hasBefore ? 'Sau' : 'Giá trị', style: head),
          ],
        ),
        for (final (field, before, after) in rows)
          TableRow(
            children: [
              cell(field, style: head.copyWith(fontWeight: FontWeight.w600)),
              if (hasBefore) cell(before ?? '—', color: c.muted),
              cell(after, color: hasBefore ? c.success : null),
            ],
          ),
      ],
    );
  }
}

/// Lịch sử thao tác của nhân viên (QT-03) — GET /bff/audit-logs, chỉ Quản trị.
class AuditLogPage extends ConsumerWidget {
  const AuditLogPage({super.key, this.initialUserId});

  final int? initialUserId;

  void _showDetail(BuildContext context, RowData r) {
    final rows = detailRows(r['detail']);
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
                rows.isEmpty
                    ? const Text('Không có chi tiết.')
                    : _DetailTable(rows: rows),
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
