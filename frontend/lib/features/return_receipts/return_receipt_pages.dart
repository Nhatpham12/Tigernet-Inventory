import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/form/form_footer.dart';
import '../../shared/form/receipt_line_editor.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../common/doc_widgets.dart';
import '../common/lookups.dart';

List<PrintDoc> returnReceiptPrintDocs(Object? id) => [
  PrintDoc('Biên bản trả hàng', '/bff/api/return-receipts/$id/print'),
];

final _returnReceiptsProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/return-receipts'),
);

/// Danh sách phiếu trả hàng thuê / mượn — GET /bff/api/return-receipts.
/// (BFF chưa có view phân trang `/bff/return-receipts` nên lọc/tìm tại máy.)
class ReturnReceiptListPage extends ConsumerWidget {
  const ReturnReceiptListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_returnReceiptsProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: 'Danh sách phiếu trả hàng',
          subtitle:
              'Trả hàng thuê / mượn, phạt quá hạn, bồi thường và hoàn cọc',
          crumbs: const [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Cho thuê'),
            Crumb('Phiếu trả hàng'),
          ],
          actions: [
            ElevatedButton(
              onPressed: () => context.go('/app/return-receipts/new'),
              child: const Text('Tạo phiếu trả'),
            ),
          ],
        ),
        async.when(
          loading: () => const LoadingView(),
          error: (e, _) => StateView.error(
            e,
            onRetry: () => ref.invalidate(_returnReceiptsProvider),
          ),
          data: (rows) => ClientTable(
            rows: rows,
            searchHint: 'Tìm mã phiếu trả, phiếu thuê...',
            onRowTap: (r) => context.go('/app/return-receipts/${r['id']}'),
            columns: [
              TableColumnDef.text(
                'Mã phiếu trả',
                (r) => '${r['receiptCode']}',
                minWidth: 120,
                bold: true,
              ),
              TableColumnDef.text(
                'Phiếu thuê / mượn',
                (r) => '${r['rentalReceiptCode'] ?? '—'}',
                minWidth: 130,
              ),
              TableColumnDef.text(
                'Kho nhận',
                (r) => '${r['warehouseName'] ?? '—'}',
                minWidth: 110,
              ),
              TableColumnDef.text(
                'Ngày trả',
                (r) => Fmt.date(r['returnDate']),
                minWidth: 100,
              ),
              TableColumnDef.text(
                'Quá hạn',
                (r) => '${r['daysOverdue'] ?? 0} ngày',
                flex: 1,
                minWidth: 80,
                align: Alignment.centerRight,
              ),
              TableColumnDef.text(
                'Phạt',
                (r) => Fmt.money(r['penaltyAmount']),
                minWidth: 100,
                align: Alignment.centerRight,
              ),
              TableColumnDef.text(
                'Bồi thường',
                (r) => Fmt.money(r['compensationAmount']),
                minWidth: 100,
                align: Alignment.centerRight,
              ),
              TableColumnDef.text(
                'Hoàn cọc',
                (r) => Fmt.money(r['depositRefund']),
                minWidth: 100,
                align: Alignment.centerRight,
              ),
              TableColumnDef(
                label: 'In',
                flex: 1,
                minWidth: 70,
                align: Alignment.center,
                cell: (r) => PrintButton(
                  compact: true,
                  docs: returnReceiptPrintDocs(r['id']),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

final _openRentalsProvider = FutureProvider.autoDispose<List<Json>>((
  ref,
) async {
  final page = await ref
      .watch(apiClientProvider)
      .getPage(
        '/bff/rental-receipts',
        query: {
          'size': 100,
          'state': 'NOT_RETURNED',
          'sort': 'dateOut,desc',
          'keyword': '',
        },
      );
  return page.items;
});

const _conditions = ['Nguyên vẹn', 'Hư hỏng', 'Mất'];

class _RLine {
  _RLine(this.detail)
    : qty = TextEditingController(text: '${detail['quantity'] ?? 1}');

  final Json detail;
  final TextEditingController qty;
  bool selected = true;
  String condition = _conditions.first;

  int get rentalDetailId => (detail['id'] as num).toInt();
  int get max => (Fmt.toNum(detail['quantity']) ?? 0).toInt();
  int get quantity => parseNumber(qty.text)?.toInt() ?? 0;
}

/// Tạo phiếu trả hàng thuê / mượn — POST /bff/api/return-receipts (ReturnReceiptCreationRequest).
/// Tiền phạt quá hạn do backend tính; form nhập bồi thường và cọc hoàn lại.
class ReturnReceiptFormPage extends ConsumerStatefulWidget {
  const ReturnReceiptFormPage({super.key, this.rentalId});

  final int? rentalId;

  @override
  ConsumerState<ReturnReceiptFormPage> createState() =>
      _ReturnReceiptFormPageState();
}

class _ReturnReceiptFormPageState extends ConsumerState<ReturnReceiptFormPage> {
  final _form = GlobalKey<FormState>();
  final _compensation = TextEditingController();
  final _deposit = TextEditingController();
  final _note = TextEditingController();
  final List<_RLine> _lines = [];
  Json? _rental;
  int? _warehouseId;
  DateTime? _date = DateTime.now();
  bool _saving = false;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    if (widget.rentalId != null) _pick(widget.rentalId!);
  }

  @override
  void dispose() {
    _compensation.dispose();
    _deposit.dispose();
    _note.dispose();
    for (final l in _lines) {
      l.qty.dispose();
    }
    super.dispose();
  }

  Future<void> _pick(int id) async {
    setState(() => _loading = true);
    try {
      final r = await ref
          .read(apiClientProvider)
          .get('/bff/api/rental-receipts/$id');
      final rental = Map<String, dynamic>.from(r as Map);
      for (final l in _lines) {
        l.qty.dispose();
      }
      setState(() {
        _rental = rental;
        _warehouseId = (rental['warehouseId'] as num?)?.toInt();
        _deposit.text = '${(Fmt.toNum(rental['deposit']) ?? 0).toInt()}';
        _lines
          ..clear()
          ..addAll([
            for (final d in (rental['details'] as List? ?? const []))
              if ((d as Map)['isReturned'] != true)
                _RLine(Map<String, dynamic>.from(d)),
          ]);
      });
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  int get _overdueDays {
    final due = Fmt.parseDate(_rental?['dueDate']);
    if (due == null || _date == null) return 0;
    final d = _date!.difference(due).inDays;
    return d < 0 ? 0 : d;
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final chosen = _lines.where((l) => l.selected).toList();
    if (_rental == null || chosen.isEmpty) {
      Toast.error(context, 'Chọn phiếu thuê và ít nhất một dòng hàng trả lại.');
      return;
    }
    for (final l in chosen) {
      if (l.quantity <= 0 || l.quantity > l.max) {
        Toast.error(
          context,
          'Số lượng trả của ${l.detail['productName']} phải từ 1 đến ${l.max}.',
        );
        return;
      }
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/return-receipts',
            data: {
              'rentalReceiptId': _rental!['id'],
              'warehouseId': _warehouseId,
              'returnDate': Fmt.apiDateTime(_date),
              'compensationAmount': parseNumber(_compensation.text) ?? 0,
              'depositRefund': parseNumber(_deposit.text) ?? 0,
              'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
              'details': [
                for (final l in chosen)
                  {
                    'rentalDetailId': l.rentalDetailId,
                    'quantity': l.quantity,
                    'conditionNote': l.condition,
                  },
              ],
            },
          );
      if (!mounted) return;
      final m = result as Map?;
      Toast.success(context, 'Đã lập phiếu trả ${m?['receiptCode'] ?? ''}.');
      context.go(
        m?['id'] == null
            ? '/app/return-receipts'
            : '/app/return-receipts/${m!['id']}',
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
    final rentals = ref.watch(_openRentalsProvider);
    final warehouses = ref.watch(warehousesLookupProvider);
    final lend = _rental?['rentalType'] == 'LEND';
    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: 'Tạo phiếu trả hàng',
            crumbs: const [
              Crumb('Trang chủ', '/app/dashboard'),
              Crumb('Cho thuê'),
              Crumb('Phiếu trả hàng', '/app/return-receipts'),
              Crumb('Tạo phiếu'),
            ],
          ),
          SectionCard(
            title: 'Thông tin phiếu trả',
            child: FormGrid(
              children: [
                rentals.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) {
                    final id = (_rental?['id'] as num?)?.toInt();
                    final items = [
                      for (final r in list)
                        (
                          (r['id'] as num).toInt(),
                          '${r['receiptCode']} · ${r['customerName'] ?? ''}',
                        ),
                    ];
                    if (id != null && !items.any((e) => e.$1 == id)) {
                      items.add((id, '${_rental!['receiptCode']}'));
                    }
                    return AppDropdown<int>(
                      label: 'Phiếu thuê / mượn',
                      required: true,
                      hint: 'Chọn phiếu chưa trả',
                      value: id,
                      items: items,
                      onChanged: (v) {
                        if (v != null) _pick(v);
                      },
                    );
                  },
                ),
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Kho nhận lại',
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
            title: 'Hàng trả lại',
            child: _loading
                ? const LoadingView()
                : _rental == null
                ? Text(
                    'Chọn phiếu thuê / mượn để hiển thị các dòng hàng chưa trả.',
                    style: TextStyle(color: c.muted),
                  )
                : _lines.isEmpty
                ? const Text('Phiếu này không còn dòng hàng nào chưa trả.')
                : DocTable(
                    headers: const [
                      'Trả',
                      'Sản phẩm',
                      'Serial',
                      'Đang thuê',
                      'SL trả',
                      'Tình trạng',
                    ],
                    flex: const [1, 5, 3, 2, 2, 3],
                    rightAligned: const {3},
                    minWidth: 760,
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
                          docText(context, Fmt.number(l.max)),
                          SizedBox(
                            width: 70,
                            child: TextField(
                              controller: l.qty,
                              enabled: l.selected,
                              textAlign: TextAlign.right,
                              keyboardType: TextInputType.number,
                              inputFormatters: digitsOnly,
                              style: const TextStyle(fontSize: 13),
                            ),
                          ),
                          DropdownButton<String>(
                            value: l.condition,
                            isExpanded: true,
                            underline: const SizedBox.shrink(),
                            style: TextStyle(fontSize: 13, color: c.text),
                            items: [
                              for (final k in _conditions)
                                DropdownMenuItem(value: k, child: Text(k)),
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
            title: 'Phạt, bồi thường và hoàn cọc',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                FormGrid(
                  children: [
                    AppTextField(
                      label: 'Bồi thường hư hỏng / mất (₫)',
                      controller: _compensation,
                      keyboardType: TextInputType.number,
                      inputFormatters: digitsOnly,
                    ),
                    if (!lend)
                      AppTextField(
                        label: 'Cọc hoàn lại cho khách (₫)',
                        controller: _deposit,
                        keyboardType: TextInputType.number,
                        inputFormatters: digitsOnly,
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                TotalsPanel(
                  rows: [
                    (
                      'Hạn trả',
                      _rental == null ? '—' : Fmt.date(_rental!['dueDate']),
                      false,
                    ),
                    ('Số ngày quá hạn (ước tính)', '$_overdueDays ngày', false),
                    if (!lend)
                      (
                        'Tiền đặt cọc đã thu',
                        Fmt.money(_rental?['deposit']),
                        false,
                      ),
                  ],
                ),
                const HintText(
                  'Tiền phạt quá hạn do hệ thống tính theo giá thuê / ngày khi lưu phiếu.',
                ),
              ],
            ),
          ),
          FormFooter(
            saving: _saving,
            saveLabel: 'Lập phiếu trả',
            onCancel: () => context.go('/app/return-receipts'),
            onSave: _save,
          ),
        ],
      ),
    );
  }
}

final _returnReceiptProvider = FutureProvider.autoDispose.family<Json, int>((
  ref,
  id,
) async {
  final r = await ref
      .watch(apiClientProvider)
      .get('/bff/api/return-receipts/$id');
  return Map<String, dynamic>.from(r as Map);
});

/// Chi tiết phiếu trả hàng — GET /bff/api/return-receipts/{id}.
class ReturnReceiptDetailPage extends ConsumerWidget {
  const ReturnReceiptDetailPage({super.key, required this.id});

  final int id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_returnReceiptProvider(id));
    return async.when(
      loading: () => const LoadingView(),
      error: (e, _) => StateView.error(
        e,
        onRetry: () => ref.invalidate(_returnReceiptProvider(id)),
      ),
      data: (d) {
        final details = [
          for (final x in (d['details'] as List? ?? const []))
            Map<String, dynamic>.from(x as Map),
        ];
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PageHeader(
              title: 'Phiếu trả ${d['receiptCode'] ?? ''}',
              crumbs: const [
                Crumb('Trang chủ', '/app/dashboard'),
                Crumb('Cho thuê'),
                Crumb('Phiếu trả hàng', '/app/return-receipts'),
                Crumb('Chi tiết'),
              ],
              actions: [PrintButton(docs: returnReceiptPrintDocs(id))],
            ),
            infoCard('Thông tin phiếu trả', [
              ('Mã phiếu trả', InfoGrid.text(context, '${d['receiptCode']}')),
              (
                'Phiếu thuê / mượn',
                InfoGrid.text(context, d['rentalReceiptCode'] as String?),
              ),
              (
                'Kho nhận',
                InfoGrid.text(context, d['warehouseName'] as String?),
              ),
              (
                'Ngày trả',
                InfoGrid.text(context, Fmt.dateTime(d['returnDate'])),
              ),
              (
                'Số ngày quá hạn',
                InfoGrid.text(context, '${d['daysOverdue'] ?? 0} ngày'),
              ),
              (
                'Người lập',
                InfoGrid.text(context, d['createdByName'] as String?),
              ),
              ('Ghi chú', InfoGrid.text(context, d['note'] as String?)),
            ]),
            const SizedBox(height: 16),
            SectionCard(
              title: 'Hàng trả lại',
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
                      docText(context, x['conditionNote']),
                    ],
                ],
              ),
            ),
            const SizedBox(height: 16),
            SectionCard(
              title: 'Phạt, bồi thường và hoàn cọc',
              child: TotalsPanel(
                rows: [
                  ('Phạt quá hạn', Fmt.money(d['penaltyAmount']), false),
                  ('Bồi thường', Fmt.money(d['compensationAmount']), false),
                  ('Cọc hoàn lại', Fmt.money(d['depositRefund']), true),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}
