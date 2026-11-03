import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/form/form_footer.dart';
import '../../shared/form/receipt_line_editor.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

/// Danh sách phiếu nhập — GET /bff/import-receipts. Duyệt/hủy phiếu chờ duyệt ngay trên dòng.
class ImportReceiptListPage extends StatefulWidget {
  const ImportReceiptListPage({super.key});

  @override
  State<ImportReceiptListPage> createState() => _ImportReceiptListPageState();
}

class _ImportReceiptListPageState extends State<ImportReceiptListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách phiếu nhập',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Nhập hàng'),
      Crumb('Danh sách phiếu nhập'),
    ],
    controller: _controller,
    actions: [
      ElevatedButton(
        onPressed: () => context.go('/app/import-receipts/new'),
        child: const Text('Tạo phiếu nhập'),
      ),
    ],
    endpoint: '/bff/import-receipts',
    defaultSort: 'dateIn,desc',
    searchHint: 'Tìm mã phiếu, nhà cung cấp...',
    itemLabel: 'phiếu',
    filters: const [
      ListFilter(
        key: 'status',
        label: 'Trạng thái',
        options: [
          (null, 'Tất cả'),
          ('DRAFT', 'Nháp'),
          ('PENDING', 'Chờ duyệt'),
          ('CONFIRMED', 'Đã nhập kho'),
          ('CANCELLED', 'Đã hủy'),
        ],
      ),
    ],
    columns: [
      TableColumnDef.text(
        'Mã phiếu',
        (r) => '${r['receiptCode']}',
        minWidth: 120,
        sortKey: 'receiptCode',
        bold: true,
      ),
      TableColumnDef.text(
        'Ngày nhập',
        (r) => Fmt.date(r['dateIn']),
        minWidth: 100,
        sortKey: 'dateIn',
      ),
      TableColumnDef.text(
        'Nhà cung cấp',
        (r) => '${r['sourcePartyName'] ?? '—'}',
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
        cell: (r) => Statuses.importReceipt(r['status'] as String?),
      ),
      TableColumnDef(
        label: 'Thao tác',
        minWidth: 110,
        align: Alignment.center,
        cell: (r) => _ImportActions(row: r, onDone: _controller.reload),
      ),
    ],
  );
}

class _ImportActions extends ConsumerWidget {
  const _ImportActions({required this.row, required this.onDone});

  final Map<String, dynamic> row;
  final VoidCallback onDone;

  Future<void> _submitForApproval(BuildContext context, WidgetRef ref) async {
    try {
      await ref
          .read(apiClientProvider)
          .put(
            '/bff/api/import-receipts/${row['id']}',
            data: {'status': 'PENDING'},
          );
      if (context.mounted) {
        Toast.success(context, 'Đã gửi duyệt phiếu ${row['receiptCode']}.');
      }
      onDone();
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  // DELETE = hủy phiếu (backend giữ phiếu ở trạng thái CANCELLED để tra cứu)
  Future<void> _cancel(BuildContext context, WidgetRef ref) async {
    final ok = await confirmDialog(
      context,
      title: 'Hủy phiếu ${row['receiptCode']}',
      message: 'Phiếu được giữ lại để tra cứu nhưng không còn hiệu lực.',
      confirmLabel: 'Hủy phiếu',
      danger: true,
    );
    if (!ok) return;
    try {
      await ref
          .read(apiClientProvider)
          .delete('/bff/api/import-receipts/${row['id']}');
      if (context.mounted) {
        Toast.success(context, 'Đã hủy phiếu ${row['receiptCode']}.');
      }
      onDone();
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = row['status'];
    if (status == 'CONFIRMED' || status == 'CANCELLED') return const Text('—');
    // Bước "xác nhận nhập kho" (CONFIRMED, cộng tồn) chưa có API ở backend (chờ StockService)
    return PopupMenuButton<String>(
      icon: const Icon(Icons.more_horiz),
      onSelected: (v) => v == 'PENDING'
          ? _submitForApproval(context, ref)
          : _cancel(context, ref),
      itemBuilder: (_) => [
        if (status == 'DRAFT')
          const PopupMenuItem(value: 'PENDING', child: Text('Gửi duyệt')),
        const PopupMenuItem(value: 'CANCELLED', child: Text('Hủy phiếu')),
      ],
    );
  }
}

/// Figma 978:1453 "Nhập hàng / Tạo phiếu nhập hàng" — POST /bff/api/import-receipts.
class ImportReceiptFormPage extends ConsumerStatefulWidget {
  const ImportReceiptFormPage({super.key});

  @override
  ConsumerState<ImportReceiptFormPage> createState() =>
      _ImportReceiptFormPageState();
}

class _ImportReceiptFormPageState extends ConsumerState<ImportReceiptFormPage> {
  final _form = GlobalKey<FormState>();
  final _note = TextEditingController();
  final List<ReceiptLine> _lines = [];
  int? _supplierPartyId;
  int? _warehouseId;
  int? _vatId;
  DateTime? _dateIn = DateTime.now();
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _warehouseId = ref.read(warehouseScopeProvider);
  }

  @override
  void dispose() {
    _note.dispose();
    for (final l in _lines) {
      l.dispose();
    }
    super.dispose();
  }

  Future<void> _save(String status) async {
    if (!_form.currentState!.validate()) return;
    if (_lines.isEmpty || _lines.any((l) => l.quantity <= 0)) {
      Toast.error(context, 'Phiếu cần ít nhất một dòng hàng có số lượng > 0.');
      return;
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/import-receipts',
            data: {
              'sourcePartyId': _supplierPartyId,
              'warehouseId': _warehouseId,
              'dateIn': Fmt.apiDateTime(_dateIn),
              'vatId': _vatId,
              'status': status,
              'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
              'details': [
                for (final l in _lines)
                  {
                    'productId': l.productId,
                    'quantity': l.quantity,
                    'priceIn': l.unitPrice,
                    'serial': l.serialValue,
                  },
              ],
            },
          );
      if (!mounted) return;
      Toast.success(
        context,
        'Đã lưu phiếu ${(result as Map?)?['receiptCode'] ?? ''}.',
      );
      context.go('/app/import-receipts');
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final suppliers = ref.watch(suppliersLookupProvider);
    final warehouses = ref.watch(warehousesLookupProvider);
    final vats = ref.watch(vatOptionsLookupProvider);
    final products = ref.watch(productsLookupProvider);
    final session = ref.watch(sessionProvider).value;

    final subtotal = _lines.fold<num>(0, (s, l) => s + l.lineTotal);
    final vatRate =
        (vats.value ?? const [])
            .where((v) => v['id'] == _vatId)
            .map((v) => (v['rate'] as num?) ?? 0)
            .firstOrNull ??
        0;
    final vatAmount = subtotal * vatRate / 100;

    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const PageHeader(
            title: 'Tạo phiếu nhập hàng',
            crumbs: [
              Crumb('Trang chủ', '/app/dashboard'),
              Crumb('Nhập hàng', '/app/import-receipts'),
              Crumb('Tạo phiếu nhập'),
            ],
          ),
          SectionCard(
            title: 'Thông tin phiếu',
            child: FormGrid(
              children: [
                AppTextField(
                  label: 'Mã phiếu',
                  readOnly: true,
                  controller: TextEditingController(text: 'Tự sinh khi lưu'),
                ),
                suppliers.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Nhà cung cấp',
                    required: true,
                    hint: 'Chọn nhà cung cấp',
                    value: _supplierPartyId,
                    items: [
                      for (final s in list)
                        (
                          ((s['party'] as Map)['id'] as num).toInt(),
                          'NCC${'${s['id']}'.padLeft(3, '0')} · ${partyName(s)}',
                        ),
                    ],
                    onChanged: (v) => setState(() => _supplierPartyId = v),
                  ),
                ),
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Kho nhập',
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
                  label: 'Ngày nhập',
                  required: true,
                  value: _dateIn,
                  onChanged: (d) => setState(() => _dateIn = d),
                ),
                vats.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int?>(
                    label: 'VAT đầu vào',
                    value: _vatId,
                    items: [
                      (null, 'Không VAT'),
                      for (final v in list)
                        ((v['id'] as num).toInt(), '${v['vat']}'),
                    ],
                    onChanged: (v) => setState(() => _vatId = v),
                  ),
                ),
                AppTextField(
                  label: 'Người lập',
                  readOnly: true,
                  controller: TextEditingController(
                    text: session?.displayName ?? '',
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
                priceLabel: 'Đơn giá nhập',
                defaultPrice: (p) => p['defaultPriceIn'] as num?,
                onChanged: () => setState(() {}),
              ),
            ),
          ),
          const SizedBox(height: 16),
          LayoutBuilder(
            builder: (context, constraints) {
              final note = SectionCard(
                title: 'Ghi chú',
                child: AppTextField(
                  label: 'Ghi chú cho phiếu',
                  controller: _note,
                  maxLines: 3,
                ),
              );
              final totals = SectionCard(
                title: 'Thanh toán',
                child: TotalsPanel(
                  rows: [
                    ('Tạm tính', Fmt.money(subtotal), false),
                    ('VAT', Fmt.money(vatAmount), false),
                    ('Tổng cộng', Fmt.money(subtotal + vatAmount), true),
                  ],
                ),
              );
              return constraints.maxWidth < 800
                  ? Column(children: [note, const SizedBox(height: 16), totals])
                  : Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(child: note),
                        const SizedBox(width: 16),
                        Expanded(child: totals),
                      ],
                    );
            },
          ),
          FormFooter(
            saving: _saving,
            onCancel: () => context.go('/app/import-receipts'),
            onSaveDraft: () => _save('DRAFT'),
            onSave: () => _save('PENDING'),
            saveLabel: 'Lưu & gửi duyệt',
          ),
        ],
      ),
    );
  }
}
