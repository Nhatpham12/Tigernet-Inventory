import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import 'warranty_common.dart';
import 'warranty_status_dialog.dart';

/// Danh sách phiếu bảo hành — GET /bff/warranty-claims.
class WarrantyClaimListPage extends StatefulWidget {
  const WarrantyClaimListPage({super.key});

  @override
  State<WarrantyClaimListPage> createState() => _WarrantyClaimListPageState();
}

class _WarrantyClaimListPageState extends State<WarrantyClaimListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách phiếu bảo hành',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Bảo hành'),
      Crumb('Danh sách phiếu bảo hành'),
    ],
    controller: _controller,
    actions: [
      SizedBox(
        width: 200,
        child: ElevatedButton(
          onPressed: () => context.go('/app/warranty-claims/new'),
          child: const Text('Tiếp nhận bảo hành'),
        ),
      ),
    ],
    endpoint: '/bff/warranty-claims',
    defaultSort: 'receivedAt,desc',
    searchHint: 'Tìm mã phiếu, khách hàng, thiết bị, số seri...',
    itemLabel: 'phiếu',
    onRowTap: (r) => context.go('/app/warranty-claims/${r['id']}'),
    filters: const [
      ListFilter(
        key: 'status',
        label: 'Trạng thái',
        options: kWarrantyStatuses,
      ),
    ],
    columns: [
      TableColumnDef.text(
        'Mã phiếu',
        (r) => '${r['claimCode']}',
        minWidth: 185,
        sortKey: 'claimCode',
        bold: true,
      ),
      TableColumnDef.text(
        'Ngày nhận',
        (r) => Fmt.date(r['receivedAt']),
        minWidth: 100,
        sortKey: 'receivedAt',
      ),
      TableColumnDef.text(
        'Khách hàng',
        (r) => '${r['customerName'] ?? '—'}',
        flex: 3,
        minWidth: 160,
      ),
      TableColumnDef.text(
        'Thiết bị',
        (r) => '${r['productName'] ?? '—'}',
        flex: 3,
        minWidth: 160,
      ),
      TableColumnDef.text(
        'Số seri',
        (r) => '${r['serial'] ?? '—'}',
        minWidth: 120,
      ),
      TableColumnDef.text(
        'Hạn BH',
        (r) => Fmt.date(r['warrantyExpiry']),
        minWidth: 100,
        sortKey: 'warrantyExpiry',
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 130,
        cell: (r) => warrantyStatusChip(r['status'] as String?),
      ),
      TableColumnDef(
        label: 'Thao tác',
        flex: 1,
        minWidth: 140,
        align: Alignment.center,
        cell: (r) => Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            PrintButton(
              compact: true,
              docs: [
                warrantyPrintDoc([r['id']]),
              ],
            ),
            if (kWarrantyTransitions.containsKey(r['status']))
              IconButton(
                tooltip: 'Đổi trạng thái',
                icon: const Icon(Icons.sync_alt),
                onPressed: () async {
                  if (await showWarrantyStatusDialog(context, r)) {
                    _controller.reload();
                  }
                },
              ),
          ],
        ),
      ),
    ],
  );
}
