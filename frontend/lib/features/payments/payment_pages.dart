import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/role.dart';
import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/form/form_footer.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

/// Phiếu thu / chi: IN = phiếu thu (từ khách hàng), OUT = phiếu chi (trả nhà cung cấp).
String paymentTitle(Object? direction) =>
    direction == 'OUT' ? 'Phiếu chi' : 'Phiếu thu';

String _methodLabel(Object? m) => m == 'TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt';

/// Danh sách phiếu thu/chi — GET /bff/payments. Bán hàng chỉ xem phiếu thu.
class PaymentListPage extends ConsumerStatefulWidget {
  const PaymentListPage({super.key, this.initialDirection});

  final String? initialDirection;

  @override
  ConsumerState<PaymentListPage> createState() => _PaymentListPageState();
}

class _PaymentListPageState extends ConsumerState<PaymentListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _cancel(Map<String, dynamic> r) async {
    final reason = await formDialog(
      context,
      title:
          'Hủy ${paymentTitle(r['direction']).toLowerCase()} ${r['paymentCode']}',
      fields: const [('reason', 'Lý do hủy', '', false)],
      confirmLabel: 'Hủy phiếu',
    );
    if (reason == null) return;
    try {
      final text = reason['reason']?.trim() ?? '';
      await ref
          .read(apiClientProvider)
          .put(
            '/bff/api/payments/${r['id']}/cancel',
            data: {'reason': text.isEmpty ? null : text},
          );
      if (mounted) Toast.success(context, 'Đã hủy phiếu ${r['paymentCode']}.');
      _controller.reload();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final role = ref.watch(sessionProvider).value?.role;
    final onlyIn = role == AppRole.sales;
    final canWrite = !onlyIn;
    final c = context.colors;
    return PagedListView(
      key: ValueKey(widget.initialDirection),
      title: onlyIn ? 'Phiếu thu' : 'Phiếu thu / chi',
      subtitle: 'Thu tiền khách hàng, chi tiền nhà cung cấp',
      crumbs: const [
        Crumb('Trang chủ', '/app/dashboard'),
        Crumb('Thu chi'),
        Crumb('Phiếu thu / chi'),
      ],
      controller: _controller,
      useWarehouseScope: false,
      endpoint: '/bff/payments',
      extraQuery: {'direction': onlyIn ? 'IN' : widget.initialDirection},
      defaultSort: 'paidAt,desc',
      searchHint: 'Tìm mã phiếu, đối tượng, phiếu gốc...',
      itemLabel: 'phiếu',
      actions: [
        if (canWrite)
          OutlinedButton(
            onPressed: () => context.go('/app/payments/new?direction=OUT'),
            child: const Text('Tạo phiếu chi'),
          ),
        if (canWrite)
          ElevatedButton(
            onPressed: () => context.go('/app/payments/new?direction=IN'),
            child: const Text('Tạo phiếu thu'),
          ),
      ],
      filters: [
        if (!onlyIn)
          ListFilter(
            key: 'direction',
            label: 'Loại',
            options: const [
              (null, 'Tất cả'),
              ('IN', 'Phiếu thu'),
              ('OUT', 'Phiếu chi'),
            ],
          ),
      ],
      columns: [
        TableColumnDef.text(
          'Mã phiếu',
          (r) => '${r['paymentCode']}',
          minWidth: 130,
          sortKey: 'paymentCode',
          bold: true,
        ),
        TableColumnDef(
          label: 'Loại',
          flex: 1,
          minWidth: 100,
          cell: (r) => r['direction'] == 'OUT'
              ? const StatusChip(label: 'Chi', tone: ChipTone.purple)
              : const StatusChip(label: 'Thu', tone: ChipTone.info),
        ),
        TableColumnDef.text(
          'Ngày',
          (r) => Fmt.date(r['paidAt']),
          minWidth: 100,
          sortKey: 'paidAt',
        ),
        TableColumnDef.text(
          'Đối tượng',
          (r) => '${r['partyName'] ?? '—'}',
          flex: 3,
          minWidth: 170,
        ),
        TableColumnDef.text(
          'Phiếu gốc',
          (r) => r['referenceCode'] == null ? '—' : '${r['referenceCode']}',
          minWidth: 130,
        ),
        TableColumnDef(
          label: 'Số tiền',
          minWidth: 120,
          align: Alignment.centerRight,
          sortKey: 'amount',
          cell: (r) => Text(
            Fmt.money(r['amount']),
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: r['direction'] == 'OUT' ? c.danger : c.success,
            ),
          ),
        ),
        TableColumnDef.text(
          'Hình thức',
          (r) => _methodLabel(r['method']),
          minWidth: 110,
        ),
        TableColumnDef(
          label: 'Trạng thái',
          minWidth: 120,
          cell: (r) => r['status'] == 'CANCELLED'
              ? const StatusChip(label: 'Đã hủy', tone: ChipTone.danger)
              : const StatusChip(label: 'Hiệu lực', tone: ChipTone.success),
        ),
        TableColumnDef(
          label: 'Thao tác',
          minWidth: 120,
          align: Alignment.center,
          cell: (r) => Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              PrintButton(
                compact: true,
                docs: [
                  PrintDoc(
                    r['direction'] == 'OUT' ? 'Phiếu chi' : 'Phiếu thu',
                    '/bff/api/payments/${r['id']}/print',
                  ),
                ],
              ),
              if (canWrite && r['status'] != 'CANCELLED')
                PopupMenuButton<String>(
                  icon: const Icon(Icons.more_horiz),
                  onSelected: (_) => _cancel(r),
                  itemBuilder: (_) => const [
                    PopupMenuItem(value: 'cancel', child: Text('Hủy phiếu')),
                  ],
                ),
            ],
          ),
        ),
      ],
    );
  }
}

typedef _Json = Map<String, dynamic>;

/// Loại phiếu gốc chọn được khi lập phiếu thu/chi: (Reference_Type, nhãn, endpoint BFF).
const _refKinds = <String, (String, String)>{
  'Export_Receipts': ('Phiếu xuất', '/bff/export-receipts'),
  'Import_Receipts': ('Phiếu nhập', '/bff/import-receipts'),
  'Rental_Receipts': ('Phiếu thuê / mượn', '/bff/rental-receipts'),
  'Sales_Returns': ('Phiếu đổi trả hàng bán', '/bff/sales-returns'),
};

/// Thu (IN): xuất hàng, thuê/mượn. Chi (OUT): nhập hàng, hoàn cọc thuê/mượn, hoàn tiền đổi trả.
List<String> _kindsFor(String direction) => direction == 'OUT'
    ? const ['Import_Receipts', 'Rental_Receipts', 'Sales_Returns']
    : const ['Export_Receipts', 'Rental_Receipts'];

/// Tạo phiếu thu / phiếu chi — POST /bff/api/payments.
/// Phiếu gốc bắt buộc; đối tượng (Parties.ID) và số tiền còn lại tự điền theo phiếu gốc.
class PaymentFormPage extends ConsumerStatefulWidget {
  const PaymentFormPage({super.key, required this.direction});

  /// IN hoặc OUT
  final String direction;

  @override
  ConsumerState<PaymentFormPage> createState() => _PaymentFormPageState();
}

class _PaymentFormPageState extends ConsumerState<PaymentFormPage> {
  final _form = GlobalKey<FormState>();
  final _amount = TextEditingController();
  final _note = TextEditingController();
  late String _refType = _kindsFor(widget.direction).first;
  int? _referenceId;
  _Json? _ref;
  String _method = 'CASH';
  DateTime? _paidAt = DateTime.now();
  bool _saving = false;
  late Future<List<_Json>> _refs = _loadRefs();

  bool get _out => widget.direction == 'OUT';

  @override
  void dispose() {
    _amount.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<List<_Json>> _loadRefs() async {
    final page = await ref
        .read(apiClientProvider)
        .getPage(_refKinds[_refType]!.$2, query: {'size': 100});
    return page.items.where((r) {
      if (r['status'] == 'CANCELLED') return false;
      // phiếu xuất/nhập: chỉ lấy phiếu còn nợ
      if (_refType == 'Export_Receipts' || _refType == 'Import_Receipts') {
        return (Fmt.toNum(r['remain']) ?? 0) > 0;
      }
      return true;
    }).toList();
  }

  /// Parties.ID của đối tượng trong phiếu gốc (khách hàng → party qua danh mục khách).
  (int?, String) _party(_Json r, List<_Json> customers) {
    switch (_refType) {
      case 'Import_Receipts':
        return (
          (r['sourcePartyId'] as num?)?.toInt(),
          '${r['sourcePartyName'] ?? ''}',
        );
      case 'Sales_Returns':
        return ((r['partyId'] as num?)?.toInt(), '${r['partyName'] ?? ''}');
      default:
        final cust = customers
            .where((c) => c['id'] == r['customerId'])
            .firstOrNull;
        final pid = ((cust?['party'] as Map?)?['id'] as num?)?.toInt();
        return (pid, '${r['customerName'] ?? ''}');
    }
  }

  String _refText(_Json r, List<_Json> customers) {
    final who = _party(r, customers).$2;
    final remain = Fmt.toNum(r['remain']) ?? 0;
    return '${r['receiptCode']} · $who'
        '${remain > 0 ? ' · còn ${Fmt.money(remain)}' : ''}';
  }

  Future<void> _save(List<_Json> customers) async {
    if (!_form.currentState!.validate()) return;
    final r = _ref;
    if (r == null) {
      Toast.error(context, 'Chọn phiếu gốc.');
      return;
    }
    final partyId = _party(r, customers).$1;
    if (partyId == null) {
      Toast.error(context, 'Không xác định được đối tượng của phiếu gốc.');
      return;
    }
    final amount = parseNumber(_amount.text) ?? 0;
    if (amount <= 0) {
      Toast.error(context, 'Số tiền phải lớn hơn 0.');
      return;
    }
    final remain = Fmt.toNum(r['remain']);
    if (remain != null && amount > remain) {
      Toast.error(context, 'Số tiền vượt quá số còn lại ${Fmt.money(remain)}.');
      return;
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/payments',
            data: {
              'direction': widget.direction,
              'partyId': partyId,
              'amount': amount,
              'method': _method,
              'paidAt': Fmt.apiDateTime(_paidAt),
              'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
              'referenceType': _refType,
              'referenceId': _referenceId,
            },
          );
      if (!mounted) return;
      await _afterSaved(Map<String, dynamic>.from(result as Map? ?? {}));
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  /// Sau khi tạo xong: hỏi in ngay hay quay lại danh sách.
  Future<void> _afterSaved(_Json saved) async {
    final title = paymentTitle(widget.direction);
    Toast.success(context, 'Đã tạo $title ${saved['paymentCode'] ?? ''}.');
    final printNow = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Đã tạo $title ${saved['paymentCode'] ?? ''}'),
        content: Text('In $title ngay bây giờ?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Để sau'),
          ),
          ElevatedButton.icon(
            onPressed: () => Navigator.pop(ctx, true),
            icon: const Icon(Icons.print_outlined),
            label: const Text('In ngay'),
          ),
        ],
      ),
    );
    if (printNow == true && saved['id'] != null && mounted) {
      await PrintButton.run(
        context,
        ref,
        PrintDoc(title, '/bff/api/payments/${saved['id']}/print'),
        download: false,
      );
    }
    if (mounted) context.go('/app/payments');
  }

  @override
  Widget build(BuildContext context) {
    final out = _out;
    final title = out ? 'Tạo phiếu chi' : 'Tạo phiếu thu';
    final session = ref.watch(sessionProvider).value;
    final customers = ref.watch(customersLookupProvider).value ?? const [];
    final party = _ref == null ? null : _party(_ref!, customers);

    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PageHeader(
            title: title,
            crumbs: [
              const Crumb('Trang chủ', '/app/dashboard'),
              const Crumb('Thu chi', '/app/payments'),
              Crumb(title),
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
                AppDropdown<String>(
                  label: 'Loại phiếu gốc',
                  required: true,
                  value: _refType,
                  items: [
                    for (final k in _kindsFor(widget.direction))
                      (k, _refKinds[k]!.$1),
                  ],
                  onChanged: (v) => setState(() {
                    _refType = v ?? _refType;
                    _referenceId = null;
                    _ref = null;
                    _refs = _loadRefs();
                  }),
                ),
                FutureBuilder<List<_Json>>(
                  future: _refs,
                  builder: (context, snap) {
                    final list = snap.data ?? const <_Json>[];
                    return AppDropdown<int>(
                      label: 'Phiếu gốc',
                      required: true,
                      hint: snap.connectionState == ConnectionState.waiting
                          ? 'Đang tải...'
                          : (list.isEmpty
                                ? 'Không có phiếu phù hợp'
                                : 'Chọn phiếu gốc'),
                      value: _referenceId,
                      items: [
                        for (final r in list)
                          ((r['id'] as num).toInt(), _refText(r, customers)),
                      ],
                      onChanged: (v) => setState(() {
                        _referenceId = v;
                        _ref = list.where((r) => r['id'] == v).firstOrNull;
                        final remain = Fmt.toNum(_ref?['remain']) ?? 0;
                        if (remain > 0) _amount.text = '${remain.toInt()}';
                      }),
                    );
                  },
                ),
                AppTextField(
                  label: out ? 'Trả cho' : 'Thu của',
                  readOnly: true,
                  hint: 'Tự điền theo phiếu gốc',
                  controller: TextEditingController(text: party?.$2 ?? ''),
                ),
                AppTextField(
                  label: 'Số tiền (₫)',
                  required: true,
                  controller: _amount,
                  keyboardType: TextInputType.number,
                  inputFormatters: digitsOnly,
                  validator: (v) => (parseNumber(v) ?? 0) <= 0
                      ? 'Nhập số tiền lớn hơn 0'
                      : null,
                ),
                AppDropdown<String>(
                  label: 'Hình thức',
                  value: _method,
                  items: const [
                    ('CASH', 'Tiền mặt'),
                    ('TRANSFER', 'Chuyển khoản'),
                  ],
                  onChanged: (v) => setState(() => _method = v ?? 'CASH'),
                ),
                DateField(
                  label: out ? 'Ngày chi' : 'Ngày thu',
                  required: true,
                  value: _paidAt,
                  onChanged: (d) => setState(() => _paidAt = d),
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
            title: 'Ghi chú',
            child: AppTextField(
              label: 'Ghi chú cho phiếu',
              controller: _note,
              maxLines: 3,
            ),
          ),
          FormFooter(
            saving: _saving,
            onCancel: () => context.go('/app/payments'),
            onSave: () => _save(customers),
            saveLabel: 'Lưu phiếu',
          ),
        ],
      ),
    );
  }
}
