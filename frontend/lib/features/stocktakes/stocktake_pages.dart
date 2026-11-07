import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
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
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/doc_widgets.dart';
import '../common/lookups.dart';

List<PrintDoc> stocktakePrintDocs(Object? id, {required bool completed}) => [
  PrintDoc('Biên bản kiểm kê 05-VT', '/bff/api/stocktakes/$id/print'),
  if (completed)
    PrintDoc(
      'Biên bản điều chỉnh tồn kho',
      '/bff/api/stocktakes/$id/print',
      query: {'template': 'adjustment'},
    ),
];

StatusChip _stocktakeChip(Object? status) => status == 'COMPLETED'
    ? const StatusChip(
        label: 'Đã chốt',
        tone: ChipTone.success,
        icon: Icons.check,
      )
    : const StatusChip(label: 'Đang kiểm', tone: ChipTone.warning);

/// Figma 3005:11028 "Kiểm kê / Danh sách đợt kiểm kê" — GET /bff/stocktakes.
class StocktakeListPage extends StatelessWidget {
  const StocktakeListPage({super.key});

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách đợt kiểm kê',
    subtitle: 'Kiểm đếm, đối chiếu chênh lệch và chốt điều chỉnh tồn',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Kho'),
      Crumb('Kiểm kê'),
    ],
    actions: [
      ElevatedButton(
        onPressed: () => context.go('/app/stocktakes/new'),
        child: const Text('Tạo đợt kiểm kê'),
      ),
    ],
    endpoint: '/bff/stocktakes',
    defaultSort: 'stocktakeDate,desc',
    searchHint: 'Tìm mã đợt, kho...',
    itemLabel: 'đợt',
    onRowTap: (r) => context.go('/app/stocktakes/${r['id']}'),
    filters: const [
      ListFilter(
        key: 'status',
        label: 'Trạng thái',
        options: [
          (null, 'Tất cả'),
          ('DRAFT', 'Đang kiểm'),
          ('COMPLETED', 'Đã chốt'),
        ],
      ),
    ],
    columns: [
      TableColumnDef.text(
        'Mã đợt',
        (r) => '${r['stocktakeCode']}',
        minWidth: 110,
        sortKey: 'stocktakeCode',
        bold: true,
      ),
      TableColumnDef.text(
        'Kho',
        (r) => '${r['warehouseName'] ?? r['warehouseCode']}',
        flex: 2,
        minWidth: 130,
      ),
      TableColumnDef.text(
        'Ngày kiểm',
        (r) => Fmt.date(r['stocktakeDate']),
        minWidth: 100,
        sortKey: 'stocktakeDate',
      ),
      TableColumnDef.text(
        'Số SP',
        (r) => '${(r['details'] as List?)?.length ?? 0}',
        flex: 1,
        minWidth: 70,
        align: Alignment.centerRight,
      ),
      TableColumnDef.text(
        'Người lập',
        (r) => '${r['createdByName'] ?? '—'}',
        minWidth: 130,
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 120,
        cell: (r) => _stocktakeChip(r['status']),
      ),
      TableColumnDef(
        label: 'In',
        flex: 1,
        minWidth: 70,
        align: Alignment.center,
        cell: (r) => PrintButton(
          compact: true,
          docs: stocktakePrintDocs(
            r['id'],
            completed: r['status'] == 'COMPLETED',
          ),
        ),
      ),
    ],
  );
}

/// Tạo đợt kiểm kê — POST /bff/api/stocktakes (StocktakeCreationRequest).
/// Backend tự lấy số sổ sách (systemQty) từ tồn kho nên form chỉ nhập số đếm thực tế.
class StocktakeFormPage extends ConsumerStatefulWidget {
  const StocktakeFormPage({super.key});

  @override
  ConsumerState<StocktakeFormPage> createState() => _StocktakeFormPageState();
}

class _StocktakeFormPageState extends ConsumerState<StocktakeFormPage> {
  final _form = GlobalKey<FormState>();
  final List<ReceiptLine> _lines = [];
  int? _warehouseId;
  DateTime? _date = DateTime.now();
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _warehouseId = ref.read(warehouseScopeProvider);
  }

  @override
  void dispose() {
    for (final l in _lines) {
      l.dispose();
    }
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    if (_lines.isEmpty) {
      Toast.error(context, 'Đợt kiểm kê cần ít nhất một sản phẩm.');
      return;
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/stocktakes',
            data: {
              'warehouseId': _warehouseId,
              'stocktakeDate': Fmt.apiDateTime(_date),
              'details': [
                for (final l in _lines)
                  {'productId': l.productId, 'actualQty': l.quantity},
              ],
            },
          );
      if (!mounted) return;
      final m = result as Map?;
      Toast.success(
        context,
        'Đã tạo đợt kiểm kê ${m?['stocktakeCode'] ?? ''}.',
      );
      context.go(
        m?['id'] == null ? '/app/stocktakes' : '/app/stocktakes/${m!['id']}',
      );
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final warehouses = ref.watch(warehousesLookupProvider);
    final products = ref.watch(productsLookupProvider);
    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: 'Tạo đợt kiểm kê',
            crumbs: const [
              Crumb('Trang chủ', '/app/dashboard'),
              Crumb('Kho'),
              Crumb('Kiểm kê', '/app/stocktakes'),
              Crumb('Tạo đợt kiểm kê'),
            ],
          ),
          SectionCard(
            title: 'Thông tin đợt kiểm kê',
            child: FormGrid(
              maxColumns: 2,
              children: [
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int>(
                    label: 'Kho kiểm kê',
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
                  label: 'Ngày giờ kiểm kê',
                  required: true,
                  value: _date,
                  onChanged: (d) => setState(() => _date = d),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          SectionCard(
            title: 'Sản phẩm kiểm đếm',
            child: products.when(
              loading: () => const LoadingView(),
              error: (e, _) => StateView.error(e),
              data: (list) => Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ReceiptLineEditor(
                    lines: _lines,
                    products: list,
                    showPrice: false,
                    showSerial: false,
                    defaultPrice: (_) => null,
                    onChanged: () => setState(() {}),
                  ),
                  const HintText(
                    'Cột "SL" là số đếm thực tế. Số sổ sách do hệ thống tự lấy theo tồn kho khi lưu, chênh lệch xem ở màn chi tiết.',
                  ),
                ],
              ),
            ),
          ),
          FormFooter(
            saving: _saving,
            saveLabel: 'Tạo đợt',
            onCancel: () => context.go('/app/stocktakes'),
            onSave: _save,
          ),
        ],
      ),
    );
  }
}

final _stocktakeProvider = FutureProvider.autoDispose.family<Json, int>((
  ref,
  id,
) async {
  final r = await ref.watch(apiClientProvider).get('/bff/api/stocktakes/$id');
  return Map<String, dynamic>.from(r as Map);
});

final _adjustmentsProvider = FutureProvider.autoDispose.family<List<Json>, int>(
  (ref, id) => ref
      .watch(apiClientProvider)
      .getList('/bff/api/stock-adjustments', query: {'stocktakeId': id}),
);

/// Chi tiết đợt kiểm kê: xem chênh lệch, sửa số đếm khi DRAFT, chốt phiếu, in biên bản.
class StocktakeDetailPage extends ConsumerStatefulWidget {
  const StocktakeDetailPage({super.key, required this.id});

  final int id;

  @override
  ConsumerState<StocktakeDetailPage> createState() =>
      _StocktakeDetailPageState();
}

class _StocktakeDetailPageState extends ConsumerState<StocktakeDetailPage> {
  final Map<int, TextEditingController> _qty = {};
  Object? _loadedFor;
  bool _busy = false;

  @override
  void dispose() {
    for (final c in _qty.values) {
      c.dispose();
    }
    super.dispose();
  }

  void _sync(Json data) {
    if (identical(_loadedFor, data)) return;
    _loadedFor = data;
    for (final c in _qty.values) {
      c.dispose();
    }
    _qty.clear();
    for (final d in (data['details'] as List? ?? const [])) {
      final m = d as Map;
      _qty[(m['productId'] as num).toInt()] = TextEditingController(
        text: '${m['actualQty'] ?? 0}',
      );
    }
  }

  List<Json> _details(Json data) => [
    for (final d in (data['details'] as List? ?? const []))
      Map<String, dynamic>.from(d as Map),
  ];

  int _actual(Json d) {
    final c = _qty[(d['productId'] as num).toInt()];
    return parseNumber(c?.text)?.toInt() ?? 0;
  }

  bool _dirty(Json data) => _details(
    data,
  ).any((d) => _actual(d) != (Fmt.toNum(d['actualQty'])?.toInt() ?? 0));

  Future<void> _run(Future<void> Function() job) async {
    setState(() => _busy = true);
    try {
      await job();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _refresh() {
    ref.invalidate(_stocktakeProvider(widget.id));
    ref.invalidate(_adjustmentsProvider(widget.id));
  }

  Future<void> _saveItems(Json data) => _run(() async {
    await ref
        .read(apiClientProvider)
        .put(
          '/bff/api/stocktakes/${widget.id}/items',
          data: {
            'stocktakeDate': data['stocktakeDate'],
            'details': [
              for (final d in _details(data))
                {'productId': d['productId'], 'actualQty': _actual(d)},
            ],
          },
        );
    if (mounted) Toast.success(context, 'Đã lưu số đếm.');
    _refresh();
  });

  Future<void> _complete(Json data) async {
    // chênh lệch tính theo số đang nhập; lưu trước nếu có sửa chưa lưu
    final diffs = [
      for (final d in _details(data))
        if (_actual(d) != (Fmt.toNum(d['systemQty'])?.toInt() ?? 0)) d,
    ];
    final reasons = await showDialog<Map<String, String>>(
      context: context,
      builder: (_) => _CompleteDialog(diffs: diffs, actual: _actual),
    );
    if (reasons == null) return;
    await _run(() async {
      final api = ref.read(apiClientProvider);
      if (_dirty(data)) {
        await api.put(
          '/bff/api/stocktakes/${widget.id}/items',
          data: {
            'stocktakeDate': data['stocktakeDate'],
            'details': [
              for (final d in _details(data))
                {'productId': d['productId'], 'actualQty': _actual(d)},
            ],
          },
        );
      }
      // Hợp đồng: PUT /stocktakes/{id}/complete {reasons:{productId: lý do}} → sinh Stock_Adjustments.
      // Lưu ý: điều chỉnh CHƯA được áp vào tồn kho (chờ StockService phía backend).
      await api.put(
        '/bff/api/stocktakes/${widget.id}/complete',
        data: {'reasons': reasons},
      );
      if (mounted) Toast.success(context, 'Đã chốt phiếu kiểm kê.');
      _refresh();
    });
  }

  Future<void> _delete() async {
    final ok = await confirmDialog(
      context,
      title: 'Xóa đợt kiểm kê',
      message: 'Đợt kiểm kê nháp sẽ bị xóa. Tiếp tục?',
      confirmLabel: 'Xóa',
      danger: true,
    );
    if (!ok) return;
    await _run(() async {
      await ref
          .read(apiClientProvider)
          .delete('/bff/api/stocktakes/${widget.id}');
      if (mounted) {
        Toast.success(context, 'Đã xóa đợt kiểm kê.');
        context.go('/app/stocktakes');
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(_stocktakeProvider(widget.id));
    return async.when(
      loading: () => const LoadingView(),
      error: (e, _) => StateView.error(
        e,
        onRetry: () => ref.invalidate(_stocktakeProvider(widget.id)),
      ),
      data: (data) {
        _sync(data);
        return _body(context, data);
      },
    );
  }

  Widget _body(BuildContext context, Json data) {
    final c = context.colors;
    final draft = data['status'] != 'COMPLETED';
    final details = _details(data);
    final dirty = draft && _dirty(data);
    final diffCount = details
        .where((d) => _actual(d) != (Fmt.toNum(d['systemQty'])?.toInt() ?? 0))
        .length;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: 'Đợt kiểm kê ${data['stocktakeCode'] ?? ''}',
          crumbs: const [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Kho'),
            Crumb('Kiểm kê', '/app/stocktakes'),
            Crumb('Chi tiết'),
          ],
          actions: [
            PrintButton(docs: stocktakePrintDocs(widget.id, completed: !draft)),
            if (draft)
              OutlinedButton(
                onPressed: _busy ? null : _delete,
                child: const Text('Xóa nháp'),
              ),
            if (draft)
              ElevatedButton(
                onPressed: _busy ? null : () => _complete(data),
                child: const Text('Chốt phiếu'),
              ),
          ],
        ),
        infoCard('Thông tin đợt kiểm kê', [
          ('Mã đợt', InfoGrid.text(context, '${data['stocktakeCode']}')),
          (
            'Kho',
            InfoGrid.text(
              context,
              '${data['warehouseName'] ?? data['warehouseCode'] ?? ''}',
            ),
          ),
          (
            'Ngày giờ kiểm',
            InfoGrid.text(context, Fmt.dateTime(data['stocktakeDate'])),
          ),
          (
            'Người lập',
            InfoGrid.text(context, data['createdByName'] as String?),
          ),
          ('Trạng thái', _stocktakeChip(data['status'])),
          (
            'Số dòng chênh lệch',
            InfoGrid.text(context, '$diffCount / ${details.length}'),
          ),
        ]),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Kết quả kiểm đếm',
          trailing: dirty
              ? ElevatedButton(
                  onPressed: _busy ? null : () => _saveItems(data),
                  child: const Text('Lưu số đếm'),
                )
              : null,
          child: DocTable(
            headers: const [
              'Sản phẩm',
              'ĐVT',
              'Sổ sách',
              'Thực tế',
              'Chênh lệch',
            ],
            flex: const [5, 1, 2, 2, 2],
            rightAligned: const {2, 3, 4},
            rows: [
              for (final d in details)
                () {
                  final sys = Fmt.toNum(d['systemQty'])?.toInt() ?? 0;
                  final actual = _actual(d);
                  final diff = actual - sys;
                  final id = (d['productId'] as num).toInt();
                  return <Widget>[
                    docText(
                      context,
                      '${Fmt.productCode(id)} · ${d['productName']}',
                      bold: true,
                    ),
                    docText(context, d['unit']),
                    docText(context, Fmt.number(sys)),
                    draft
                        ? SizedBox(
                            width: 90,
                            child: TextField(
                              controller: _qty[id],
                              textAlign: TextAlign.right,
                              keyboardType: TextInputType.number,
                              inputFormatters: digitsOnly,
                              style: const TextStyle(fontSize: 13),
                              onChanged: (_) => setState(() {}),
                            ),
                          )
                        : docText(context, Fmt.number(actual)),
                    Text(
                      diff > 0 ? '+$diff' : '$diff',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: diff == 0
                            ? c.muted
                            : (diff > 0 ? c.success : c.danger),
                      ),
                    ),
                  ];
                }(),
            ],
          ),
        ),
        if (!draft) ...[
          const SizedBox(height: 16),
          _AdjustmentsCard(id: widget.id),
        ],
      ],
    );
  }
}

class _AdjustmentsCard extends ConsumerWidget {
  const _AdjustmentsCard({required this.id});

  final int id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(_adjustmentsProvider(id));
    final c = context.colors;
    return SectionCard(
      title: 'Điều chỉnh tồn kho',
      trailing: PrintButton(
        docs: [
          PrintDoc(
            'Biên bản điều chỉnh tồn kho',
            '/bff/api/stocktakes/$id/print',
            query: const {'template': 'adjustment'},
          ),
        ],
      ),
      child: async.when(
        loading: () => const LoadingView(),
        error: (e, _) => StateView.error(e),
        data: (list) => list.isEmpty
            ? const Text('Không có dòng chênh lệch nào cần điều chỉnh.')
            : DocTable(
                headers: const ['Sản phẩm', 'Điều chỉnh', 'Lý do', 'Thời gian'],
                flex: const [4, 2, 5, 3],
                rightAligned: const {1},
                rows: [
                  for (final a in list)
                    [
                      docText(context, a['productName'], bold: true),
                      Text(
                        '${(Fmt.toNum(a['adjustQty']) ?? 0) > 0 ? '+' : ''}${a['adjustQty']}',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: (Fmt.toNum(a['adjustQty']) ?? 0) >= 0
                              ? c.success
                              : c.danger,
                        ),
                      ),
                      docText(context, a['reason']),
                      docText(context, Fmt.dateTime(a['createdAt'])),
                    ],
                ],
              ),
      ),
    );
  }
}

/// Dialog chốt phiếu: nhập lý do cho từng dòng chênh lệch.
class _CompleteDialog extends StatefulWidget {
  const _CompleteDialog({required this.diffs, required this.actual});

  final List<Json> diffs;
  final int Function(Json) actual;

  @override
  State<_CompleteDialog> createState() => _CompleteDialogState();
}

class _CompleteDialogState extends State<_CompleteDialog> {
  final _form = GlobalKey<FormState>();
  late final Map<int, TextEditingController> _reasons = {
    for (final d in widget.diffs)
      (d['productId'] as num).toInt(): TextEditingController(),
  };

  @override
  void dispose() {
    for (final c in _reasons.values) {
      c.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return AlertDialog(
      title: const Text('Chốt phiếu kiểm kê'),
      content: SizedBox(
        width: 560,
        child: SingleChildScrollView(
          child: Form(
            key: _form,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.diffs.isEmpty
                      ? 'Không có chênh lệch. Chốt phiếu sẽ khóa số liệu kiểm kê.'
                      : 'Nhập lý do cho ${widget.diffs.length} dòng chênh lệch. Sau khi chốt không sửa được số đếm.',
                  style: TextStyle(color: c.muted),
                ),
                const SizedBox(height: 12),
                for (final d in widget.diffs) ...[
                  Builder(
                    builder: (_) {
                      final sys = Fmt.toNum(d['systemQty'])?.toInt() ?? 0;
                      final diff = widget.actual(d) - sys;
                      return Text(
                        '${d['productName']}  (sổ sách $sys, thực tế ${widget.actual(d)}, chênh ${diff > 0 ? '+' : ''}$diff)',
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      );
                    },
                  ),
                  const SizedBox(height: 6),
                  TextFormField(
                    controller: _reasons[(d['productId'] as num).toInt()],
                    decoration: const InputDecoration(
                      hintText:
                          'Lý do chênh lệch (mất, hư hỏng, nhập thiếu...)',
                    ),
                    validator: (v) => (v == null || v.trim().isEmpty)
                        ? 'Vui lòng nhập lý do'
                        : null,
                  ),
                  const SizedBox(height: 14),
                ],
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: () {
            if (!_form.currentState!.validate()) return;
            Navigator.pop(context, {
              for (final e in _reasons.entries) '${e.key}': e.value.text.trim(),
            });
          },
          child: const Text('Chốt phiếu'),
        ),
      ],
    );
  }
}
