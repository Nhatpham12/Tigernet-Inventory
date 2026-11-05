import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
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

/// Figma 978:1930 "Nhà cung cấp / Danh sách".
class SupplierListPage extends StatelessWidget {
  const SupplierListPage({super.key});

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Danh sách nhà cung cấp',
    subtitle: 'Thông tin liên hệ, mã số thuế và hợp đồng nhà cung cấp',
    crumbs: const [
      Crumb('Trang chủ', '/app/dashboard'),
      Crumb('Nhập hàng'),
      Crumb('Danh sách nhà cung cấp'),
    ],
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
    columns: [
      ..._partyColumns('NCC'),
      TableColumnDef.text(
        'Số hợp đồng',
        (r) => '${r['contractNo'] ?? '—'}',
        minWidth: 120,
      ),
    ],
  );
}

/// Danh sách khách hàng (dùng cho phiếu xuất / thuê / mượn).
class CustomerListPage extends StatefulWidget {
  const CustomerListPage({super.key});

  @override
  State<CustomerListPage> createState() => _CustomerListPageState();
}

class _CustomerListPageState extends State<CustomerListPage> {
  final controller = PagedListController();

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PagedListView(
      title: 'Khách hàng',
      subtitle: 'Khách mua, thuê và mượn hàng',
      crumbs: const [
        Crumb('Trang chủ', '/app/dashboard'),
        Crumb('Xuất kho'),
        Crumb('Khách hàng'),
      ],
      controller: controller,
      actions: [
        ElevatedButton(
          onPressed: () async {
            final created = await showDialog<bool>(
              context: context,
              builder: (_) => const Dialog(
                child: SizedBox(
                  width: 720,
                  child: PartnerForm(
                    kind: PartnerKind.customer,
                    inDialog: true,
                  ),
                ),
              ),
            );
            if (created == true) controller.reload();
          },
          child: const Text('Thêm khách hàng'),
        ),
      ],
      endpoint: '/bff/customers',
      useWarehouseScope: false,
      searchHint: 'Tìm tên, mã số thuế, số điện thoại...',
      itemLabel: 'khách hàng',
      columns: _partyColumns('KH'),
    );
  }
}

enum PartnerKind { supplier, customer }

/// Figma 978:2025 "Nhà cung cấp / Thêm nhà cung cấp" — dùng chung cho khách hàng.
class PartnerForm extends ConsumerStatefulWidget {
  const PartnerForm({super.key, required this.kind, this.inDialog = false});

  final PartnerKind kind;
  final bool inDialog;

  @override
  ConsumerState<PartnerForm> createState() => _PartnerFormState();
}

class _PartnerFormState extends ConsumerState<PartnerForm> {
  final _form = GlobalKey<FormState>();
  final _c = {
    for (final k in [
      'fullName',
      'taxCode',
      'nationalId',
      'address',
      'phone',
      'email',
      'contractNo',
      'trackingCode',
    ])
      k: TextEditingController(),
  };
  String _partyType = 'ORGANIZATION';
  bool _saving = false;

  bool get _supplier => widget.kind == PartnerKind.supplier;

  @override
  void dispose() {
    for (final c in _c.values) {
      c.dispose();
    }
    super.dispose();
  }

  String? _v(String key) =>
      _c[key]!.text.trim().isEmpty ? null : _c[key]!.text.trim();

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _saving = true);
    final party = {
      'partyType': _partyType,
      'fullName': _v('fullName'),
      'taxCode': _v('taxCode'),
      'nationalId': _v('nationalId'),
      'address': _v('address'),
      'phone': _v('phone'),
      'email': _v('email'),
    };
    try {
      final api = ref.read(apiClientProvider);
      if (_supplier) {
        await api.post(
          '/bff/api/suppliers',
          data: {
            'party': party,
            'contractNo': _v('contractNo'),
            'trackingCode': _v('trackingCode'),
          },
        );
        ref.invalidate(suppliersLookupProvider);
      } else {
        await api.post('/bff/api/customers', data: {'party': party});
        ref.invalidate(customersLookupProvider);
      }
      if (!mounted) return;
      Toast.success(
        context,
        _supplier ? 'Đã thêm nhà cung cấp.' : 'Đã thêm khách hàng.',
      );
      if (widget.inDialog) {
        Navigator.pop(context, true);
      } else {
        context.go(_supplier ? '/app/suppliers' : '/app/customers');
      }
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = _supplier ? 'Thêm nhà cung cấp' : 'Thêm khách hàng';
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
                onPressed: () => widget.inDialog
                    ? Navigator.pop(context, false)
                    : context.go(
                        _supplier ? '/app/suppliers' : '/app/customers',
                      ),
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
