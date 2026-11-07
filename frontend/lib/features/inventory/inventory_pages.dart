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

/// Danh sách kho (QL-12) — GET /bff/warehouses, thêm kho qua /bff/api/warehouses.
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
      _controller.reload();
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
