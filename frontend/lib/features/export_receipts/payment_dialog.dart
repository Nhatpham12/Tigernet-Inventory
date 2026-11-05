import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';

/// Ghi nhận thanh toán cho phiếu xuất — POST /bff/api/export-receipts/{id}/payments.
/// Trả về true khi đã ghi nhận (Figma: "nhận thêm" hoặc "tất toán").
Future<bool> showPaymentDialog(
  BuildContext context,
  Map<String, dynamic> receipt,
) async {
  final result = await showDialog<bool>(
    context: context,
    builder: (_) => _PaymentDialog(receipt: receipt),
  );
  return result ?? false;
}

class _PaymentDialog extends ConsumerStatefulWidget {
  const _PaymentDialog({required this.receipt});

  final Map<String, dynamic> receipt;

  @override
  ConsumerState<_PaymentDialog> createState() => _PaymentDialogState();
}

class _PaymentDialogState extends ConsumerState<_PaymentDialog> {
  final _amount = TextEditingController();
  final _note = TextEditingController();
  String _method = 'CASH';
  bool _saving = false;

  num get _remain => Fmt.toNum(widget.receipt['remain']) ?? 0;

  @override
  void dispose() {
    _amount.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final amount = parseNumber(_amount.text) ?? 0;
    if (amount <= 0 || amount > _remain) {
      Toast.error(
        context,
        'Số tiền phải lớn hơn 0 và không vượt quá ${Fmt.money(_remain)}.',
      );
      return;
    }
    setState(() => _saving = true);
    try {
      await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/export-receipts/${widget.receipt['id']}/payments',
            data: {
              'amount': amount,
              'method': _method,
              'note': _note.text.trim().isEmpty ? null : _note.text.trim(),
            },
          );
      if (!mounted) return;
      Toast.success(
        context,
        'Đã ghi nhận ${Fmt.money(amount)} cho ${widget.receipt['receiptCode']}.',
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (mounted) Toast.error(context, e);
      setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return AlertDialog(
      title: Text('Ghi nhận thanh toán · ${widget.receipt['receiptCode']}'),
      content: SizedBox(
        width: 440,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Khách hàng: ${widget.receipt['customerName'] ?? '—'}',
              style: TextStyle(color: c.text),
            ),
            const SizedBox(height: 4),
            Text(
              'Tổng tiền: ${Fmt.money(widget.receipt['totalAmount'])}  ·  Còn lại: ${Fmt.money(_remain)}',
              style: TextStyle(color: c.muted),
            ),
            const SizedBox(height: 16),
            AppTextField(
              label: 'Số tiền nhận',
              required: true,
              controller: _amount,
              keyboardType: TextInputType.number,
              inputFormatters: digitsOnly,
              suffix: TextButton(
                onPressed: () => _amount.text = '${_remain.toInt()}',
                child: const Text('Tất toán'),
              ),
            ),
            const SizedBox(height: 12),
            AppDropdown<String>(
              label: 'Hình thức',
              value: _method,
              items: const [('CASH', 'Tiền mặt'), ('TRANSFER', 'Chuyển khoản')],
              onChanged: (v) => setState(() => _method = v ?? 'CASH'),
            ),
            const SizedBox(height: 12),
            AppTextField(label: 'Ghi chú', controller: _note),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context, false),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: _saving ? null : _submit,
          child: Text(_saving ? 'Đang lưu...' : 'Ghi nhận'),
        ),
      ],
    );
  }
}
