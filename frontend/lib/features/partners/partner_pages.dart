import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

String _party(Map<String, dynamic> r, String field) =>
    '${(r['party'] as Map?)?[field] ?? '—'}';

List<TableColumnDef> _partyColumns(String codePrefix) => [
  TableColumnDef.text(
    'Mã',
    (r) => '$codePrefix${'${r['id']}'.padLeft(3, '0')}',
    flex: 1,
    minWidth: 80,
    sortKey: 'id',
  ),
  TableColumnDef.text(
    'Tên',
    (r) => _party(r, 'fullName'),
    flex: 3,
    minWidth: 200,
    bold: true,
  ),
  TableColumnDef(
    label: 'Loại',
    minWidth: 110,
    cell: (r) => (r['party'] as Map?)?['partyType'] == 'ORGANIZATION'
        ? const StatusChip(label: 'Tổ chức', tone: ChipTone.info)
        : const StatusChip(label: 'Cá nhân', tone: ChipTone.neutral),
  ),
  TableColumnDef.text('Mã số thuế', (r) => _party(r, 'taxCode'), minWidth: 120),
  TableColumnDef.text('Điện thoại', (r) => _party(r, 'phone'), minWidth: 120),
  TableColumnDef.text(
    'Email',
    (r) => _party(r, 'email'),
    flex: 2,
    minWidth: 160,
  ),
  TableColumnDef.text(
    'Địa chỉ',
    (r) => _party(r, 'address'),
    flex: 3,
    minWidth: 200,
  ),
];

enum PartnerKind { supplier, customer }

extension on PartnerKind {
  bool get isSupplier => this == PartnerKind.supplier;
  String get noun => isSupplier ? 'nhà cung cấp' : 'khách hàng';
  String get apiPath =>
      isSupplier ? '/bff/api/suppliers' : '/bff/api/customers';
  String get listPath => isSupplier ? '/app/suppliers' : '/app/customers';
}

/// Cột "Thao tác": sửa / xóa đối tác. Xóa là xóa mềm ở backend (phiếu cũ vẫn giữ tên đối tác).
TableColumnDef _actionsColumn({
  required PartnerKind kind,
  required void Function(Map<String, dynamic> row) onEdit,
  required void Function(Map<String, dynamic> row) onDelete,
}) => TableColumnDef(
  label: 'Thao tác',
  minWidth: 100,
  align: Alignment.center,
  cell: (r) => Builder(
    builder: (context) {
      final c = context.colors;
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          IconButton(
            tooltip: 'Sửa ${kind.noun}',
            icon: Icon(Icons.edit_outlined, size: 20, color: c.info),
            onPressed: () => onEdit(r),
          ),
          IconButton(
            tooltip: 'Xóa ${kind.noun}',
            icon: Icon(Icons.delete_outline, size: 20, color: c.danger),
            onPressed: () => onDelete(r),
          ),
        ],
      );
    },
  ),
);

/// Hỏi xác nhận rồi xóa đối tác; trả true nếu đã xóa.
Future<bool> _deletePartner(
  BuildContext context,
  WidgetRef ref,
  PartnerKind kind,
  Map<String, dynamic> r,
) async {
  final name = _party(r, 'fullName');
  final ok = await confirmDialog(
    context,
    title: 'Xóa ${kind.noun} $name',
    message:
        '${kind.isSupplier ? 'Nhà cung cấp' : 'Khách hàng'} sẽ không còn trong danh sách '
        'và không chọn được khi lập phiếu mới. Các phiếu đã lập vẫn giữ nguyên.',
    confirmLabel: 'Xóa',
    danger: true,
  );
  if (!ok) return false;
  try {
    await ref.read(apiClientProvider).delete('${kind.apiPath}/${r['id']}');
    ref.invalidate(
      kind.isSupplier ? suppliersLookupProvider : customersLookupProvider,
    );
    if (context.mounted) Toast.success(context, 'Đã xóa ${kind.noun} $name.');
    return true;
  } catch (e) {
    if (context.mounted) Toast.error(context, e);
    return false;
  }
}

/// Mở form đối tác trong hộp thoại (thêm khi [partnerId] null, sửa khi có); trả true nếu đã lưu.
Future<bool> _partnerDialog(
  BuildContext context,
  PartnerKind kind, {
  int? partnerId,
}) async =>
    await showDialog<bool>(
      context: context,
      builder: (_) => Dialog(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: SingleChildScrollView(
            child: PartnerForm(
              kind: kind,
              partnerId: partnerId,
              inDialog: true,
            ),
          ),
        ),
      ),
    ) ==
    true;

/// Figma 978:1930 "Nhà cung cấp / Danh sách".
class SupplierListPage extends ConsumerStatefulWidget {
  const SupplierListPage({super.key});

  @override
  ConsumerState<SupplierListPage> createState() => _SupplierListPageState();
}

class _SupplierListPageState extends ConsumerState<SupplierListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách nhà cung cấp',
    subtitle: 'Thông tin liên hệ, mã số thuế và hợp đồng nhà cung cấp',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Nhập hàng'),
      Crumb('Danh sách nhà cung cấp'),
    ],
    controller: _controller,
    actions: [
      ElevatedButton(
        onPressed: () => context.go('/app/suppliers/new'),
        child: const Text('Thêm nhà cung cấp'),
      ),
    ],
    endpoint: '/bff/suppliers',
    useWarehouseScope: false,
    searchHint: 'Tìm tên, mã số thuế, số điện thoại...',
    itemLabel: 'nhà cung cấp',
    onRowTap: (r) => context.go('/app/suppliers/${r['id']}/edit'),
    columns: [
      ..._partyColumns('NCC'),
      TableColumnDef.text(
        'Số hợp đồng',
        (r) => '${r['contractNo'] ?? '—'}',
        minWidth: 120,
      ),
      _actionsColumn(
        kind: PartnerKind.supplier,
        onEdit: (r) => context.go('/app/suppliers/${r['id']}/edit'),
        onDelete: (r) async {
          if (await _deletePartner(context, ref, PartnerKind.supplier, r)) {
            _controller.reload();
          }
        },
      ),
    ],
  );
}

/// Danh sách khách hàng (dùng cho phiếu xuất / thuê / mượn). Thêm / sửa trong hộp thoại.
class CustomerListPage extends ConsumerStatefulWidget {
  const CustomerListPage({super.key});

  @override
  ConsumerState<CustomerListPage> createState() => _CustomerListPageState();
}

class _CustomerListPageState extends ConsumerState<CustomerListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _open({int? id}) async {
    if (await _partnerDialog(context, PartnerKind.customer, partnerId: id)) {
      _controller.reload();
    }
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Khách hàng',
    subtitle: 'Khách mua, thuê và mượn hàng',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Xuất kho'),
      Crumb('Khách hàng'),
    ],
    controller: _controller,
    actions: [
      ElevatedButton(onPressed: _open, child: const Text('Thêm khách hàng')),
    ],
    endpoint: '/bff/customers',
    useWarehouseScope: false,
    searchHint: 'Tìm tên, mã số thuế, số điện thoại...',
    itemLabel: 'khách hàng',
    onRowTap: (r) => _open(id: (r['id'] as num).toInt()),
    columns: [
      ..._partyColumns('KH'),
      _actionsColumn(
        kind: PartnerKind.customer,
        onEdit: (r) => _open(id: (r['id'] as num).toInt()),
        onDelete: (r) async {
          if (await _deletePartner(context, ref, PartnerKind.customer, r)) {
            _controller.reload();
          }
        },
      ),
    ],
  );
}

/// Figma 978:2025 "Nhà cung cấp / Thêm nhà cung cấp" — dùng chung cho khách hàng.
/// [partnerId] != null: sửa (GET rồi PUT), ngược lại thêm mới (POST).
class PartnerForm extends ConsumerStatefulWidget {
  const PartnerForm({
    super.key,
    required this.kind,
    this.partnerId,
    this.inDialog = false,
  });

  final PartnerKind kind;
  final int? partnerId;
  final bool inDialog;

  @override
  ConsumerState<PartnerForm> createState() => _PartnerFormState();
}

class _PartnerFormState extends ConsumerState<PartnerForm> {
  static const _partyFields = [
    'fullName',
    'taxCode',
    'nationalId',
    'address',
    'phone',
    'email',
  ];
  static const _supplierFields = ['contractNo', 'trackingCode'];

  final _form = GlobalKey<FormState>();
  final _c = {
    for (final k in [..._partyFields, ..._supplierFields])
      k: TextEditingController(),
  };
  String _partyType = 'ORGANIZATION';
  bool _saving = false;
  late final Future<void> _loading = _isEdit ? _load() : Future.value();

  bool get _supplier => widget.kind.isSupplier;
  bool get _isEdit => widget.partnerId != null;

  Future<void> _load() async {
    final r =
        await ref
                .read(apiClientProvider)
                .get('${widget.kind.apiPath}/${widget.partnerId}')
            as Map;
    final party = (r['party'] as Map?) ?? const {};
    _partyType = '${party['partyType'] ?? 'ORGANIZATION'}';
    for (final k in _partyFields) {
      _c[k]!.text = '${party[k] ?? ''}';
    }
    for (final k in _supplierFields) {
      _c[k]!.text = '${r[k] ?? ''}';
    }
  }

  @override
  void dispose() {
    for (final c in _c.values) {
      c.dispose();
    }
    super.dispose();
  }

  /// Ô trống: khi thêm gửi null; khi sửa gửi '' vì backend bỏ qua null (giữ giá trị cũ).
  String? _v(String key) {
    final text = _c[key]!.text.trim();
    return text.isEmpty && !_isEdit ? null : text;
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _saving = true);
    final body = {
      'party': {
        'partyType': _partyType,
        for (final k in _partyFields) k: _v(k),
      },
      if (_supplier)
        for (final k in _supplierFields) k: _v(k),
    };
    try {
      final api = ref.read(apiClientProvider);
      final path = widget.kind.apiPath;
      if (_isEdit) {
        await api.put('$path/${widget.partnerId}', data: body);
      } else {
        await api.post(path, data: body);
      }
      ref.invalidate(
        _supplier ? suppliersLookupProvider : customersLookupProvider,
      );
      if (!mounted) return;
      Toast.success(
        context,
        '${_isEdit ? 'Đã cập nhật' : 'Đã thêm'} ${widget.kind.noun}.',
      );
      _close(true);
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _close(bool saved) => widget.inDialog
      ? Navigator.pop(context, saved)
      : context.go(widget.kind.listPath);

  @override
  Widget build(BuildContext context) => FutureBuilder<void>(
    future: _loading,
    builder: (context, snap) {
      if (snap.connectionState != ConnectionState.done) {
        return const LoadingView();
      }
      if (snap.hasError) return StateView.error(snap.error!);
      return _body(context);
    },
  );

  Widget _body(BuildContext context) {
    final noun = widget.kind.noun;
    final title = _isEdit ? 'Sửa $noun' : 'Thêm $noun';
    final body = Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!widget.inDialog)
            PageHeader(
              title: title,
              crumbs: [
                const Crumb('Trang chủ', '/app/dashboard'),
                const Crumb('Nhập hàng'),
                const Crumb('Danh sách nhà cung cấp', '/app/suppliers'),
                Crumb(title),
              ],
            ),
          SectionCard(
            title: widget.inDialog ? title : 'Thông tin chung',
            child: FormGrid(
              children: [
                AppDropdown<String>(
                  label: 'Loại đối tác',
                  required: true,
                  value: _partyType,
                  items: const [
                    ('ORGANIZATION', 'Tổ chức / doanh nghiệp'),
                    ('INDIVIDUAL', 'Cá nhân / hộ kinh doanh'),
                  ],
                  onChanged: (v) =>
                      setState(() => _partyType = v ?? 'ORGANIZATION'),
                ),
                AppTextField(
                  label: 'Tên',
                  required: true,
                  controller: _c['fullName'],
                  hint: 'Công ty Bosch Việt Nam',
                ),
                AppTextField(label: 'Mã số thuế', controller: _c['taxCode']),
                AppTextField(
                  label: 'Số điện thoại',
                  controller: _c['phone'],
                  keyboardType: TextInputType.phone,
                ),
                AppTextField(
                  label: 'Email',
                  controller: _c['email'],
                  keyboardType: TextInputType.emailAddress,
                  validator: (v) =>
                      v == null ||
                          v.isEmpty ||
                          RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(v)
                      ? null
                      : 'Email không hợp lệ',
                ),
                AppTextField(
                  label: 'CCCD (cá nhân)',
                  controller: _c['nationalId'],
                ),
                AppTextField(label: 'Địa chỉ', controller: _c['address']),
                if (_supplier)
                  AppTextField(
                    label: 'Số hợp đồng',
                    controller: _c['contractNo'],
                  ),
                if (_supplier)
                  AppTextField(
                    label: 'Mã theo dõi',
                    controller: _c['trackingCode'],
                  ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton(
                onPressed: () => _close(false),
                child: const Text('Hủy'),
              ),
              const SizedBox(width: 12),
              ElevatedButton(
                onPressed: _saving ? null : _save,
                child: Text(_saving ? 'Đang lưu...' : 'Lưu'),
              ),
            ],
          ),
        ],
      ),
    );
    return widget.inDialog
        ? Padding(padding: const EdgeInsets.all(16), child: body)
        : body;
  }
}
