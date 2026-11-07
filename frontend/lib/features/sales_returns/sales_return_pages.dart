import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
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
import '../common/doc_widgets.dart';
import '../common/lookups.dart';

List<PrintDoc> salesReturnPrintDocs(Object? id) => [
  PrintDoc('Phiếu trả hàng / đổi hàng', '/bff/api/sales-returns/$id/print'),
];

StatusChip _typeChip(Object? type) => switch (type) {
  'EXCHANGE' => const StatusChip(label: 'Đổi hàng', tone: ChipTone.info),
  'WARRANTY' => const StatusChip(label: 'Bảo hành', tone: ChipTone.purple),
  _ => const StatusChip(label: 'Trả hàng', tone: ChipTone.warning),
};

String _conditionLabel(Object? c) => switch (c) {
  'RESELLABLE' => 'Còn bán được',
  'DAMAGED' => 'Hư hỏng',
  'TO_WARRANTY' => 'Chuyển bảo hành',
  _ => '${c ?? '—'}',
};

String _refundText(Object? value) {
  final v = Fmt.toNum(value) ?? 0;
  return v < 0 ? 'Thu thêm ${Fmt.money(-v)}' : 'Hoàn ${Fmt.money(v)}';
}

/// Figma 1138:10049 "Đổi trả / Danh sách phiếu" — GET /bff/sales-returns.
class SalesReturnListPage extends StatelessWidget {
  const SalesReturnListPage({super.key});

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách phiếu đổi / trả',
    subtitle: 'Trả hàng hoàn tiền, đổi sản phẩm hoặc chuyển bảo hành',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Xuất kho'),
      Crumb('Danh sách đổi trả'),
    ],
    actions: [
      ElevatedButton(
        onPressed: () => context.go('/app/sales-returns/new'),
        child: const Text('Tạo phiếu đổi / trả'),
      ),
    ],
    endpoint: '/bff/sales-returns',
    defaultSort: 'returnDate,desc',
    searchHint: 'Tìm mã phiếu, phiếu xuất gốc, khách hàng...',
    itemLabel: 'phiếu',
    onRowTap: (r) => context.go('/app/sales-returns/${r['id']}'),
    filters: const [
      ListFilter(
        key: 'returnType',
        label: 'Loại',
        options: [
          (null, 'Tất cả'),
          ('REFUND', 'Trả hàng'),
          ('EXCHANGE', 'Đổi hàng'),
          ('WARRANTY', 'Bảo hành'),
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
        'Phiếu gốc',
        (r) => '${r['exportReceiptCode'] ?? '—'}',
        minWidth: 110,
      ),
      TableColumnDef.text(
        'Khách hàng',
        (r) => '${r['partyName'] ?? '—'}',
        flex: 3,
        minWidth: 160,
      ),
      TableColumnDef.text(
        'Ngày trả',
        (r) => Fmt.date(r['returnDate']),
        minWidth: 100,
        sortKey: 'returnDate',
      ),
      TableColumnDef(
        label: 'Loại',
        minWidth: 100,
        cell: (r) => _typeChip(r['returnType']),
      ),
      TableColumnDef.text(
        'Phí',
        (r) => Fmt.money(r['feeAmount']),
        minWidth: 100,
        align: Alignment.centerRight,
      ),
      TableColumnDef(
        label: 'Hoàn / Thu thêm',
        minWidth: 130,
        align: Alignment.centerRight,
        cell: (r) => Text(_refundText(r['refundAmount'])),
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 120,
        cell: (r) => Statuses.receipt(r['status'] as String?),
      ),
      TableColumnDef(
        label: 'In',
        flex: 1,
        minWidth: 70,
        align: Alignment.center,
        cell: (r) =>
            PrintButton(compact: true, docs: salesReturnPrintDocs(r['id'])),
      ),
    ],
  );
}

final _exportReceiptsProvider = FutureProvider.autoDispose<List<Json>>((
  ref,
) async {
  final page = await ref
      .watch(apiClientProvider)
      .getPage(
        '/bff/export-receipts',
        query: {'size': 100, 'sort': 'dateOut,desc', 'keyword': ''},
      );
  return [
    for (final r in page.items)
      if (r['status'] != 'CANCELLED' && r['status'] != 'DRAFT') r,
  ];
});

final _feePoliciesProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/return-fee-policies'),
);

class _ReturnLine {
  _ReturnLine(this.detail)
    : qty = TextEditingController(text: '${detail['quantity'] ?? 1}');

  final Json detail;
  final TextEditingController qty;
  bool selected = true;
  String condition = 'RESELLABLE';

  int get exportDetailId => (detail['id'] as num).toInt();
  int get sold => (Fmt.toNum(detail['quantity']) ?? 0).toInt();
  int get quantity => parseNumber(qty.text)?.toInt() ?? 0;
  num get price => Fmt.toNum(detail['priceOut']) ?? 0;
}

/// Tạo phiếu trả / đổi từ phiếu xuất gốc — POST /bff/api/sales-returns (SalesReturnCreationRequest).
/// Phí và số tiền hoàn do backend tính theo ReturnFeePolicy; form chỉ hiển thị ước tính.
class SalesReturnFormPage extends ConsumerStatefulWidget {
  const SalesReturnFormPage({super.key, this.exportReceiptId});

  final int? exportReceiptId;

  @override
  ConsumerState<SalesReturnFormPage> createState() =>
      _SalesReturnFormPageState();
}

class _SalesReturnFormPageState extends ConsumerState<SalesReturnFormPage> {
  final _form = GlobalKey<FormState>();
  final _note = TextEditingController();
  final List<_ReturnLine> _lines = [];
  Json? _receipt;
  int? _warehouseId;
  int? _newExportId;
  String _type = 'REFUND';
  String _direction = 'EQUAL_HIGHER';
  DateTime? _date = DateTime.now();
  bool _saving = false;
  bool _loadingReceipt = false;

  @override
  void initState() {
    super.initState();
    if (widget.exportReceiptId != null) _pick(widget.exportReceiptId!);
  }

  @override
  void dispose() {
    _note.dispose();
    for (final l in _lines) {
      l.qty.dispose();
    }
    super.dispose();
  }

  Future<void> _pick(int id) async {
    setState(() => _loadingReceipt = true);
    try {
      final r = await ref
          .read(apiClientProvider)
          .get('/bff/api/export-receipts/$id');
      final receipt = Map<String, dynamic>.from(r as Map);
      for (final l in _lines) {
        l.qty.dispose();
      }
      setState(() {
        _receipt = receipt;
        _warehouseId = (receipt['warehouseId'] as num?)?.toInt();
        _lines
          ..clear()
          ..addAll([
            for (final d in (receipt['details'] as List? ?? const []))
              _ReturnLine(Map<String, dynamic>.from(d as Map)),
          ]);
      });
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _loadingReceipt = false);
    }
  }

  int? get _days {
    final delivered = Fmt.parseDate(_receipt?['deliveryDate']);
    if (delivered == null || _date == null) return null;
    return DateTime(_date!.year, _date!.month, _date!.day)
        .difference(DateTime(delivered.year, delivered.month, delivered.day))
        .inDays;
  }

  String get _actionType => _type == 'EXCHANGE'
      ? (_direction == 'LOWER' ? 'EXCHANGE_LOWER' : 'EXCHANGE_EQUAL_HIGHER')
      : 'REFUND';

  num get _originalValue => _lines
      .where((l) => l.selected)
      .fold<num>(0, (s, l) => s + l.quantity * l.price);

  /// Bậc phí áp dụng (ước tính): bậc nhỏ nhất có maxDays >= số ngày kể từ khi giao.
  Json? _policyFor(List<Json> policies) {
    final days = _days;
    if (days == null) return null;
    final fit =
        policies
            .where(
              (p) =>
                  p['actionType'] == _actionType &&
                  (Fmt.toNum(p['maxDays']) ?? 0) >= days,
            )
            .toList()
          ..sort(
            (a, b) => (Fmt.toNum(a['maxDays']) ?? 0).compareTo(
              Fmt.toNum(b['maxDays']) ?? 0,
            ),
          );
    return fit.isEmpty ? null : fit.first;
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final chosen = _lines.where((l) => l.selected).toList();
    if (_receipt == null || chosen.isEmpty) {
      Toast.error(context, 'Chọn phiếu xuất và ít nhất một dòng hàng cần trả.');
      return;
    }
    for (final l in chosen) {
      if (l.quantity <= 0 || l.quantity > l.sold) {
        Toast.error(
          context,
          'Số lượng trả của ${l.detail['productName']} phải từ 1 đến ${l.sold}.',
        );
        return;
      }
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/sales-returns',
            data: {
              'exportReceiptId': _receipt!['id'],
              'warehouseId': _warehouseId,
              'returnType': _type,
              if (_type == 'EXCHANGE') 'exchangeDirection': _direction,
              if (_type == 'EXCHANGE' && _newExportId != null)
                'newExportReceiptId': _newExportId,
              'returnDate': Fmt.apiDateTime(_date),
              'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
              'details': [
                for (final l in chosen)
                  {
                    'exportDetailId': l.exportDetailId,
                    'quantity': l.quantity,
                    'returnCondition': _type == 'WARRANTY'
                        ? 'TO_WARRANTY'
                        : l.condition,
                  },
              ],
            },
          );
      if (!mounted) return;
      final m = result as Map?;
      Toast.success(context, 'Đã lập phiếu ${m?['receiptCode'] ?? ''}.');
      context.go(
        m?['id'] == null
            ? '/app/sales-returns'
            : '/app/sales-returns/${m!['id']}',
      );
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final receipts = ref.watch(_exportReceiptsProvider);
    final warehouses = ref.watch(warehousesLookupProvider);
    final policies = ref.watch(_feePoliciesProvider).value ?? const <Json>[];
    final policy = _policyFor(policies);
    final original = _originalValue;
    final feePercent = Fmt.toNum(policy?['feePercent']) ?? 0;
    final blocked =
        _type != 'WARRANTY' &&
        _receipt != null &&
        _days != null &&
        (policy == null || policy['isAllowed'] == false);
    final fee = _type == 'WARRANTY' ? 0 : original * feePercent / 100;

    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: 'Tạo phiếu đổi / trả hàng',
            crumbs: const [
              Crumb('Trang chủ', '/app/dashboard'),
              Crumb('Xuất kho'),
              Crumb('Danh sách đổi trả', '/app/sales-returns'),
              Crumb('Tạo phiếu'),
            ],
          ),
          SectionCard(
            title: 'Thông tin phiếu',
            child: FormGrid(
              children: [
                receipts.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Phiếu xuất gốc',
                    required: true,
                    hint: 'Chọn phiếu xuất',
                    value: (_receipt?['id'] as num?)?.toInt(),
                    items: [
                      for (final r in list)
                        (
                          (r['id'] as num).toInt(),
                          '${r['receiptCode']} · ${r['customerName'] ?? ''}',
                        ),
                    ],
                    onChanged: (v) {
                      if (v != null) _pick(v);
                    },
                  ),
                ),
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Kho tiếp nhận',
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
                AppDropdown<String>(
                  label: 'Loại phiếu',
                  required: true,
                  value: _type,
                  items: const [
                    ('REFUND', 'Trả hàng, hoàn tiền'),
                    ('EXCHANGE', 'Đổi sang sản phẩm khác'),
                    ('WARRANTY', 'Chuyển bảo hành'),
                  ],
                  onChanged: (v) => setState(() => _type = v ?? 'REFUND'),
                ),
                if (_type == 'EXCHANGE')
                  AppDropdown<String>(
                    label: 'Hướng đổi',
                    required: true,
                    value: _direction,
                    items: const [
                      ('EQUAL_HIGHER', 'Đổi sang mã giá bằng / cao hơn'),
                      ('LOWER', 'Đổi sang mã giá thấp hơn'),
                    ],
                    onChanged: (v) =>
                        setState(() => _direction = v ?? 'EQUAL_HIGHER'),
                  ),
                if (_type == 'EXCHANGE')
                  receipts.when(
                    loading: () => const LinearProgressIndicator(),
                    error: (e, _) => Text('$e'),
                    data: (list) => AppDropdown<int?>(
                      label: 'Phiếu xuất đổi hàng (nếu đã lập)',
                      hint: 'Chưa có',
                      value: _newExportId,
                      items: [
                        (null, 'Chưa có'),
                        for (final r in list)
                          if (r['id'] != _receipt?['id'])
                            (
                              (r['id'] as num).toInt(),
                              '${r['receiptCode']} · ${Fmt.money(r['totalAmount'])}',
                            ),
                      ],
                      onChanged: (v) => setState(() => _newExportId = v),
                    ),
                  ),
                DateTimeField(
                  label: 'Ngày giờ trả',
                  required: true,
                  value: _date,
                  onChanged: (d) => setState(() => _date = d),
                ),
                AppTextField(label: 'Ghi chú', controller: _note),
              ],
            ),
          ),
          const SizedBox(height: 16),
          SectionCard(
            title: 'Hàng trả / đổi',
            child: _loadingReceipt
                ? const LoadingView()
                : _receipt == null
                ? Text(
                    'Chọn phiếu xuất gốc để hiển thị các dòng hàng đã bán.',
                    style: TextStyle(color: c.muted),
                  )
                : DocTable(
                    headers: const [
                      'Trả',
                      'Sản phẩm',
                      'Serial',
                      'Đã bán',
                      'SL trả',
                      'Đơn giá',
                      'Tình trạng',
                    ],
                    flex: const [1, 5, 3, 2, 2, 3, 4],
                    rightAligned: const {3, 5},
                    minWidth: 860,
                    rows: [
                      for (final l in _lines)
                        [
                          Checkbox(
                            value: l.selected,
                            onChanged: (v) =>
                                setState(() => l.selected = v ?? false),
                          ),
                          docText(context, l.detail['productName'], bold: true),
                          docText(context, l.detail['serial']),
                          docText(context, Fmt.number(l.sold)),
                          SizedBox(
                            width: 70,
                            child: TextField(
                              controller: l.qty,
                              enabled: l.selected,
                              textAlign: TextAlign.right,
                              keyboardType: TextInputType.number,
                              inputFormatters: digitsOnly,
                              style: const TextStyle(fontSize: 13),
                              onChanged: (_) => setState(() {}),
                            ),
                          ),
                          docText(context, Fmt.money(l.price)),
                          _type == 'WARRANTY'
                              ? docText(context, _conditionLabel('TO_WARRANTY'))
                              : DropdownButton<String>(
                                  value: l.condition,
                                  isExpanded: true,
                                  underline: const SizedBox.shrink(),
                                  style: TextStyle(fontSize: 13, color: c.text),
                                  items: [
                                    for (final k in [
                                      'RESELLABLE',
                                      'DAMAGED',
                                      'TO_WARRANTY',
                                    ])
                                      DropdownMenuItem(
                                        value: k,
                                        child: Text(_conditionLabel(k)),
                                      ),
                                  ],
                                  onChanged: l.selected
                                      ? (v) => setState(
                                          () => l.condition = v ?? l.condition,
                                        )
                                      : null,
                                ),
                        ],
                    ],
                  ),
          ),
          const SizedBox(height: 16),
          SectionCard(
            title: 'Phí đổi / trả (ước tính)',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                TotalsPanel(
                  rows: [
                    (
                      'Số ngày kể từ khi giao',
                      _days == null ? '—' : '$_days ngày',
                      false,
                    ),
                    (
                      'Bậc phí áp dụng',
                      _type == 'WARRANTY'
                          ? 'Bảo hành không tính phí'
                          : (policy == null
                                ? (_days == null ? '—' : 'Không có bậc phù hợp')
                                : '${policy['tierName']} · ${Fmt.number(feePercent)}%'),
                      false,
                    ),
                    ('Giá trị hàng trả', Fmt.money(original), false),
                    ('Phí', Fmt.money(fee), false),
                    (
                      'Hoàn lại khách (chưa trừ phiếu đổi)',
                      Fmt.money(original - fee),
                      true,
                    ),
                  ],
                ),
                if (blocked)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      'Quá thời hạn hoặc bậc phí bị khóa: hệ thống sẽ chuyển phiếu sang trạng thái Từ chối.',
                      style: TextStyle(color: c.danger, fontSize: 13),
                    ),
                  ),
                const HintText(
                  'Phí và số tiền hoàn chính thức do hệ thống tính theo chính sách phí khi lưu phiếu.',
                ),
              ],
            ),
          ),
          FormFooter(
            saving: _saving,
            saveLabel: 'Lập phiếu',
            onCancel: () => context.go('/app/sales-returns'),
            onSave: _save,
          ),
        ],
      ),
    );
  }
}

final _salesReturnProvider = FutureProvider.autoDispose.family<Json, int>((
  ref,
  id,
) async {
  final r = await ref
      .watch(apiClientProvider)
      .get('/bff/api/sales-returns/$id');
  return Map<String, dynamic>.from(r as Map);
});

/// Chi tiết phiếu trả / đổi hàng bán — GET /bff/api/sales-returns/{id}.
class SalesReturnDetailPage extends ConsumerWidget {
  const SalesReturnDetailPage({super.key, required this.id});

  final int id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_salesReturnProvider(id));
    return async.when(
      loading: () => const LoadingView(),
      error: (e, _) => StateView.error(
        e,
        onRetry: () => ref.invalidate(_salesReturnProvider(id)),
      ),
      data: (d) {
        final details = [
          for (final x in (d['details'] as List? ?? const []))
            Map<String, dynamic>.from(x as Map),
        ];
        final exchange = d['returnType'] == 'EXCHANGE';
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PageHeader(
              title: 'Phiếu ${d['receiptCode'] ?? ''}',
              crumbs: const [
                Crumb('Trang chủ', '/app/dashboard'),
                Crumb('Xuất kho'),
                Crumb('Danh sách đổi trả', '/app/sales-returns'),
                Crumb('Chi tiết'),
              ],
              actions: [PrintButton(docs: salesReturnPrintDocs(id))],
            ),
            infoCard('Thông tin phiếu', [
              ('Mã phiếu', InfoGrid.text(context, '${d['receiptCode']}')),
              ('Loại', _typeChip(d['returnType'])),
              ('Trạng thái', Statuses.receipt(d['status'] as String?)),
              (
                'Phiếu xuất gốc',
                InfoGrid.text(context, d['exportReceiptCode'] as String?),
              ),
              ('Khách hàng', InfoGrid.text(context, d['partyName'] as String?)),
              (
                'Kho tiếp nhận',
                InfoGrid.text(context, d['warehouseCode'] as String?),
              ),
              (
                'Ngày trả',
                InfoGrid.text(context, Fmt.dateTime(d['returnDate'])),
              ),
              (
                'Số ngày kể từ khi giao',
                InfoGrid.text(
                  context,
                  d['daysSinceDelivery'] == null
                      ? null
                      : '${d['daysSinceDelivery']} ngày',
                ),
              ),
              (
                'Người lập',
                InfoGrid.text(context, d['createdByName'] as String?),
              ),
              if (exchange)
                (
                  'Hướng đổi',
                  InfoGrid.text(
                    context,
                    d['exchangeDirection'] == 'LOWER'
                        ? 'Sang mã giá thấp hơn'
                        : 'Sang mã giá bằng / cao hơn',
                  ),
                ),
              if (exchange)
                (
                  'Phiếu xuất đổi hàng',
                  InfoGrid.text(context, d['newExportReceiptCode'] as String?),
                ),
              ('Ghi chú', InfoGrid.text(context, d['note'] as String?)),
            ]),
            const SizedBox(height: 16),
            SectionCard(
              title: 'Hàng trả',
              child: DocTable(
                headers: const ['Sản phẩm', 'Serial', 'SL', 'Tình trạng'],
                flex: const [5, 3, 1, 3],
                rightAligned: const {2},
                rows: [
                  for (final x in details)
                    [
                      docText(context, x['productName'], bold: true),
                      docText(context, x['serial']),
                      docText(context, Fmt.number(x['quantity'])),
                      docText(context, _conditionLabel(x['returnCondition'])),
                    ],
                ],
              ),
            ),
            const SizedBox(height: 16),
            SectionCard(
              title: 'Phí và hoàn tiền',
              child: TotalsPanel(
                rows: [
                  ('Bậc phí', '${d['policyTierName'] ?? '—'}', false),
                  ('Giá trị gốc', Fmt.money(d['originalValue']), false),
                  (
                    'Phí (${Fmt.number(d['feePercent'])}%)',
                    Fmt.money(d['feeAmount']),
                    false,
                  ),
                  ('Hoàn / thu thêm', _refundText(d['refundAmount']), true),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}
