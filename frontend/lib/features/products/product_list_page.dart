import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/status_chip.dart';

/// Figma 968:610 "Sản phẩm / Danh sách sản phẩm" — GET /bff/products (ghép tồn theo kho đang chọn).
class ProductListPage extends StatelessWidget {
  const ProductListPage({super.key, this.initialStockStatus});

  final String? initialStockStatus;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return PagedListView(
      title: 'Danh sách sản phẩm',
      subtitle: 'Quản lý danh mục sản phẩm, giá bán và tồn kho theo từng kho',
      crumbs: const [
        Crumb('Trang chủ', '/app/dashboard'),
        Crumb('Sản phẩm'),
        Crumb('Danh sách sản phẩm'),
      ],
      actions: [
        SizedBox(
          width: 190,
          child: ElevatedButton(
            onPressed: () => context.go('/app/products/new'),
            child: const Text('Thêm sản phẩm'),
          ),
        ),
      ],
      endpoint: '/bff/products',
      extraQuery: {'stockStatus': initialStockStatus},
      searchHint: 'Tìm sản phẩm, mã SP/SKU...',
      itemLabel: 'sản phẩm',
      filters: const [
        ListFilter(
          key: 'stockStatus',
          label: 'Trạng thái',
          options: [
            (null, 'Tất cả'),
            ('NORMAL', 'Bình thường'),
            ('NEAR_LOW', 'Sắp hết'),
            ('LOW', 'Tồn thấp'),
            ('OUT_OF_STOCK', 'Hết hàng'),
          ],
        ),
      ],
      banner: (data) => Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
        decoration: BoxDecoration(
          border: Border.all(color: c.divider),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(Icons.circle, size: 8, color: c.accent),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                '${data.totalItems} sản phẩm trong danh sách  •  Tra cứu SKU, tồn kho và giá bán',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: c.text,
                ),
              ),
            ),
            Text(
              'Nhấn vào dòng để xem chi tiết',
              style: TextStyle(fontSize: 11, color: c.muted),
            ),
          ],
        ),
      ),
      onRowTap: (row) => context.go('/app/products/${row['id']}/edit'),
      columns: [
        TableColumnDef(
          label: 'Ảnh',
          flex: 1,
          minWidth: 70,
          cell: (_) => Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: c.surfaceMuted,
              border: Border.all(color: c.border),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Icon(Icons.inventory_2_outlined, size: 16, color: c.muted),
          ),
        ),
        TableColumnDef.text(
          'Mã SP/SKU',
          (r) => '${r['code']}',
          minWidth: 100,
          sortKey: 'id',
        ),
        TableColumnDef.text(
          'Tên sản phẩm',
          (r) => '${r['productName']}',
          flex: 3,
          minWidth: 180,
          sortKey: 'productName',
        ),
        TableColumnDef.text(
          'ĐVT',
          (r) => '${r['unit'] ?? ''}',
          flex: 1,
          minWidth: 60,
        ),
        TableColumnDef.text(
          'Giá nhập',
          (r) => Fmt.money(r['priceIn']),
          minWidth: 110,
          align: Alignment.centerRight,
          sortKey: 'priceIn',
        ),
        TableColumnDef.text(
          'Giá bán',
          (r) => Fmt.money(r['priceOut']),
          minWidth: 110,
          align: Alignment.centerRight,
          sortKey: 'priceOut',
        ),
        TableColumnDef.text(
          'Tồn kho',
          (r) => Fmt.number(r['qtyOnHand']),
          flex: 1,
          minWidth: 80,
          align: Alignment.centerRight,
          sortKey: 'qtyOnHand',
        ),
        TableColumnDef.text(
          'Tồn khả dụng',
          (r) => Fmt.number(r['qtyAvailable']),
          minWidth: 110,
          align: Alignment.centerRight,
          sortKey: 'qtyAvailable',
        ),
        TableColumnDef(
          label: 'Trạng thái',
          minWidth: 130,
          cell: (r) => Statuses.stock(r['stockStatus'] as String?),
        ),
        TableColumnDef(
          label: 'Thao tác',
          flex: 1,
          minWidth: 80,
          align: Alignment.center,
          cell: (r) => IconButton(
            tooltip: 'Sửa',
            icon: const Icon(Icons.edit_outlined, size: 18),
            onPressed: () => context.go('/app/products/${r['id']}/edit'),
          ),
        ),
      ],
    );
  }
}
