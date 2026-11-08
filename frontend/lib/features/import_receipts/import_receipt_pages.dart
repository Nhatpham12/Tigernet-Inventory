import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session.dart';
import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/router/menu.dart';
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
import '../products/product_form_page.dart';

typedef _Json = Map<String, dynamic>;

/// Phiếu còn sửa / gửi duyệt / hủy được (backend chặn CONFIRMED và CANCELLED).
bool _editable(Object? status) => status == 'DRAFT' || status == 'PENDING';

/// Phiếu chưa hủy và còn nợ nhà cung cấp thì lập được phiếu chi.
bool _payable(_Json r) =>
    r['status'] != 'CANCELLED' && (Fmt.toNum(r['remain']) ?? 0) > 0;

String _paymentPath(Object? id) =>
    '/app/payments/new?direction=OUT&refType=Import_Receipts&refId=$id';

/// Quyền thao tác theo menu (chỉ để ẩn/hiện, backend vẫn chặn quyền API).
({bool edit, bool pay}) _rights(Session? s) => (
  edit: s != null && canOpenPage('/app/import-receipts/new', s),
  pay: s != null && canOpenPage('/app/payments/new?direction=OUT', s),
);

/// Gửi duyệt / hủy phiếu: dùng chung cho danh sách và màn chi tiết.
class _ImportOps {
  const _ImportOps._();

  static Future<void> submit(
    BuildContext context,
    WidgetRef ref,
    _Json r,
    VoidCallback onDone,
  ) async {
    try {
      await ref
          .read(apiClientProvider)
          .put(
            '/bff/api/import-receipts/${r['id']}',
            data: {'status': 'PENDING'},
          );
      if (context.mounted) {
        Toast.success(context, 'Đã gửi duyệt phiếu ${r['receiptCode']}.');
      }
      onDone();
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  // DELETE = hủy phiếu (backend giữ phiếu ở trạng thái CANCELLED để tra cứu)
  static Future<void> cancel(
    BuildContext context,
    WidgetRef ref,
    _Json r,
    VoidCallback onDone,
  ) async {
    final values = await formDialog(
      context,
      title: 'Hủy phiếu ${r['receiptCode']}',
      fields: const [('reason', 'Lý do hủy', '', false)],
      confirmLabel: 'Hủy phiếu',
    );
    if (values == null) return;
    final reason = values['reason'] ?? '';
    try {
      await ref
          .read(apiClientProvider)
          .delete(
            Uri(
              path: '/bff/api/import-receipts/${r['id']}',
              queryParameters: reason.isEmpty ? null : {'reason': reason},
            ).toString(),
          );
      if (context.mounted) {
        Toast.success(context, 'Đã hủy phiếu ${r['receiptCode']}.');
      }
      onDone();
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }
}

PrintButton _printButton(Object? id, {bool compact = false}) => PrintButton(
  compact: compact,
  docs: [
    PrintDoc('Phiếu nhập kho 01-VT', '/bff/api/import-receipts/$id/print'),
  ],
);

/// Danh sách phiếu nhập — GET /bff/import-receipts. Bấm dòng để mở chi tiết.
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
    onRowTap: (r) => context.go('/app/import-receipts/${r['id']}'),
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
      TableColumnDef.text(
        'Còn nợ',
        (r) => Fmt.money(r['remain']),
        minWidth: 110,
        align: Alignment.centerRight,
      ),
      TableColumnDef(
        label: 'Trạng thái',
        minWidth: 130,
        cell: (r) => Statuses.importReceipt(r['status'] as String?),
      ),
      TableColumnDef(
        label: 'Thao tác',
        minWidth: 130,
        align: Alignment.center,
        cell: (r) => _ImportActions(row: r, onDone: _controller.reload),
      ),
    ],
  );
}

class _ImportActions extends ConsumerWidget {
  const _ImportActions({required this.row, required this.onDone});

  final _Json row;
  final VoidCallback onDone;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = row['status'];
    final rights = _rights(ref.watch(sessionProvider).value);
    final id = row['id'];
    final items = <PopupMenuEntry<String>>[
      const PopupMenuItem(value: 'view', child: Text('Xem chi tiết')),
      if (rights.edit && _editable(status))
        const PopupMenuItem(value: 'edit', child: Text('Sửa phiếu')),
      if (rights.edit && status == 'DRAFT')
        const PopupMenuItem(value: 'submit', child: Text('Gửi duyệt')),
      if (rights.pay && _payable(row))
        const PopupMenuItem(value: 'pay', child: Text('Tạo phiếu chi')),
      if (rights.edit && _editable(status))
        const PopupMenuItem(value: 'cancel', child: Text('Hủy phiếu')),
    ];
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Phiếu nào cũng in được (kể cả đã hủy)
        _printButton(id, compact: true),
        PopupMenuButton<String>(
          tooltip: 'Thao tác',
          icon: const Icon(Icons.more_horiz),
          itemBuilder: (_) => items,
          onSelected: (v) => switch (v) {
            'view' => context.go('/app/import-receipts/$id'),
            'edit' => context.go('/app/import-receipts/$id/edit'),
            'submit' => _ImportOps.submit(context, ref, row, onDone),
            'pay' => context.go(_paymentPath(id)),
            _ => _ImportOps.cancel(context, ref, row, onDone),
          },
        ),
      ],
    );
  }
}

/// Chi tiết phiếu nhập: thông tin, hàng hóa, công nợ và lịch sử thanh toán (phiếu chi của phiếu này).
class ImportReceiptDetailPage extends ConsumerStatefulWidget {
  const ImportReceiptDetailPage({super.key, required this.id});

  final int id;

  @override
  ConsumerState<ImportReceiptDetailPage> createState() =>
      _ImportReceiptDetailPageState();
}

class _ImportReceiptDetailPageState
    extends ConsumerState<ImportReceiptDetailPage> {
  late Future<(_Json, List<_Json>)> _data = _load();

  Future<(_Json, List<_Json>)> _load() async {
    final api = ref.read(apiClientProvider);
    final results = await Future.wait([
      api.get('/bff/api/import-receipts/${widget.id}'),
      api.getList(
        '/bff/api/payments',
        query: {'referenceType': 'Import_Receipts', 'referenceId': widget.id},
      ),
    ]);
    final payments = (results[1] as List<_Json>)
      ..sort((a, b) => '${b['paidAt']}'.compareTo('${a['paidAt']}'));
    return (Map<String, dynamic>.from(results[0] as Map), payments);
  }

  void _reload() => setState(() => _data = _load());

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<(_Json, List<_Json>)>(
      future: _data,
      builder: (context, snap) {
        if (snap.connectionState != ConnectionState.done) {
          return const LoadingView();
        }
        if (snap.hasError) {
          return StateView.error(snap.error!, onRetry: _reload);
        }
        final (r, payments) = snap.data!;
        return _detail(context, r, payments);
      },
    );
  }

  Widget _detail(BuildContext context, _Json r, List<_Json> payments) {
    final c = context.colors;
    final rights = _rights(ref.watch(sessionProvider).value);
    final status = r['status'];
    final details = (r['details'] as List? ?? const []).cast<Map>();
    final total = Fmt.toNum(r['totalAmount']) ?? 0;
    final subtotal = details.fold<num>(
      0,
      (s, d) => s + (Fmt.toNum(d['lineTotal']) ?? 0),
    );
    final code = '${r['receiptCode']}';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: 'Phiếu nhập $code',
          crumbs: [
            const Crumb('Trang chủ', '/app/dashboard'),
            const Crumb('Danh sách phiếu nhập', '/app/import-receipts'),
            Crumb(code),
          ],
          actions: [
            _printButton(r['id']),
            if (rights.edit && _editable(status))
              OutlinedButton.icon(
                icon: const Icon(Icons.edit_outlined, size: 18),
                label: const Text('Sửa phiếu'),
                onPressed: () =>
                    context.go('/app/import-receipts/${r['id']}/edit'),
              ),
            if (rights.edit && status == 'DRAFT')
              OutlinedButton(
                onPressed: () => _ImportOps.submit(context, ref, r, _reload),
                child: const Text('Gửi duyệt'),
              ),
            if (rights.edit && _editable(status))
              OutlinedButton(
                style: OutlinedButton.styleFrom(foregroundColor: c.danger),
                onPressed: () => _ImportOps.cancel(context, ref, r, _reload),
                child: const Text('Hủy phiếu'),
              ),
            if (rights.pay && _payable(r))
              ElevatedButton.icon(
                icon: const Icon(Icons.payments_outlined, size: 18),
                label: const Text('Tạo phiếu chi'),
                onPressed: () => context.go(_paymentPath(r['id'])),
              ),
          ],
        ),
        infoCard('Thông tin phiếu', [
          ('Mã phiếu', InfoGrid.text(context, code)),
          ('Trạng thái', Statuses.importReceipt(status as String?)),
          ('Nhà cung cấp', InfoGrid.text(context, r['sourcePartyName'])),
          ('Kho nhập', InfoGrid.text(context, r['warehouseName'])),
          ('Ngày nhập', InfoGrid.text(context, Fmt.dateTime(r['dateIn']))),
          ('Người lập', InfoGrid.text(context, r['createdByName'])),
          ('VAT', InfoGrid.text(context, r['vat'] ?? 'Không VAT')),
          ('Ghi chú', InfoGrid.text(context, r['note'])),
          if (status == 'CANCELLED') ...[
            ('Người hủy', InfoGrid.text(context, r['cancelledByName'])),
            (
              'Thời gian hủy',
              InfoGrid.text(context, Fmt.dateTime(r['cancelledAt'])),
            ),
            ('Lý do hủy', InfoGrid.text(context, r['cancelReason'])),
          ],
        ]),
        if (status == 'PENDING') ...[
          const SizedBox(height: 12),
          const HintText(
            'Phiếu đang chờ duyệt. Bước xác nhận nhập kho (cộng tồn) chưa có trên hệ thống.',
          ),
        ],
        const SizedBox(height: 16),
        SectionCard(
          title: 'Hàng hóa (${details.length} dòng)',
          child: DocTable(
            headers: const [
              'Sản phẩm',
              'ĐVT',
              'SL',
              'Đơn giá nhập',
              'Thành tiền',
              'Serial',
            ],
            flex: const [5, 1, 1, 2, 2, 2],
            rightAligned: const {2, 3, 4},
            minWidth: 760,
            rows: [
              for (final d in details)
                [
                  docText(
                    context,
                    '${Fmt.productCode(d['productId'])} · ${d['productName']}',
                    bold: true,
                  ),
                  docText(context, d['unit']),
                  docText(context, Fmt.number(d['quantity'])),
                  docText(context, Fmt.money(d['priceIn'])),
                  docText(context, Fmt.money(d['lineTotal']), bold: true),
                  docText(context, d['serial'] ?? '—'),
                ],
            ],
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Công nợ nhà cung cấp',
          trailing: Statuses.payment(r['remain'], r['paid']),
          child: TotalsPanel(
            rows: [
              ('Tạm tính', Fmt.money(subtotal), false),
              ('VAT', Fmt.money(total - subtotal), false),
              ('Tổng cộng', Fmt.money(total), true),
              ('Đã thanh toán', Fmt.money(r['paid']), false),
              ('Còn nợ', Fmt.money(r['remain']), false),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Lịch sử thanh toán (${payments.length})',
          child: payments.isEmpty
              ? Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Text(
                    'Chưa có phiếu chi nào cho phiếu nhập này.',
                    style: TextStyle(fontSize: 13, color: c.muted),
                  ),
                )
              : DocTable(
                  headers: const [
                    'Mã phiếu chi',
                    'Ngày chi',
                    'Số tiền',
                    'Hình thức',
                    'Người lập',
                    'Trạng thái',
                    '',
                  ],
                  flex: const [2, 2, 2, 2, 2, 2, 1],
                  rightAligned: const {2},
                  minWidth: 820,
                  rows: [
                    for (final p in payments)
                      [
                        docText(context, p['paymentCode'], bold: true),
                        docText(context, Fmt.dateTime(p['paidAt'])),
                        Text(
                          Fmt.money(p['amount']),
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w700,
                            color: p['status'] == 'CANCELLED'
                                ? c.muted
                                : c.danger,
                            decoration: p['status'] == 'CANCELLED'
                                ? TextDecoration.lineThrough
                                : null,
                          ),
                        ),
                        docText(
                          context,
                          p['method'] == 'TRANSFER'
                              ? 'Chuyển khoản'
                              : 'Tiền mặt',
                        ),
                        docText(context, p['recordedByName']),
                        p['status'] == 'CANCELLED'
                            ? const StatusChip(
                                label: 'Đã hủy',
                                tone: ChipTone.danger,
                              )
                            : const StatusChip(
                                label: 'Hiệu lực',
                                tone: ChipTone.success,
                              ),
                        PrintButton(
                          compact: true,
                          docs: [
                            PrintDoc(
                              'Phiếu chi',
                              '/bff/api/payments/${p['id']}/print',
                            ),
                          ],
                        ),
                      ],
                  ],
                ),
        ),
      ],
    );
  }
}

/// Figma 978:1453 "Nhập hàng / Tạo phiếu nhập hàng" — POST /bff/api/import-receipts.
/// [receiptId] != null: sửa phiếu nháp / chờ duyệt (PUT); kho nhập không đổi được sau khi tạo.
class ImportReceiptFormPage extends ConsumerStatefulWidget {
  const ImportReceiptFormPage({super.key, this.receiptId});

  final int? receiptId;

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
  _Json? _existing;
  late final Future<void> _loading = _isEdit ? _load() : Future.value();

  bool get _isEdit => widget.receiptId != null;

  @override
  void initState() {
    super.initState();
    if (!_isEdit) _warehouseId = ref.read(warehouseScopeProvider);
  }

  /// Nạp phiếu cần sửa vào form; phiếu đã nhập kho / đã hủy thì báo lỗi.
  Future<void> _load() async {
    final r = Map<String, dynamic>.from(
      await ref
              .read(apiClientProvider)
              .get('/bff/api/import-receipts/${widget.receiptId}')
          as Map,
    );
    if (!_editable(r['status'])) {
      throw StateError('Phiếu ${r['receiptCode']} không còn sửa được.');
    }
    _existing = r;
    _supplierPartyId = (r['sourcePartyId'] as num?)?.toInt();
    _warehouseId = (r['warehouseId'] as num?)?.toInt();
    _vatId = (r['vatId'] as num?)?.toInt();
    _dateIn = DateTime.tryParse('${r['dateIn']}') ?? _dateIn;
    _note.text = '${r['note'] ?? ''}';
    for (final d in (r['details'] as List? ?? const []).cast<Map>()) {
      final line = ReceiptLine(
        product: {
          'id': d['productId'],
          'productName': d['productName'],
          'unit': d['unit'],
          'isSerialized': d['serial'] != null,
        },
        price: Fmt.toNum(d['priceIn']),
        quantity: (d['quantity'] as num?)?.toInt() ?? 1,
      );
      line.serial.text = '${d['serial'] ?? ''}';
      _lines.add(line);
    }
  }

  @override
  void dispose() {
    _note.dispose();
    for (final l in _lines) {
      l.dispose();
    }
    super.dispose();
  }

  String get _backPath => _isEdit
      ? '/app/import-receipts/${widget.receiptId}'
      : '/app/import-receipts';

  /// Mở form "Thêm sản phẩm" trong hộp thoại; trả sản phẩm vừa tạo để thêm vào phiếu.
  Future<_Json?> _createProduct() => showDialog<_Json>(
    context: context,
    builder: (_) => Dialog(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 820, maxHeight: 720),
        child: const SingleChildScrollView(
          padding: EdgeInsets.all(20),
          child: ProductFormPage(inDialog: true),
        ),
      ),
    ),
  );

  Future<void> _save(String status) async {
    if (!_form.currentState!.validate()) return;
    if (_lines.isEmpty || _lines.any((l) => l.quantity <= 0)) {
      Toast.error(context, 'Phiếu cần ít nhất một dòng hàng có số lượng > 0.');
      return;
    }
    setState(() => _saving = true);
    final note = _note.text.trim();
    final body = {
      'sourcePartyId': _supplierPartyId,
      'dateIn': Fmt.apiDateTime(_dateIn),
      'vatId': _vatId,
      'status': status,
      // khi sửa, backend bỏ qua null nên gửi '' để xóa được ghi chú cũ
      'note': note.isEmpty && !_isEdit ? null : note,
      'details': [
        for (final l in _lines)
          {
            'productId': l.productId,
            'quantity': l.quantity,
            'priceIn': l.unitPrice,
            'serial': l.serialValue,
          },
      ],
    };
    try {
      final api = ref.read(apiClientProvider);
      final result = _isEdit
          ? await api.put(
              '/bff/api/import-receipts/${widget.receiptId}',
              data: body,
            )
          : await api.post(
              '/bff/api/import-receipts',
              data: {...body, 'warehouseId': _warehouseId},
            );
      if (!mounted) return;
      final saved = result is Map ? result : const {};
      Toast.success(context, 'Đã lưu phiếu ${saved['receiptCode'] ?? ''}.');
      final id = saved['id'] ?? widget.receiptId;
      context.go(
        id == null ? '/app/import-receipts' : '/app/import-receipts/$id',
      );
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<void>(
    future: _loading,
    builder: (context, snap) {
      if (snap.connectionState != ConnectionState.done) {
        return const LoadingView();
      }
      if (snap.hasError) return StateView.error(snap.error!);
      return _formBody(context);
    },
  );

  Widget _formBody(BuildContext context) {
    final suppliers = ref.watch(suppliersLookupProvider);
    final warehouses = ref.watch(warehousesLookupProvider);
    final vats = ref.watch(vatOptionsLookupProvider);
    final products = ref.watch(productsLookupProvider);
    final session = ref.watch(sessionProvider).value;
    final title = _isEdit
        ? 'Sửa phiếu nhập ${_existing?['receiptCode'] ?? ''}'
        : 'Tạo phiếu nhập hàng';

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
          PageHeader(
            title: title,
            crumbs: [
              const Crumb('Trang chủ', '/app/dashboard'),
              const Crumb('Nhập hàng', '/app/import-receipts'),
              Crumb(_isEdit ? 'Sửa phiếu nhập' : 'Tạo phiếu nhập'),
            ],
          ),
          SectionCard(
            title: 'Thông tin phiếu',
            child: FormGrid(
              children: [
                AppTextField(
                  label: 'Mã phiếu',
                  readOnly: true,
                  controller: TextEditingController(
                    text: _existing?['receiptCode'] ?? 'Tự sinh khi lưu',
                  ),
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
                if (_isEdit)
                  AppTextField(
                    label: 'Kho nhập',
                    readOnly: true,
                    controller: TextEditingController(
                      text: '${_existing?['warehouseName'] ?? ''}',
                    ),
                  )
                else
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
                    text:
                        _existing?['createdByName'] ??
                        session?.displayName ??
                        '',
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
                onCreateProduct: _createProduct,
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
            onCancel: () => context.go(_backPath),
            onSaveDraft: () => _save('DRAFT'),
            onSave: () => _save('PENDING'),
            saveLabel: 'Lưu & gửi duyệt',
          ),
        ],
      ),
    );
  }
}
