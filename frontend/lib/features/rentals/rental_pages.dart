import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session_controller.dart';
import '../../core/network/api_exception.dart';
import '../../core/providers.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/form/form_footer.dart';
import '../../shared/form/unsaved_changes.dart';
import '../../shared/form/receipt_line_editor.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

/// RENT = cho thuê, LEND = cho mượn (ERD v7, dùng chung /rental-receipts).
enum RentalKind {
  rent('RENT', 'Cho thuê', 'thuê', '/app/rentals'),
  lend('LEND', 'Cho mượn', 'mượn', '/app/lends');

  const RentalKind(this.code, this.menu, this.noun, this.basePath);

  final String code;
  final String menu;
  final String noun;
  final String basePath;
}

/// Figma 978:2705 / 1112:3135 "Danh sách phiếu" thuê – mượn; 1294:1434 báo cáo cho thuê (tab theo trạng thái).
class RentalListPage extends StatelessWidget {
  const RentalListPage({
    super.key,
    required this.kind,
    this.initialState,
    this.asReport = false,
  });

  final RentalKind? kind;
  final String? initialState;
  final bool asReport;

  @override
  Widget build(BuildContext context) {
    final k = kind;
    return PagedListView(
      key: ValueKey('${k?.code}-$initialState-$asReport'),
      title: asReport
          ? 'Báo cáo cho thuê / cho mượn'
          : 'Danh sách phiếu ${k!.noun}',
      subtitle: asReport ? 'Theo dõi hàng đang thuê, đã trả và quá hạn' : null,
      crumbs: [
        const Crumb('Trang chủ', '/app/dashboard'),
        Crumb(asReport ? 'Báo cáo' : k!.menu),
        Crumb(asReport ? 'Cho thuê' : 'Danh sách phiếu'),
      ],
      actions: [
        if (!asReport)
          ElevatedButton(
            onPressed: () => context.go('${k.basePath}/new'),
            child: Text('Tạo phiếu ${k!.noun}'),
          ),
      ],
      endpoint: '/bff/rental-receipts',
      extraQuery: {'type': k?.code, 'state': initialState},
      defaultSort: 'dateOut,desc',
      searchHint: 'Tìm mã phiếu, khách hàng, serial...',
      itemLabel: 'phiếu',
      filters: [
        if (asReport)
          const ListFilter(
            key: 'type',
            label: 'Loại',
            options: [(null, 'Tất cả'), ('RENT', 'Thuê'), ('LEND', 'Mượn')],
          ),
        const ListFilter(
          key: 'state',
          label: 'Trạng thái',
          options: [
            (null, 'Tất cả'),
            ('NOT_RETURNED', 'Chưa trả'),
            ('RETURNED', 'Đã trả'),
            ('OVERDUE', 'Quá hạn'),
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
        if (asReport)
          TableColumnDef(
            label: 'Loại',
            flex: 1,
            minWidth: 80,
            cell: (r) => r['rentalType'] == 'LEND'
                ? const StatusChip(label: 'Mượn', tone: ChipTone.purple)
                : const StatusChip(label: 'Thuê', tone: ChipTone.success),
          ),
        TableColumnDef.text(
          'Khách hàng',
          (r) => '${r['customerName'] ?? '—'}',
          flex: 3,
          minWidth: 170,
        ),
        TableColumnDef.text(
          'Kho',
          (r) => '${r['warehouseName'] ?? '—'}',
          minWidth: 110,
        ),
        TableColumnDef.text(
          'Ngày xuất',
          (r) => Fmt.date(r['dateOut']),
          minWidth: 100,
          sortKey: 'dateOut',
        ),
        TableColumnDef.text(
          'Hạn trả',
          (r) => Fmt.date(r['dueDate']),
          minWidth: 100,
          sortKey: 'dueDate',
        ),
        TableColumnDef.text(
          'SL',
          (r) => Fmt.number(r['totalQuantity']),
          flex: 1,
          minWidth: 50,
          align: Alignment.centerRight,
        ),
        if (k != RentalKind.lend)
          TableColumnDef.text(
            'Tiền thuê',
            (r) => Fmt.money(r['totalRent']),
            minWidth: 110,
            align: Alignment.centerRight,
            sortKey: 'totalRent',
          ),
        TableColumnDef.text(
          'Đặt cọc',
          (r) => Fmt.money(r['deposit']),
          minWidth: 110,
          align: Alignment.centerRight,
        ),
        TableColumnDef(
          label: 'Trạng thái',
          minWidth: 120,
          cell: (r) => Statuses.rental(r['rentalState'] as String?),
        ),
      ],
    );
  }
}

/// Figma 978:2585 "Tạo phiếu cho thuê" / 1112:2889 "Tạo phiếu cho mượn" — POST /bff/api/rental-receipts.
class RentalFormPage extends ConsumerStatefulWidget {
  const RentalFormPage({super.key, required this.kind});

  final RentalKind kind;

  @override
  ConsumerState<RentalFormPage> createState() => _RentalFormPageState();
}

class _RentalFormPageState extends ConsumerState<RentalFormPage> {
  final _form = GlobalKey<FormState>();
  final _receiverName = TextEditingController();
  final _receiverPhone = TextEditingController();
  final _receiverAddress = TextEditingController();
  final _deposit = TextEditingController();
  final _pricePerDay = TextEditingController();
  final List<ReceiptLine> _lines = [];
  int? _customerId;
  int? _warehouseId;
  DateTime? _dateOut = DateTime.now();
  DateTime? _dueDate = DateTime.now().add(const Duration(days: 7));
  bool _saving = false;

  bool get _lend => widget.kind == RentalKind.lend;

  @override
  void initState() {
    super.initState();
    _warehouseId = ref.read(warehouseScopeProvider);
  }

  @override
  void dispose() {
    for (final c in [
      _receiverName,
      _receiverPhone,
      _receiverAddress,
      _deposit,
      _pricePerDay,
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

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    if (_lines.isEmpty || _lines.any((l) => l.quantity <= 0)) {
      Toast.error(context, 'Phiếu cần ít nhất một dòng hàng có số lượng > 0.');
      return;
    }
    if (_dueDate != null && _dateOut != null && _dueDate!.isBefore(_dateOut!)) {
      Toast.error(context, 'Hạn trả phải sau ngày xuất.');
      return;
    }
    setState(() => _saving = true);
    try {
      final result = await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/rental-receipts',
            data: {
              'customerId': _customerId,
              'warehouseId': _warehouseId,
              'rentalType': widget.kind.code,
              'receiverName': _text(_receiverName),
              'receiverPhone': _text(_receiverPhone),
              'receiverAddress': _text(_receiverAddress),
              'dateOut': Fmt.apiDateTime(_dateOut),
              'dueDate': Fmt.apiDateTime(_dueDate),
              'deposit': _lend ? 0 : parseNumber(_deposit.text),
              'rentalPricePerDay': parseNumber(_pricePerDay.text),
              'details': [
                for (final l in _lines)
                  {
                    'productId': l.productId,
                    'quantity': l.quantity,
                    'rentalPrice': _lend ? 0 : l.unitPrice,
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
      UnsavedChanges.clear();
      context.go(widget.kind.basePath);
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
    final products = ref.watch(productsLookupProvider);
    final totalRent = _lines.fold<num>(0, (s, l) => s + l.lineTotal);
    final days = _dateOut != null && _dueDate != null
        ? _dueDate!.difference(_dateOut!).inDays
        : 0;

    return FormPageLayout(
      formKey: _form,
      children: [
        PageHeader(
          title: 'Tạo phiếu cho ${widget.kind.noun}',
          crumbs: [
            const Crumb('Trang chủ', '/app/dashboard'),
            Crumb(widget.kind.menu, widget.kind.basePath),
            Crumb('Tạo phiếu ${widget.kind.noun}'),
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
                  label: _lend ? 'Người / đơn vị mượn' : 'Khách thuê',
                  required: true,
                  hint: 'Chọn khách hàng',
                  value: _customerId,
                  items: [
                    for (final c in list)
                      ((c['id'] as num).toInt(), partyName(c)),
                  ],
                  onChanged: (v) {
                    final party =
                        (list.firstWhere(
                              (c) => c['id'] == v,
                              orElse: () => {},
                            )['party']
                            as Map?) ??
                        {};
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
                error: (e, _) => Text(userMessage(e)),
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
              AppTextField(
                label: 'Người nhận hàng',
                controller: _receiverName,
              ),
              DateField(
                label: 'Ngày xuất',
                required: true,
                value: _dateOut,
                onChanged: (d) => setState(() => _dateOut = d),
              ),
              DateField(
                label: 'Hạn trả',
                required: true,
                value: _dueDate,
                onChanged: (d) => setState(() => _dueDate = d),
              ),
              AppTextField(
                label: 'SĐT người nhận',
                controller: _receiverPhone,
                keyboardType: TextInputType.phone,
              ),
              if (!_lend)
                AppTextField(
                  label: 'Tiền đặt cọc (₫)',
                  controller: _deposit,
                  keyboardType: TextInputType.number,
                  inputFormatters: digitsOnly,
                ),
              AppTextField(
                label: 'Giá thuê / ngày (tính phạt quá hạn)',
                controller: _pricePerDay,
                keyboardType: TextInputType.number,
                inputFormatters: digitsOnly,
              ),
              AppTextField(label: 'Địa chỉ', controller: _receiverAddress),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Hàng ${widget.kind.noun}',
          child: products.when(
            loading: () => const LoadingView(),
            error: (e, _) => StateView.error(e),
            data: (list) => ReceiptLineEditor(
              lines: _lines,
              products: list,
              showPrice: !_lend,
              priceLabel: 'Giá thuê cả kỳ',
              defaultPrice: (_) => null,
              onChanged: () => setState(() {}),
            ),
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Tổng kết',
          child: TotalsPanel(
            rows: [
              ('Số ngày ${widget.kind.noun}', '$days ngày', false),
              if (!_lend)
                (
                  'Tiền đặt cọc',
                  Fmt.money(parseNumber(_deposit.text) ?? 0),
                  false,
                ),
              if (!_lend) ('Tổng tiền thuê', Fmt.money(totalRent), true),
              if (_lend) ('Số mặt hàng', '${_lines.length}', true),
            ],
          ),
        ),
      ],
      footer: FormFooter(
        saving: _saving,
        onCancel: () => guardedGo(context, widget.kind.basePath),
        onSave: _save,
      ),
    );
  }
}
