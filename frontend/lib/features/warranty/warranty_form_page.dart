import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/form/form_footer.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';
import 'warranty_common.dart';

/// Tiếp nhận bảo hành — tra cứu dòng bán theo serial hoặc phiếu xuất, POST /bff/api/warranty-claims
/// (mỗi thiết bị một phiếu). Lưu xong cho in biên bản tiếp nhận của tất cả phiếu vừa tạo.
class WarrantyClaimFormPage extends ConsumerStatefulWidget {
  const WarrantyClaimFormPage({super.key});

  @override
  ConsumerState<WarrantyClaimFormPage> createState() =>
      _WarrantyClaimFormPageState();
}

class _WarrantyClaimFormPageState extends ConsumerState<WarrantyClaimFormPage> {
  final _serial = TextEditingController();
  final _note = TextEditingController();
  final List<Json> _items = [];
  List<Json> _receiptLines = [];
  final Set<int> _picked = {};
  bool _bySerial = true;
  int? _warehouseId;
  int? _receiptId;
  DateTime? _receivedAt = DateTime.now();
  bool _looking = false;
  bool _saving = false;
  List<Json> _created = [];

  @override
  void initState() {
    super.initState();
    _warehouseId = ref.read(warehouseScopeProvider);
  }

  @override
  void dispose() {
    _serial.dispose();
    _note.dispose();
    super.dispose();
  }

  bool _has(Json line) =>
      _items.any((i) => i['exportDetailId'] == line['exportDetailId']);

  Future<void> _lookupSerial() async {
    final serial = _serial.text.trim();
    if (serial.isEmpty) return;
    setState(() => _looking = true);
    try {
      final res = await ref
          .read(apiClientProvider)
          .get('/bff/api/warranty-claims/lookup', query: {'serial': serial});
      final line = Json.from(res as Map);
      if (!mounted) return;
      if (_has(line)) {
        Toast.error(context, 'Serial $serial đã có trong danh sách.');
      } else {
        setState(() {
          _items.add(line);
          _serial.clear();
        });
      }
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _looking = false);
    }
  }

  Future<void> _lookupReceipt(int? id) async {
    setState(() {
      _receiptId = id;
      _receiptLines = [];
      _picked.clear();
    });
    if (id == null) return;
    setState(() => _looking = true);
    try {
      final lines = await ref
          .read(apiClientProvider)
          .getList(
            '/bff/api/warranty-claims/lookup',
            query: {'exportReceiptId': id},
          );
      if (mounted) setState(() => _receiptLines = lines);
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _looking = false);
    }
  }

  void _addPicked() {
    setState(() {
      for (final l in _receiptLines) {
        if (_picked.contains(l['exportDetailId']) && !_has(l)) _items.add(l);
      }
      _picked.clear();
    });
  }

  Future<void> _save() async {
    if (_items.isEmpty) {
      Toast.error(context, 'Thêm ít nhất một thiết bị cần bảo hành.');
      return;
    }
    if (_warehouseId == null) {
      Toast.error(context, 'Chọn kho tiếp nhận.');
      return;
    }
    setState(() => _saving = true);
    final api = ref.read(apiClientProvider);
    final done = <Json>[];
    final failed = <String>[];
    for (final item in List<Json>.from(_items)) {
      try {
        final res = await api.post(
          '/bff/api/warranty-claims',
          data: {
            'exportDetailId': item['exportDetailId'],
            'inventoryId': item['inventoryId'],
            'receivedWarehouseId': _warehouseId,
            'receivedAt': Fmt.apiDateTime(_receivedAt),
            'note': _note.text.trim(),
          },
        );
        done.add(Json.from(res as Map));
        _items.remove(item);
      } catch (e) {
        failed.add('${item['serial'] ?? item['productName']}: $e');
      }
    }
    if (!mounted) return;
    setState(() {
      _saving = false;
      _created = [..._created, ...done];
    });
    if (failed.isNotEmpty) {
      Toast.error(context, 'Không lưu được: ${failed.join('; ')}');
    } else {
      Toast.success(context, 'Đã tạo ${done.length} phiếu bảo hành.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final warehouses = ref.watch(warehousesLookupProvider);
    final receipts = ref.watch(_exportReceiptsProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const PageHeader(
          title: 'Tiếp nhận bảo hành',
          crumbs: [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Bảo hành', '/app/warranty-claims'),
            Crumb('Tiếp nhận bảo hành'),
          ],
        ),
        if (_created.isNotEmpty) ...[
          _createdBanner(c),
          const SizedBox(height: 16),
        ],
        SectionCard(
          title: 'Thông tin tiếp nhận',
          child: FormGrid(
            children: [
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
              DateField(
                label: 'Ngày nhận',
                required: true,
                value: _receivedAt,
                onChanged: (d) => setState(() => _receivedAt = d),
              ),
              AppTextField(
                label: 'Tình trạng thiết bị khi nhận',
                controller: _note,
                maxLines: 2,
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Tìm thiết bị đã bán',
          trailing: SegmentedButton<bool>(
            showSelectedIcon: false,
            segments: const [
              ButtonSegment(value: true, label: Text('Theo số seri')),
              ButtonSegment(value: false, label: Text('Theo phiếu xuất')),
            ],
            selected: {_bySerial},
            onSelectionChanged: (s) => setState(() => _bySerial = s.first),
          ),
          child: _bySerial ? _serialSearch() : _receiptSearch(receipts, c),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Thiết bị nhận bảo hành (${_items.length})',
          child: _items.isEmpty
              ? Text(
                  'Chưa có thiết bị. Tra cứu theo số seri hoặc chọn từ phiếu xuất.',
                  style: TextStyle(color: c.muted),
                )
              : Column(
                  children: [
                    for (final item in _items)
                      _ItemCard(
                        item: item,
                        onRemove: () => setState(() => _items.remove(item)),
                      ),
                  ],
                ),
        ),
        FormFooter(
          saving: _saving,
          saveLabel: 'Lưu phiếu',
          onCancel: () => context.go('/app/warranty-claims'),
          onSave: _save,
        ),
      ],
    );
  }

  Widget _createdBanner(AppColors c) => Container(
    padding: const EdgeInsets.all(16),
    decoration: BoxDecoration(
      color: c.successBg,
      borderRadius: BorderRadius.circular(8),
    ),
    child: Wrap(
      spacing: 16,
      runSpacing: 8,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        Icon(Icons.check_circle, color: c.success),
        Text(
          'Đã tạo ${_created.length} phiếu: ${_created.map((e) => e['claimCode']).join(', ')}',
          style: TextStyle(fontWeight: FontWeight.w600, color: c.text),
        ),
        PrintButton(docs: [warrantyPrintDoc(_created.map((e) => e['id']))]),
        TextButton(
          onPressed: () => context.go('/app/warranty-claims'),
          child: const Text('Về danh sách'),
        ),
      ],
    ),
  );

  Widget _serialSearch() => Row(
    crossAxisAlignment: CrossAxisAlignment.end,
    children: [
      Expanded(
        child: AppTextField(
          label: 'Số seri',
          controller: _serial,
          onSubmitted: (_) => _lookupSerial(),
        ),
      ),
      const SizedBox(width: 12),
      SizedBox(
        height: 44,
        child: ElevatedButton.icon(
          onPressed: _looking ? null : _lookupSerial,
          icon: const Icon(Icons.search),
          label: const Text('Tra cứu'),
        ),
      ),
    ],
  );

  Widget _receiptSearch(AsyncValue<List<Json>> receipts, AppColors c) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      receipts.when(
        loading: () => const LinearProgressIndicator(),
        error: (e, _) => Text('$e'),
        data: (list) => AppDropdown<int?>(
          label: 'Phiếu xuất',
          hint: 'Chọn phiếu xuất',
          value: _receiptId,
          items: [
            for (final r in list)
              (
                (r['id'] as num).toInt(),
                '${r['receiptCode']} · ${r['customerName'] ?? ''}',
              ),
          ],
          onChanged: _lookupReceipt,
        ),
      ),
      if (_looking)
        const Padding(
          padding: EdgeInsets.only(top: 8),
          child: LinearProgressIndicator(),
        ),
      for (final l in _receiptLines)
        CheckboxListTile(
          dense: true,
          controlAffinity: ListTileControlAffinity.leading,
          value: _picked.contains(l['exportDetailId']) || _has(l),
          onChanged: _has(l)
              ? null
              : (v) => setState(() {
                  final id = (l['exportDetailId'] as num).toInt();
                  v == true ? _picked.add(id) : _picked.remove(id);
                }),
          title: Text('${l['productName']} · ${l['serial'] ?? 'không serial'}'),
          subtitle: Text(
            'Hạn BH ${Fmt.date(l['warrantyExpiry'])} · ${l['inWarranty'] == true ? 'còn bảo hành' : 'hết bảo hành'}',
            style: TextStyle(
              color: l['inWarranty'] == true ? c.success : c.danger,
            ),
          ),
        ),
      if (_receiptLines.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: OutlinedButton(
            onPressed: _picked.isEmpty ? null : _addPicked,
            child: Text('Thêm ${_picked.length} dòng đã chọn'),
          ),
        ),
    ],
  );
}

final _exportReceiptsProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/export-receipts'),
);

class _ItemCard extends StatelessWidget {
  const _ItemCard({required this.item, required this.onRemove});

  final Json item;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final inWarranty = item['inWarranty'] == true;
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border.all(color: inWarranty ? c.divider : c.danger),
        borderRadius: BorderRadius.circular(8),
        color: inWarranty ? null : c.dangerBg,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${item['productName']}',
                  style: TextStyle(fontWeight: FontWeight.w700, color: c.text),
                ),
                const SizedBox(height: 4),
                Text(
                  'Serial: ${item['serial'] ?? '—'} · Khách: ${item['customerName'] ?? '—'}',
                  style: TextStyle(fontSize: 13, color: c.text),
                ),
                Text(
                  'Phiếu xuất ${item['exportReceiptCode'] ?? '—'} · Ngày bán ${Fmt.date(item['dateOut'])} · Hạn BH ${Fmt.date(item['warrantyExpiry'])}',
                  style: TextStyle(fontSize: 13, color: c.muted),
                ),
                if (!inWarranty)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      'Cảnh báo: thiết bị đã hết hạn bảo hành. Vẫn có thể tiếp nhận (tính phí).',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: c.danger,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          StatusChip(
            label: inWarranty ? 'Còn bảo hành' : 'Hết bảo hành',
            tone: inWarranty ? ChipTone.success : ChipTone.danger,
          ),
          IconButton(
            tooltip: 'Bỏ',
            icon: const Icon(Icons.close),
            onPressed: onRemove,
          ),
        ],
      ),
    );
  }
}
