import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// Footer Lưu/Hủy của phiếu (Figma: cố định cuối màn).
class FormFooter extends StatelessWidget {
  const FormFooter({
    super.key,
    required this.onCancel,
    required this.onSave,
    this.saveLabel = 'Lưu phiếu',
    this.onSaveDraft,
    this.saving = false,
  });

  final VoidCallback onCancel;
  final VoidCallback onSave;
  final VoidCallback? onSaveDraft;
  final String saveLabel;
  final bool saving;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Container(
      margin: const EdgeInsets.only(top: 16),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border.all(color: c.divider),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          TextButton(
            onPressed: saving ? null : onCancel,
            child: const Text('Hủy'),
          ),
          if (onSaveDraft != null) ...[
            const SizedBox(width: 12),
            SizedBox(
              width: 112,
              child: OutlinedButton(
                onPressed: saving ? null : onSaveDraft,
                child: const Text('Lưu nháp'),
              ),
            ),
          ],
          const SizedBox(width: 12),
          SizedBox(
            width: 140,
            child: ElevatedButton(
              onPressed: saving ? null : onSave,
              child: Text(saving ? 'Đang lưu...' : saveLabel),
            ),
          ),
        ],
      ),
    );
  }
}
