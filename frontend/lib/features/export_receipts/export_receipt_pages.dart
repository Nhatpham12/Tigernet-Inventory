import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/role.dart';
import '../../core/auth/session.dart';
import '../../core/auth/session_controller.dart';
import '../../core/network/api_exception.dart';
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
import '../../shared/form/unsaved_changes.dart';
import '../../shared/form/receipt_line_editor.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/doc_widgets.dart';
import '../common/lookups.dart';
import 'payment_dialog.dart';

typedef _Json = Map<String, dynamic>;

/// Quyền thao tác (chỉ để ẩn/hiện, backend vẫn chặn), khớp quyền API phiếu xuất:
/// sửa/hủy = trang Tạo phiếu xuất; thu tiền = Tạo phiếu xuất hoặc Phiếu thu;
/// duyệt = Quản lý; hoàn tất xuất = Thủ kho, Quản lý (Quản trị luôn được).
({bool edit, bool pay, bool confirm, bool complete}) _rights(Session? s) {
  if (s == null) {
    return (edit: false, pay: false, confirm: false, complete: false);
  }
  final edit = canOpenPage('/app/export-receipts/new', s);
  final manager = s.role == AppRole.admin || s.role == AppRole.manager;
  return (
    edit: edit,
    pay: edit || canOpenPage(kPaymentsNewIn, s),
    confirm: manager,
    complete: manager || s.role == AppRole.warehouse,
  );
}

bool _payable(_Json r) =>
    r['status'] != 'CANCELLED' && (Fmt.toNum(r['remain']) ?? 0) > 0;

/// Hủy được khi chưa xuất kho; phiếu đã thu tiền phải hủy phiếu thu trước (backend báo lỗi 2034).
bool _cancellable(_Json r) =>
    r['status'] == 'PENDING' || r['status'] == 'CONFIRMED';

/// Duyệt / hoàn tất xuất / hủy: dùng chung cho danh sách và màn chi tiết.
class _ExportOps {
  const _ExportOps._();

  static Future<void> _put(
    BuildContext context,
    WidgetRef ref,
    String path,
    String done,
    VoidCallback onDone,
  ) async {
    try {
      await ref.read(apiClientProvider).put(path);
      if (context.mounted) Toast.success(context, done);
      onDone();
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  // giữ hàng cho phiếu: hàng này không bán cho phiếu khác được nữa
  static Future<void> confirm(
    BuildContext context,
    WidgetRef ref,
    _Json r,
    VoidCallback onDone,
  ) async {
    final ok = await confirmDialog(
      context,
      title: 'Duyệt phiếu ${r['receiptCode']}',
      message:
          'Hàng trên phiếu sẽ được giữ lại tại kho ${r['warehouseName'] ?? ''} '
          'cho tới khi Thủ kho hoàn tất xuất.',
      confirmLabel: 'Duyệt phiếu',
    );
    if (!ok || !context.mounted) return;
    await _put(
      context,
      ref,
      '/bff/api/export-receipts/${r['id']}/confirm',
      'Đã duyệt phiếu ${r['receiptCode']}.',
      onDone,
    );
  }

  // hàng giữ thành đã bán, trừ khỏi kho
  static Future<void> complete(
    BuildContext context,
    WidgetRef ref,
    _Json r,
    VoidCallback onDone,
  ) async {
    final ok = await confirmDialog(
      context,
      title: 'Hoàn tất xuất ${r['receiptCode']}',
      message:
          'Xác nhận hàng đã giao khỏi kho. Tồn kho sẽ bị trừ và phiếu không hủy được nữa.',
      confirmLabel: 'Hoàn tất xuất',
    );
    if (!ok || !context.mounted) return;
    await _put(
      context,
      ref,
      '/bff/api/export-receipts/${r['id']}/complete',
      'Đã xuất kho phiếu ${r['receiptCode']}.',
      onDone,
    );
  }

  // DELETE = hủy phiếu (giữ lại ở trạng thái CANCELLED; phiếu đã duyệt thì trả lại hàng giữ)
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
              path: '/bff/api/export-receipts/${r['id']}',
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

/// Figma 978:2335 "Xuất kho / Danh sách phiếu xuất" — GET /bff/export-receipts.
class ExportReceiptListPage extends StatefulWidget {
  const ExportReceiptListPage({super.key, this.initialKeyword});

  /// Từ khóa điền sẵn (vd. mở từ "Giao dịch gần nhất" trên Dashboard).
  final String? initialKeyword;

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
    initialKeyword: widget.initialKeyword,
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
    onRowTap: (r) => context.go('/app/export-receipts/${r['id']}'),
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
        cell: (r) => _ExportActions(row: r, onDone: _controller.reload),
      ),
    ],
  );
}

class _ExportActions extends ConsumerWidget {
  const _ExportActions({required this.row, required this.onDone});

  final _Json row;
  final VoidCallback onDone;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final rights = _rights(ref.watch(sessionProvider).value);
    final status = row['status'];
    final id = row['id'];
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _ExportPrintMenu(row: row),
        PopupMenuButton<String>(
          tooltip: 'Thao tác',
          icon: const Icon(Icons.more_horiz),
          itemBuilder: (_) => [
            const PopupMenuItem(value: 'view', child: Text('Xem chi tiết')),
            if (rights.edit && status == 'PENDING')
              const PopupMenuItem(value: 'edit', child: Text('Sửa phiếu')),
            if (rights.confirm && status == 'PENDING')
              const PopupMenuItem(value: 'confirm', child: Text('Duyệt phiếu')),
            if (rights.complete && status == 'CONFIRMED')
              const PopupMenuItem(
                value: 'complete',
                child: Text('Hoàn tất xuất'),
              ),
            if (rights.pay && _payable(row))
              const PopupMenuItem(
                value: 'pay',
                child: Text('Ghi nhận thanh toán'),
              ),
            if (rights.edit && _cancellable(row))
              const PopupMenuItem(value: 'cancel', child: Text('Hủy phiếu')),
          ],
          onSelected: (v) async {
            switch (v) {
              case 'view':
                context.go('/app/export-receipts/$id');
              case 'edit':
                context.go('/app/export-receipts/$id/edit');
              case 'confirm':
                await _ExportOps.confirm(context, ref, row, onDone);
              case 'complete':
                await _ExportOps.complete(context, ref, row, onDone);
              case 'pay':
                if (await showPaymentDialog(context, row)) onDone();
              default:
                await _ExportOps.cancel(context, ref, row, onDone);
            }
          },
        ),
      ],
    );
  }
}

/// Chi tiết phiếu xuất: thông tin, hàng hóa, tiền; nút Duyệt / Hoàn tất xuất / Sửa / Hủy theo trạng thái và quyền.
class ExportReceiptDetailPage extends ConsumerStatefulWidget {
  const ExportReceiptDetailPage({super.key, required this.id});

  final int id;

  @override
  ConsumerState<ExportReceiptDetailPage> createState() =>
      _ExportReceiptDetailPageState();
}

class _ExportReceiptDetailPageState
    extends ConsumerState<ExportReceiptDetailPage> {
  late Future<_Json> _data = _load();

  Future<_Json> _load() async => Map<String, dynamic>.from(
    await ref
            .read(apiClientProvider)
            .get('/bff/api/export-receipts/${widget.id}')
        as Map,
  );

  void _reload() => setState(() => _data = _load());

  static const _hints = {
    'PENDING':
        'Phiếu chờ Quản lý duyệt. Khi duyệt, hàng trên phiếu được giữ lại tại kho.',
    'CONFIRMED':
        'Đã duyệt, hàng đang được giữ. Thủ kho bấm "Hoàn tất xuất" khi giao hàng để trừ tồn.',
  };

  @override
  Widget build(BuildContext context) => FutureBuilder<_Json>(
    future: _data,
    builder: (context, snap) {
      if (snap.connectionState != ConnectionState.done) {
        return const LoadingView();
      }
      if (snap.hasError) return StateView.error(snap.error!, onRetry: _reload);
      return _detail(context, snap.data!);
    },
  );

  Widget _detail(BuildContext context, _Json r) {
    final c = context.colors;
    final rights = _rights(ref.watch(sessionProvider).value);
    final status = r['status'] as String?;
    final details = (r['details'] as List? ?? const []).cast<Map>();
    final total = Fmt.toNum(r['totalAmount']) ?? 0;
    final subtotal = details.fold<num>(
      0,
      (s, d) => s + (Fmt.toNum(d['lineTotal']) ?? 0),
    );
    final code = '${r['receiptCode']}';
    final hint = _hints[status];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: 'Phiếu xuất $code',
          crumbs: [
            const Crumb('Trang chủ', '/app/dashboard'),
            const Crumb('Danh sách phiếu xuất', '/app/export-receipts'),
            Crumb(code),
          ],
          actions: [
            _ExportPrintMenu(row: r),
            if (rights.edit && status == 'PENDING')
              OutlinedButton.icon(
                icon: const Icon(Icons.edit_outlined, size: 18),
                label: const Text('Sửa phiếu'),
                onPressed: () =>
                    context.go('/app/export-receipts/${r['id']}/edit'),
              ),
            if (rights.edit && _cancellable(r))
              OutlinedButton(
                style: OutlinedButton.styleFrom(foregroundColor: c.danger),
                onPressed: () => _ExportOps.cancel(context, ref, r, _reload),
                child: const Text('Hủy phiếu'),
              ),
            if (rights.pay && _payable(r))
              OutlinedButton.icon(
                icon: const Icon(Icons.payments_outlined, size: 18),
                label: const Text('Ghi nhận thanh toán'),
                onPressed: () async {
                  if (await showPaymentDialog(context, r)) _reload();
                },
              ),
            if (rights.confirm && status == 'PENDING')
              ElevatedButton.icon(
                icon: const Icon(Icons.check_circle_outline, size: 18),
                label: const Text('Duyệt phiếu'),
                onPressed: () => _ExportOps.confirm(context, ref, r, _reload),
              ),
            if (rights.complete && status == 'CONFIRMED')
              ElevatedButton.icon(
                icon: const Icon(Icons.local_shipping_outlined, size: 18),
                label: const Text('Hoàn tất xuất'),
                onPressed: () => _ExportOps.complete(context, ref, r, _reload),
              ),
          ],
        ),
        infoCard('Thông tin phiếu', [
          ('Mã phiếu', InfoGrid.text(context, code)),
          ('Trạng thái', Statuses.receipt(status)),
          ('Khách hàng', InfoGrid.text(context, r['customerName'])),
          ('Kho xuất', InfoGrid.text(context, r['warehouseName'])),
          ('Ngày xuất', InfoGrid.text(context, Fmt.dateTime(r['dateOut']))),
          ('Ngày giao', InfoGrid.text(context, Fmt.dateTime(r['deliveryDate']))),
          ('Người nhận', InfoGrid.text(context, r['receiverNameSnapshot'])),
          ('SĐT người nhận', InfoGrid.text(context, r['receiverPhoneSnapshot'])),
          ('Địa chỉ giao', InfoGrid.text(context, r['receiverAddressSnapshot'])),
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
        if (hint != null) ...[const SizedBox(height: 12), HintText(hint)],
        const SizedBox(height: 16),
        SectionCard(
          title: 'Hàng hóa (${details.length} dòng)',
          child: DocTable(
            headers: const [
              'Sản phẩm',
              'ĐVT',
              'SL',
              'Đơn giá bán',
              'Thành tiền',
              'Serial',
              'Bảo hành đến',
            ],
            flex: const [5, 1, 1, 2, 2, 2, 2],
            rightAligned: const {2, 3, 4},
            minWidth: 860,
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
                  docText(context, Fmt.money(d['priceOut'])),
                  docText(context, Fmt.money(d['lineTotal']), bold: true),
                  docText(context, d['serial'] ?? '—'),
                  docText(
                    context,
                    d['warrantyExpiry'] == null
                        ? '—'
                        : Fmt.date(d['warrantyExpiry']),
                  ),
                ],
            ],
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Thanh toán',
          trailing: Statuses.payment(r['remain'], r['paid']),
          child: TotalsPanel(
            rows: [
              ('Tạm tính', Fmt.money(subtotal), false),
              ('VAT', Fmt.money(total - subtotal), false),
              ('Tổng cộng', Fmt.money(total), true),
              ('Đặt cọc', Fmt.money(r['deposit']), false),
              ('Đã thanh toán', Fmt.money(r['paid']), false),
              ('Còn lại', Fmt.money(r['remain']), false),
            ],
          ),
        ),
      ],
    );
  }
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
/// [receiptId] != null: sửa phiếu chờ duyệt (PUT); kho xuất và số đã thu không đổi ở đây.
class ExportReceiptFormPage extends ConsumerStatefulWidget {
  const ExportReceiptFormPage({super.key, this.receiptId});

  final int? receiptId;

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
  late final Future<void> _loading;

  bool get _isEdit => widget.receiptId != null;

  @override
  void initState() {
    super.initState();
    _paid.addListener(() => setState(() {}));
    if (_isEdit) {
      _loading = _loadExisting();
    } else {
      _warehouseId = ref.read(warehouseScopeProvider);
      _loading = Future.value();
    }
  }

  // nạp phiếu cần sửa; dòng hàng lấy thông tin SP đầy đủ (có isSerialized) từ danh mục
  Future<void> _loadExisting() async {
    final r = Map<String, dynamic>.from(
      await ref
              .read(apiClientProvider)
              .get('/bff/api/export-receipts/${widget.receiptId}')
          as Map,
    );
    if (r['status'] != 'PENDING') {
      throw StateError('Phiếu ${r['receiptCode']} không còn sửa được.');
    }
    final products = await ref.read(productsLookupProvider.future);
    _customerId = (r['customerId'] as num?)?.toInt();
    _warehouseId = (r['warehouseId'] as num?)?.toInt();
    _warehouseName = '${r['warehouseName'] ?? ''}';
    _vatId = (r['vatId'] as num?)?.toInt();
    _dateOut = DateTime.tryParse('${r['dateOut']}') ?? _dateOut;
    _deliveryDate = DateTime.tryParse('${r['deliveryDate']}');
    _receiverName.text = '${r['receiverNameSnapshot'] ?? ''}';
    _receiverPhone.text = '${r['receiverPhoneSnapshot'] ?? ''}';
    _receiverAddress.text = '${r['receiverAddressSnapshot'] ?? ''}';
    final deposit = Fmt.toNum(r['deposit']) ?? 0;
    _deposit.text = deposit > 0 ? '${deposit.toInt()}' : '';
    _paid.text = '${(Fmt.toNum(r['paid']) ?? 0).toInt()}';
    _note.text = '${r['note'] ?? ''}';
    for (final d in (r['details'] as List? ?? const []).cast<Map>()) {
      final product = products.firstWhere(
        (p) => p['id'] == d['productId'],
        orElse: () => {
          'id': d['productId'],
          'productName': d['productName'],
          'unit': d['unit'],
          'isSerialized': d['serial'] != null,
        },
      );
      final line = ReceiptLine(
        product: product,
        price: Fmt.toNum(d['priceOut']),
        quantity: (d['quantity'] as num?)?.toInt() ?? 1,
      );
      line.serial.text = '${d['serial'] ?? ''}';
      line.warranty.text = d['warrantyMonths'] == null
          ? ''
          : '${d['warrantyMonths']}';
      _lines.add(line);
    }
  }

  String _warehouseName = '';

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
    // hàng serial: mỗi dòng 1 cái kèm serial đang có trong kho; hàng không serial: để trống serial
    final badLine = _lines.where(
      (l) => l.product['isSerialized'] == true
          ? l.serialValue == null || l.quantity != 1
          : l.serialValue != null,
    );
    if (badLine.isNotEmpty) {
      Toast.error(
        context,
        'Dòng "${badLine.first.product['productName'] ?? ''}": hàng theo dõi serial cần '
        'mỗi dòng SL 1 kèm serial; hàng không serial thì để trống serial.',
      );
      return;
    }
    setState(() => _saving = true);
    final body = {
      'customerId': _customerId,
      'receiverName': _text(_receiverName),
      'receiverPhone': _text(_receiverPhone),
      'receiverAddress': _text(_receiverAddress),
      'dateOut': Fmt.apiDateTime(_dateOut),
      'deliveryDate': Fmt.apiDateTime(_deliveryDate),
      'vatId': _vatId,
      'deposit': parseNumber(_deposit.text),
      // khi sửa, backend bỏ qua null nên gửi '' để xóa được ghi chú cũ
      'note': _isEdit ? _note.text.trim() : _text(_note),
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
    };
    try {
      final api = ref.read(apiClientProvider);
      final result = _isEdit
          ? await api.put(
              '/bff/api/export-receipts/${widget.receiptId}',
              data: body,
            )
          : await api.post(
              '/bff/api/export-receipts',
              data: {...body, 'warehouseId': _warehouseId, 'paid': paid},
            );
      if (!mounted) return;
      UnsavedChanges.clear();
      Toast.success(
        context,
        'Đã lưu phiếu ${(result as Map?)?['receiptCode'] ?? ''}.',
      );
      context.go(
        _isEdit
            ? '/app/export-receipts/${widget.receiptId}'
            : '/app/export-receipts',
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
    final session = ref.watch(sessionProvider).value;
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

    return FormPageLayout(
      formKey: _form,
      children: [
        PageHeader(
          title: _isEdit ? 'Sửa phiếu xuất hàng' : 'Tạo phiếu xuất hàng',
          crumbs: [
            const Crumb('Trang chủ', '/app/dashboard'),
            const Crumb('Xuất kho', '/app/export-receipts'),
            Crumb(_isEdit ? 'Sửa phiếu xuất' : 'Tạo phiếu xuất'),
          ],
        ),
        SectionCard(
          title: 'Thông tin phiếu',
          child: FormGrid(
            children: [
              customers.when(
                loading: () => const LinearProgressIndicator(),
                error: (e, _) => Text(userMessage(e)),
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
              if (_isEdit)
                AppTextField(
                  label: 'Kho xuất',
                  readOnly: true,
                  initialValue: _warehouseName,
                )
              else
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text(userMessage(e)),
                  data: (list) => AppDropdown<int>(
                    label: 'Kho xuất',
                    required: true,
                    hint: 'Chọn kho',
                    value: _warehouseId,
                    // nhân viên chỉ thấy kho được giao (backend vẫn chặn kho khác)
                    items: [
                      if (session != null && !session.canViewAllWarehouses)
                        for (final w in session.warehouses) (w.id, w.name)
                      else
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
                error: (e, _) => Text(userMessage(e)),
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
                  const SizedBox(height: 16),
                  // khi sửa, tiền thu thêm ghi qua "Ghi nhận thanh toán" (mỗi lần thu là 1 phiếu thu)
                  AppTextField(
                    label: 'Đã thanh toán (₫)',
                    controller: _paid,
                    readOnly: _isEdit,
                    keyboardType: TextInputType.number,
                    inputFormatters: digitsOnly,
                  ),
                  const SizedBox(height: 16),
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
      ],
      footer: FormFooter(
        saving: _saving,
        onCancel: () => guardedGo(
          context,
          _isEdit
              ? '/app/export-receipts/${widget.receiptId}'
              : '/app/export-receipts',
        ),
        onSave: () => _save(total),
      ),
    );
  }
}
