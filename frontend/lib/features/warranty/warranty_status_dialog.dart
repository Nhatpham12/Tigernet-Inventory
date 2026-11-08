import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/utils/formatters.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../common/lookups.dart';
import 'warranty_common.dart';

/// Đổi trạng thái phiếu — PUT /bff/api/warranty-claims/{id}/status. Trả về true khi đã đổi.
Future<bool> showWarrantyStatusDialog(
  BuildContext context,
  Map<String, dynamic> claim,
) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (_) => _StatusDialog(claim: claim),
  );
  return result ?? false;
}

class _StatusDialog extends ConsumerStatefulWidget {
  const _StatusDialog({required this.claim});
  final Map<String, dynamic> claim;

  @override
  ConsumerState<_StatusDialog> createState() => _StatusDialogState();
}

class _StatusDialogState extends ConsumerState<_StatusDialog> {
  final _cost = TextEditingController();
  final _replacement = TextEditingController();
  final _note = TextEditingController();
  late String _target = kWarrantyTransitions[widget.claim['status']]!.first;
  String _result = 'REPAIRED';
  DateTime? _newExpiry;
  int? _restockWarehouseId;
  bool _saving = false;

  bool get _returning => _target == 'RETURNED';

  @override
  void dispose() {
    _cost.dispose();
    _replacement.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_returning &&
        _result == 'REPLACED' &&
        _replacement.text.trim().isEmpty) {
      Toast.error(context, 'Nhập số seri thiết bị thay thế.');
      return;
    }
    setState(() => _saving = true);
    String? text(TextEditingController c) =>
        c.text.trim().isEmpty ? null : c.text.trim();
    try {
      await ref
          .read(apiClientProvider)
          .put(
            '/bff/api/warranty-claims/${widget.claim['id']}/status',
            data: {
              'status': _target,
              if (_returning) ...{
                'result': _result,
                'cost': parseNumber(_cost.text),
                if (_result == 'REPLACED')
                  'replacementSerial': text(_replacement),
                if (_result != 'REJECTED')
                  'newWarrantyExpiry': Fmt.apiDateTime(
                    _newExpiry,
                  )?.substring(0, 10),
                'restockedWarehouseId': _restockWarehouseId,
              },
              'note': text(_note),
            },
          );
      if (!mounted) return;
      Toast.success(
        context,
        '${widget.claim['claimCode']}: ${warrantyStatusLabel(_target)}.',
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (mounted) Toast.error(context, e);
      setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final targets = kWarrantyTransitions[widget.claim['status']]!;
    final warehouses = ref.watch(warehousesLookupProvider);
    return AlertDialog(
      title: Text('Đổi trạng thái ${widget.claim['claimCode']}'),
      content: SizedBox(
        width: 460,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppDropdown<String>(
                label: 'Chuyển sang',
                value: _target,
                items: [for (final t in targets) (t, warrantyActionLabel(t))],
                onChanged: (v) => setState(() => _target = v ?? _target),
              ),
              if (_returning) ...[
                const SizedBox(height: 12),
                AppDropdown<String>(
                  label: 'Kết quả',
                  value: _result,
                  items: kWarrantyResults,
                  onChanged: (v) => setState(() => _result = v ?? _result),
                ),
                const SizedBox(height: 12),
                AppTextField(
                  label: 'Chi phí (₫)',
                  controller: _cost,
                  keyboardType: TextInputType.number,
                  inputFormatters: digitsOnly,
                ),
                if (_result == 'REPLACED') ...[
                  const SizedBox(height: 12),
                  AppTextField(
                    label: 'Số seri thay thế',
                    required: true,
                    controller: _replacement,
                  ),
                ],
                if (_result != 'REJECTED') ...[
                  const SizedBox(height: 12),
                  DateField(
                    label: 'Hạn bảo hành mới',
                    value: _newExpiry,
                    onChanged: (d) => setState(() => _newExpiry = d),
                  ),
                ],
                const SizedBox(height: 12),
                warehouses.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text('$e'),
                  data: (list) => AppDropdown<int?>(
                    label: 'Kho nhập lại',
                    value: _restockWarehouseId,
                    items: [
                      (null, 'Không nhập kho (trả thẳng khách)'),
                      for (final w in list)
                        ((w['id'] as num).toInt(), '${w['warehouseName']}'),
                    ],
                    onChanged: (v) => setState(() => _restockWarehouseId = v),
                  ),
                ),
              ],
              const SizedBox(height: 12),
              AppTextField(label: 'Ghi chú', controller: _note, maxLines: 2),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context, false),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: _saving ? null : _submit,
          child: Text(_saving ? 'Đang lưu...' : 'Xác nhận'),
        ),
      ],
    );
  }
}
