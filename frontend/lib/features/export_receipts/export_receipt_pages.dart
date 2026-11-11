import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/form/form_footer.dart';
import '../../shared/form/receipt_line_editor.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';
import 'payment_dialog.dart';

/// Figma 978:2335 "Xuất kho / Danh sách phiếu xuất" — GET /bff/export-receipts.
class ExportReceiptListPage extends StatefulWidget {
  const ExportReceiptListPage({super.key});

  @override
  State<ExportReceiptListPage> createState() => _ExportReceiptListPageState();
}

class _ExportReceiptListPageState extends State<ExportReceiptListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách phiếu xuất',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Xuất kho'),
      Crumb('Danh sách phiếu xuất'),
    ],
    controller: _controller,
    actions: [
      SizedBox(
        width: 180,
        child: ElevatedButton(
          onPressed: () => context.go('/app/export-receipts/new'),
          child: const Text('Tạo phiếu xuất'),
        ),
      ),
    ],
    endpoint: '/bff/export-receipts',
    defaultSort: 'dateOut,desc',
    searchHint: 'Tìm sản phẩm, số seri, mã phiếu, khách hàng...',
    itemLabel: 'phiếu',
    filters: const [
      ListFilter(
        key: 'status',
        label: 'Trạng thái',
        options: [
          (null, 'Tất cả'),
          ('PENDING', 'Chờ xác nhận'),
          ('CONFIRMED', 'Đã xác nhận'),
          ('EXPORTED', 'Đã xuất'),
          ('CANCELLED', 'Đã hủy'),
        ],
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
        minWidth: 180,
      ),
      TableColumnDef.text(
        'Kho',
        (r) => '${r['warehouseName'] ?? '—'}',
        minWidth: 110,
      ),
      TableColumnDef.text(
        'SL',
        (r) => Fmt.number(r['totalQuantity']),
        flex: 1,
        minWidth: 50,
        align: Alignment.centerRight,
        sortKey: 'totalQuantity',
      ),
      TableColumnDef.text(
        'Tổng tiền',
        (r) => Fmt.money(r['totalAmount']),
        minWidth: 120,
        align: Alignment.centerRight,
        sortKey: 'totalAmount',
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 130,
        cell: (r) => Statuses.receipt(r['status'] as String?),
      ),
      TableColumnDef(
        label: 'Thao tác',
        minWidth: 130,
        align: Alignment.center,
        cell: (r) => Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _ExportPrintMenu(row: r),
            PopupMenuButton<String>(
              icon: const Icon(Icons.more_horiz),
              onSelected: (_) async {
                if (await showPaymentDialog(context, r)) _controller.reload();
              },
              itemBuilder: (_) => [
                PopupMenuItem(
                  value: 'pay',
                  enabled:
                      (Fmt.toNum(r['remain']) ?? 0) > 0 &&
                      r['status'] != 'CANCELLED',
                  child: const Text('Ghi nhận thanh toán'),
                ),
              ],
            ),
          ],
        ),
      ),
    ],
  );
}

/// In giấy tờ của phiếu xuất: 02-VT, Phiếu gửi hàng (hỏi số kiện), Biên bản bàn giao/bảo hành.
/// Phiếu đã hủy vẫn in được.
class _ExportPrintMenu extends ConsumerWidget {
  const _ExportPrintMenu({required this.row});

  final Map<String, dynamic> row;

  Future<int?> _askParcels(BuildContext context) {
    final ctrl = TextEditingController(text: '1');
    return showDialog<int>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Phiếu gửi hàng'),
        content: SizedBox(
          width: 280,
          child: AppTextField(
            label: 'Số kiện',
            controller: ctrl,
            keyboardType: TextInputType.number,
            inputFormatters: digitsOnly,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Hủy'),
          ),
          ElevatedButton(
            onPressed: () {
              final n = int.tryParse(ctrl.text) ?? 1;
              Navigator.pop(ctx, n < 1 ? 1 : n);
            },
            child: const Text('Tiếp tục'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final path = '/bff/api/export-receipts/${row['id']}/print';
    // (nhãn, template)
    const docs = [
      ('Phiếu xuất kho 02-VT', 'invoice'),
      ('Phiếu gửi hàng', 'delivery'),
      ('Biên bản bàn giao / bảo hành', 'handover'),
    ];
    final entries = <PopupMenuEntry<(String, String, bool)>>[];
    for (final (label, tpl) in docs) {
      entries
        ..add(
          PopupMenuItem(
            enabled: false,
            height: 32,
            child: Text(label, style: Theme.of(context).textTheme.labelMedium),
          ),
        )
        ..add(
          PopupMenuItem(
            value: (label, tpl, false),
            child: const ListTile(
              dense: true,
              leading: Icon(Icons.print_outlined),
              title: Text('Xem / In'),
            ),
          ),
        )
        ..add(
          PopupMenuItem(
            value: (label, tpl, true),
            child: const ListTile(
              dense: true,
              leading: Icon(Icons.download_outlined),
              title: Text('Tải PDF'),
            ),
          ),
        );
    }
    return PopupMenuButton<(String, String, bool)>(
      tooltip: 'In giấy tờ',
      itemBuilder: (_) => entries,
      onSelected: (v) async {
        final (label, tpl, download) = v;
        var parcels = 1;
        if (tpl == 'delivery') {
          final n = await _askParcels(context);
          if (n == null || !context.mounted) return;
          parcels = n;
        }
        await PrintButton.run(
          context,
          ref,
          PrintDoc(
            label,
            path,
            query: {'template': tpl, if (tpl == 'delivery') 'parcels': parcels},
          ),
          download: download,
        );
      },
      child: const Padding(
        padding: EdgeInsets.all(8),
        child: Icon(Icons.print_outlined),
      ),
    );
  }
}

/// Figma 1225:2344 "Xuất hàng / Tạo phiếu xuất hàng" — POST /bff/api/export-receipts.
class ExportReceiptFormPage extends ConsumerStatefulWidget {
  const ExportReceiptFormPage({super.key});

  @override
  ConsumerState<ExportReceiptFormPage> createState() =>
      _ExportReceiptFormPageState();
}

class _ExportReceiptFormPageState extends ConsumerState<ExportReceiptFormPage> {
  final _form = GlobalKey<FormState>();
  final _receiverName = TextEditingController();
  final _receiverPhone = TextEditingController();
  final _receiverAddress = TextEditingController();
  final _deposit = TextEditingController();
  final _paid = TextEditingController();
  final _note = TextEditingController();
  final List<ReceiptLine> _lines = [];
  int? _customerId;
  int? _warehouseId;
  int? _vatId;
  DateTime? _dateOut = DateTime.now();
  DateTime? _deliveryDate;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _warehouseId = ref.read(warehouseScopeProvider);
    _paid.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    for (final c in [
      _receiverName,
      _receiverPhone,
      _receiverAddress,
      _deposit,
      _paid,
      _note,
    ]) {
      c.dispose();
    }
    for (final l in _lines) {
      l.dispose();
    }
    super.dispose();
  }

  String? _text(TextEditingController c) =>
      c.text.trim().isEmpty ? null : c.text.trim();

  Future<void> _save(num total) async {
    if (!_form.currentState!.validate()) return;
    if (_lines.isEmpty || _lines.any((l) => l.quantity <= 0)) {
      Toast.error(context, 'Phiếu cần ít nhất một dòng hàng có số lượng > 0.');
      return;
    }
    final paid = parseNumber(_paid.text) ?? 0;
    if (paid > total) {
      Toast.error(context, 'Đã thanh toán không được vượt quá tổng tiền.');
      return;
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/export-receipts',
            data: {
              'customerId': _customerId,
              'warehouseId': _warehouseId,
              'receiverName': _text(_receiverName),
              'receiverPhone': _text(_receiverPhone),
              'receiverAddress': _text(_receiverAddress),
              'dateOut': Fmt.apiDateTime(_dateOut),
              'deliveryDate': Fmt.apiDateTime(_deliveryDate),
              'vatId': _vatId,
              'deposit': parseNumber(_deposit.text),
              'paid': paid,
              'note': _text(_note),
              'details': [
                for (final l in _lines)
                  {
                    'productId': l.productId,
                    'quantity': l.quantity,
                    'priceOut': l.unitPrice,
                    'serial': l.serialValue,
                    'warrantyMonths': l.warrantyMonths,
                  },
              ],
            },
          );
      if (!mounted) return;
      Toast.success(
        context,
        'Đã lưu phiếu ${(result as Map?)?['receiptCode'] ?? ''}.',
      );
      context.go('/app/export-receipts');
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final customers = ref.watch(customersLookupProvider);
    final warehouses = ref.watch(warehousesLookupProvider);
    final vats = ref.watch(vatOptionsLookupProvider);
    final products = ref.watch(productsLookupProvider);

    final subtotal = _lines.fold<num>(0, (s, l) => s + l.lineTotal);
    final vatRate =
        (vats.value ?? const [])
            .where((v) => v['id'] == _vatId)
            .map((v) => (v['rate'] as num?) ?? 0)
            .firstOrNull ??
        0;
    final vatAmount = subtotal * vatRate / 100;
    final total = subtotal + vatAmount;
    final paid = parseNumber(_paid.text) ?? 0;

    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeader(
            title: 'Tạo phiếu xuất hàng',
            crumbs: [
              Crumb('Trang chủ', '/app/dashboard'),
              Crumb('Xuất kho', '/app/export-receipts'),
              Crumb('Tạo phiếu xuất'),
            ],
          ),
          SectionCard(
            title: 'Thông tin phiếu',
            child: FormGrid(
              children: [
                customers.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Khách hàng',
                    required: true,
                    hint: 'Chọn khách hàng',
                    value: _customerId,
                    items: [
                      for (final c in list)
                        ((c['id'] as num).toInt(), partyName(c)),
                    ],
                    onChanged: (v) {
                      final customer = list.firstWhere(
                        (c) => c['id'] == v,
                        orElse: () => {},
                      );
                      final party = (customer['party'] as Map?) ?? {};
                      setState(() {
                        _customerId = v;
                        _receiverName.text = '${party['fullName'] ?? ''}';
                        _receiverPhone.text = '${party['phone'] ?? ''}';
                        _receiverAddress.text = '${party['address'] ?? ''}';
                      });
                    },
                  ),
                ),
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Kho xuất',
                    required: true,
                    hint: 'Chọn kho',
                    value: _warehouseId,
                    items: [
                      for (final w in list)
                        ((w['id'] as num).toInt(), '${w['warehouseName']}'),
                    ],
                    onChanged: (v) => setState(() => _warehouseId = v),
                  ),
                ),
                DateField(
                  label: 'Ngày xuất',
                  required: true,
                  value: _dateOut,
                  onChanged: (d) => setState(() => _dateOut = d),
                ),
                AppTextField(label: 'Người nhận', controller: _receiverName),
                AppTextField(
                  label: 'SĐT người nhận',
                  controller: _receiverPhone,
                  keyboardType: TextInputType.phone,
                ),
                AppTextField(
                  label: 'Địa chỉ giao',
                  controller: _receiverAddress,
                ),
                DateField(
                  label: 'Ngày giao',
                  value: _deliveryDate,
                  onChanged: (d) => setState(() => _deliveryDate = d),
                ),
                vats.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int?>(
                    label: 'VAT',
                    value: _vatId,
                    items: [
                      (null, 'Không VAT'),
                      for (final v in list)
                        ((v['id'] as num).toInt(), '${v['vat']}'),
                    ],
                    onChanged: (v) => setState(() => _vatId = v),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          SectionCard(
            title: 'Danh sách hàng hóa',
            child: products.when(
              loading: () => const LoadingView(),
              error: (e, _) => StateView.error(e),
              data: (list) => ReceiptLineEditor(
                lines: _lines,
                products: list,
                priceLabel: 'Đơn giá bán',
                showWarranty: true,
                defaultPrice: (p) => p['defaultPriceOut'] as num?,
                onChanged: () => setState(() {}),
              ),
            ),
          ),
          const SizedBox(height: 16),
          LayoutBuilder(
            builder: (context, constraints) {
              final payment = SectionCard(
                title: 'Thanh toán',
                child: Column(
                  children: [
                    AppTextField(
                      label: 'Đặt cọc (₫)',
                      controller: _deposit,
                      keyboardType: TextInputType.number,
                      inputFormatters: digitsOnly,
                    ),
                    const SizedBox(height: 12),
                    AppTextField(
                      label: 'Đã thanh toán (₫)',
                      controller: _paid,
                      keyboardType: TextInputType.number,
                      inputFormatters: digitsOnly,
                    ),
                    const SizedBox(height: 12),
                    AppTextField(
                      label: 'Ghi chú',
                      controller: _note,
                      maxLines: 2,
                    ),
                  ],
                ),
              );
              final totals = SectionCard(
                title: 'Tổng tiền',
                child: TotalsPanel(
                  rows: [
                    ('Tạm tính', Fmt.money(subtotal), false),
                    ('VAT', Fmt.money(vatAmount), false),
                    ('Tổng cộng', Fmt.money(total), true),
                    ('Đã thanh toán', Fmt.money(paid), false),
                    ('Còn lại', Fmt.money(total - paid), false),
                  ],
                ),
              );
              return constraints.maxWidth < 800
                  ? Column(
                      children: [payment, const SizedBox(height: 16), totals],
                    )
                  : Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(child: payment),
                        const SizedBox(width: 16),
                        Expanded(child: totals),
                      ],
                    );
            },
          ),
          FormFooter(
            saving: _saving,
            onCancel: () => context.go('/app/export-receipts'),
            onSave: () => _save(total),
          ),
        ],
      ),
    );
  }
}
