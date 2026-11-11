import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

/// Danh sách kho (QL-12) — GET /bff/warehouses; thêm / sửa / ngừng / xóa kho qua /bff/api/warehouses.
class WarehouseListPage extends ConsumerStatefulWidget {
  const WarehouseListPage({super.key});

  @override
  ConsumerState<WarehouseListPage> createState() => _WarehouseListPageState();
}

class _WarehouseListPageState extends ConsumerState<WarehouseListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _add() async {
    final values = await formDialog(
      context,
      title: 'Thêm kho',
      fields: [
        ('warehouseCode', 'Mã kho', '', true),
        ('warehouseName', 'Tên kho', '', true),
        ('address', 'Địa chỉ', '', true),
        ('phone', 'Số điện thoại', '', false),
      ],
    );
    if (values == null) return;
    try {
      await ref
          .read(apiClientProvider)
          .post('/bff/api/warehouses', data: {...values, 'isActive': true});
      if (mounted) {
        Toast.success(context, 'Đã thêm kho ${values['warehouseName']}.');
      }
      _changed();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  /// Danh sách kho ở các ô chọn (phiếu nhập, xuất...) cũng phải tải lại.
  void _changed() {
    ref.invalidate(warehousesLookupProvider);
    _controller.reload();
  }

  /// PUT /bff/api/warehouses/{id}: [data] chỉ chứa trường cần đổi.
  Future<void> _update(
    RowData r,
    Map<String, Object?> data,
    String done,
  ) async {
    try {
      await ref
          .read(apiClientProvider)
          .put('/bff/api/warehouses/${r['id']}', data: data);
      if (mounted) Toast.success(context, done);
      _changed();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  Future<void> _edit(RowData r) async {
    final values = await formDialog(
      context,
      title: 'Sửa kho ${r['warehouseCode']}',
      fields: [
        ('warehouseName', 'Tên kho', '${r['warehouseName'] ?? ''}', true),
        ('address', 'Địa chỉ', '${r['address'] ?? ''}', true),
        ('phone', 'Số điện thoại', '${r['phone'] ?? ''}', false),
      ],
    );
    if (values == null) return;
    await _update(r, values, 'Đã cập nhật kho ${values['warehouseName']}.');
  }

  Future<void> _toggleActive(RowData r) async {
    final active = r['isActive'] == true;
    final ok = await confirmDialog(
      context,
      title: active
          ? 'Ngừng hoạt động kho ${r['warehouseName']}'
          : 'Mở lại kho ${r['warehouseName']}',
      message: active
          ? 'Kho sẽ không chọn được khi lập phiếu mới. Phiếu và tồn kho cũ vẫn giữ nguyên.'
          : 'Kho sẽ chọn lại được khi lập phiếu.',
      confirmLabel: active ? 'Ngừng hoạt động' : 'Mở lại',
      danger: active,
    );
    if (!ok) return;
    await _update(r, {
      'isActive': !active,
    }, active ? 'Đã ngừng kho ${r['warehouseName']}.' : 'Đã mở lại kho.');
  }

  Future<void> _delete(RowData r) async {
    final ok = await confirmDialog(
      context,
      title: 'Xóa kho ${r['warehouseName']}',
      message:
          'Chỉ xóa được kho chưa có phiếu hay tồn kho nào. Thao tác này không hoàn tác được.',
      confirmLabel: 'Xóa kho',
      danger: true,
    );
    if (!ok) return;
    try {
      await ref
          .read(apiClientProvider)
          .delete('/bff/api/warehouses/${r['id']}');
      if (mounted) Toast.success(context, 'Đã xóa kho ${r['warehouseName']}.');
      _changed();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách kho',
    subtitle: 'Kho hàng, địa chỉ và người quản lý',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Kho'),
      Crumb('Danh sách kho'),
    ],
    controller: _controller,
    actions: [ElevatedButton(onPressed: _add, child: const Text('Thêm kho'))],
    endpoint: '/bff/warehouses',
    useWarehouseScope: false,
    searchHint: 'Tìm mã kho, tên kho, địa chỉ...',
    itemLabel: 'kho',
    columns: [
      TableColumnDef.text(
        'Mã kho',
        (r) => '${r['warehouseCode']}',
        minWidth: 90,
        sortKey: 'warehouseCode',
        bold: true,
      ),
      TableColumnDef.text(
        'Tên kho',
        (r) => '${r['warehouseName']}',
        flex: 3,
        minWidth: 160,
        sortKey: 'warehouseName',
      ),
      TableColumnDef.text(
        'Địa chỉ',
        (r) => '${r['address'] ?? '—'}',
        flex: 4,
        minWidth: 200,
      ),
      TableColumnDef.text(
        'Điện thoại',
        (r) => '${r['phone'] ?? '—'}',
        minWidth: 110,
      ),
      TableColumnDef.text(
        'Quản lý',
        (r) => '${r['managerName'] ?? '—'}',
        minWidth: 130,
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 120,
        cell: (r) => r['isActive'] == true
            ? const StatusChip(label: 'Hoạt động', tone: ChipTone.success)
            : const StatusChip(label: 'Ngừng', tone: ChipTone.neutral),
      ),
      TableColumnDef(
        label: 'Thao tác',
        minWidth: 90,
        align: Alignment.center,
        cell: (r) => PopupMenuButton<String>(
          tooltip: 'Thao tác',
          icon: const Icon(Icons.more_horiz),
          onSelected: (v) => switch (v) {
            'edit' => _edit(r),
            'toggle' => _toggleActive(r),
            _ => _delete(r),
          },
          itemBuilder: (_) => [
            const PopupMenuItem(value: 'edit', child: Text('Sửa thông tin')),
            PopupMenuItem(
              value: 'toggle',
              child: Text(
                r['isActive'] == true ? 'Ngừng hoạt động' : 'Mở lại kho',
              ),
            ),
            const PopupMenuItem(value: 'delete', child: Text('Xóa kho')),
          ],
        ),
      ),
    ],
  );
}

/// Figma 2992:10672 "Chuyển kho / Danh sách phiếu chuyển kho" — GET /bff/stock-transfers.
class TransferListPage extends StatelessWidget {
  const TransferListPage({super.key});

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách phiếu chuyển kho',
    subtitle: 'Theo dõi hàng đang chuyển giữa các kho',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Chuyển kho'),
      Crumb('Danh sách phiếu'),
    ],
    endpoint: '/bff/stock-transfers',
    useWarehouseScope: false,
    defaultSort: 'createdAt,desc',
    searchHint: 'Tìm mã phiếu, kho đi, kho đến...',
    itemLabel: 'phiếu',
    filters: const [
      ListFilter(
        key: 'status',
        label: 'Trạng thái',
        options: [
          (null, 'Tất cả'),
          ('DRAFT', 'Nháp'),
          ('APPROVED', 'Đã duyệt'),
          ('IN_TRANSIT', 'Đang chuyển'),
          ('RECEIVED', 'Đã nhận'),
          ('CANCELLED', 'Đã hủy'),
        ],
      ),
    ],
    columns: [
      TableColumnDef.text(
        'Mã phiếu',
        (r) => '${r['transferCode']}',
        minWidth: 110,
        sortKey: 'transferCode',
        bold: true,
      ),
      TableColumnDef.text(
        'Ngày tạo',
        (r) => Fmt.date(r['createdAt']),
        minWidth: 100,
        sortKey: 'createdAt',
      ),
      TableColumnDef.text(
        'Kho đi',
        (r) => '${r['fromWarehouseCode']}',
        minWidth: 100,
      ),
      TableColumnDef.text(
        'Kho đến',
        (r) => '${r['toWarehouseCode']}',
        minWidth: 100,
      ),
      TableColumnDef.text(
        'SL',
        (r) => Fmt.number(r['totalQuantity']),
        flex: 1,
        minWidth: 50,
        align: Alignment.centerRight,
      ),
      TableColumnDef.text(
        'Người tạo',
        (r) => '${r['createdByName'] ?? '—'}',
        minWidth: 130,
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 130,
        cell: (r) => Statuses.receipt(r['status'] as String?),
      ),
    ],
  );
}
