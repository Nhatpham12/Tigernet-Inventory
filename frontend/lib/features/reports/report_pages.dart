import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/status_chip.dart';
import '../export_receipts/payment_dialog.dart';

/// Figma 1005:539 "Báo cáo / Tồn kho" — GET /bff/reports/stock.
class StockReportPage extends StatelessWidget {
  const StockReportPage({super.key});

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Báo cáo tồn kho',
    subtitle: 'Tồn thực tế, đang giữ, cho thuê và hư hỏng theo từng kho',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Báo cáo'),
      Crumb('Tồn kho'),
    ],
    endpoint: '/bff/reports/stock',
    defaultSort: 'productName,asc',
    searchHint: 'Tìm sản phẩm, mã SP, kho...',
    itemLabel: 'dòng tồn',
    columns: [
      TableColumnDef.text(
        'Mã SP',
        (r) => '${r['productCode']}',
        minWidth: 100,
        sortKey: 'productId',
      ),
      TableColumnDef.text(
        'Sản phẩm',
        (r) => '${r['productName']}',
        flex: 3,
        minWidth: 180,
        sortKey: 'productName',
        bold: true,
      ),
      TableColumnDef.text(
        'Kho',
        (r) => '${r['warehouseCode'] ?? '—'}',
        minWidth: 90,
        sortKey: 'warehouseCode',
      ),
      for (final (label, key) in [
        ('Tồn kho', 'qtyOnHand'),
        ('Khả dụng', 'qtyAvailable'),
        ('Đang giữ', 'qtyReserved'),
        ('Cho thuê', 'qtyRented'),
        ('Hư hỏng', 'qtyDamaged'),
        ('Đang chuyển', 'qtyInTransit'),
      ])
        TableColumnDef.text(
          label,
          (r) => Fmt.number(r[key] ?? 0),
          flex: 1,
          minWidth: 90,
          align: Alignment.centerRight,
          sortKey: key,
        ),
      TableColumnDef.text(
        'Cập nhật',
        (r) => Fmt.dateTime(r['lastUpdated']),
        minWidth: 140,
        sortKey: 'lastUpdated',
      ),
    ],
  );
}

/// Figma 1039:480 "Báo cáo / Nhập xuất kho" + 1112:3261 "Lịch sử giao dịch" — GET /bff/reports/movements.
class MovementReportPage extends StatelessWidget {
  const MovementReportPage({super.key});

  static String _reference(Object? type) => switch (type) {
    // giá trị Reference_Type trong Stock_Movements (Database v7)
    'Import_Receipts' || 'IMPORT' => 'Phiếu nhập',
    'Export_Receipts' || 'EXPORT' => 'Phiếu xuất',
    'Rental_Receipts' || 'RENTAL' => 'Phiếu thuê/mượn',
    'Return_Receipts' || 'RETURN' => 'Phiếu trả',
    'Stock_Transfers' || 'TRANSFER' => 'Chuyển kho',
    'Stock_Adjustments' || 'ADJUSTMENT' => 'Điều chỉnh',
    'Sales_Returns' || 'SALES_RETURN' => 'Trả hàng bán',
    'Supplier_Returns' => 'Trả nhà cung cấp',
    'Warranty_Claims' => 'Bảo hành',
    _ => '${type ?? '—'}',
  };

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Báo cáo nhập xuất kho',
    subtitle: 'Lịch sử biến động tồn theo từng chứng từ',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Báo cáo'),
      Crumb('Nhập xuất kho'),
    ],
    endpoint: '/bff/reports/movements',
    defaultSort: 'createdAt,desc',
    searchHint: 'Tìm sản phẩm, loại chứng từ...',
    itemLabel: 'giao dịch',
    columns: [
      TableColumnDef.text(
        'Thời gian',
        (r) => Fmt.dateTime(r['createdAt']),
        minWidth: 140,
        sortKey: 'createdAt',
      ),
      TableColumnDef(
        label: 'Loại',
        flex: 1,
        minWidth: 90,
        cell: (r) => r['movementType'] == 'IN'
            ? const StatusChip(label: 'Nhập', tone: ChipTone.success)
            : const StatusChip(label: 'Xuất', tone: ChipTone.info),
      ),
      TableColumnDef.text(
        'Sản phẩm',
        (r) => '${r['productName']}',
        flex: 3,
        minWidth: 180,
        sortKey: 'productName',
      ),
      TableColumnDef.text(
        'Kho',
        (r) => '${r['warehouseCode'] ?? '—'}',
        minWidth: 90,
      ),
      TableColumnDef.text(
        'Chứng từ',
        (r) => '${_reference(r['referenceType'])} #${r['referenceId'] ?? ''}',
        minWidth: 150,
      ),
      TableColumnDef.text(
        'Số lượng',
        (r) => Fmt.number(r['quantity']),
        flex: 1,
        minWidth: 90,
        align: Alignment.centerRight,
        sortKey: 'quantity',
      ),
      TableColumnDef.text(
        'Tồn sau',
        (r) => Fmt.number(r['balanceAfter']),
        flex: 1,
        minWidth: 90,
        align: Alignment.centerRight,
      ),
    ],
  );
}

/// Figma 1295:2484 "Báo cáo / Công nợ" — GET /bff/reports/receivables + ghi nhận thanh toán.
class ReceivablesPage extends StatefulWidget {
  const ReceivablesPage({super.key});

  @override
  State<ReceivablesPage> createState() => _ReceivablesPageState();
}

class _ReceivablesPageState extends State<ReceivablesPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return PagedListView(
      title: 'Công nợ phải thu',
      subtitle: 'Phiếu xuất còn nợ; ghi nhận thanh toán và kiểm tra số dư',
      crumbs: const [
        Crumb('Trang chủ', '/app/dashboard'),
        Crumb('Báo cáo'),
        Crumb('Công nợ'),
      ],
      controller: _controller,
      endpoint: '/bff/reports/receivables',
      defaultSort: 'remain,desc',
      searchHint: 'Tìm mã phiếu, khách hàng...',
      itemLabel: 'phiếu',
      filters: const [
        ListFilter(
          key: 'onlyOutstanding',
          label: 'Hiển thị',
          options: [(null, 'Còn nợ'), ('false', 'Tất cả phiếu')],
        ),
      ],
      columns: [
        TableColumnDef.text(
          'Mã phiếu',
          (r) => '${r['receiptCode']}',
          minWidth: 110,
          sortKey: 'receiptCode',
          bold: true,
        ),
        TableColumnDef.text(
          'Ngày xuất',
          (r) => Fmt.date(r['dateOut']),
          minWidth: 100,
          sortKey: 'dateOut',
        ),
        TableColumnDef.text(
          'Khách hàng',
          (r) => '${r['customerName'] ?? '—'}',
          flex: 3,
          minWidth: 170,
          sortKey: 'customerName',
        ),
        TableColumnDef.text(
          'Tổng tiền',
          (r) => Fmt.money(r['totalAmount']),
          minWidth: 120,
          align: Alignment.centerRight,
          sortKey: 'totalAmount',
        ),
        TableColumnDef.text(
          'Đã thu',
          (r) => Fmt.money(r['paid']),
          minWidth: 120,
          align: Alignment.centerRight,
          sortKey: 'paid',
        ),
        TableColumnDef(
          label: 'Còn lại',
          minWidth: 120,
          align: Alignment.centerRight,
          sortKey: 'remain',
          cell: (r) => Text(
            Fmt.money(r['remain']),
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: (Fmt.toNum(r['remain']) ?? 0) > 0 ? c.danger : c.success,
            ),
          ),
        ),
        TableColumnDef(
          label: 'Thanh toán',
          minWidth: 150,
          cell: (r) => Statuses.payment(r['remain'], r['paid']),
        ),
        TableColumnDef(
          label: 'Thao tác',
          minWidth: 120,
          align: Alignment.center,
          cell: (r) => (Fmt.toNum(r['remain']) ?? 0) > 0
              ? OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 32),
                  ),
                  onPressed: () async {
                    if (await showPaymentDialog(context, r)) {
                      _controller.reload();
                    }
                  },
                  child: const Text('Thu tiền'),
                )
              : const Text('—'),
        ),
      ],
    );
  }
}
