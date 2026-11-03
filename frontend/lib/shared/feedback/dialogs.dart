import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

Future<bool> confirmDialog(
  BuildContext context, {
  required String title,
  required String message,
  String confirmLabel = 'Xác nhận',
  bool danger = false,
}) async {
  final c = context.colors;
  final ok = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(title),
      content: Text(message),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          style: danger
              ? ElevatedButton.styleFrom(
                  backgroundColor: c.danger,
                  foregroundColor: Colors.white,
                )
              : null,
          onPressed: () => Navigator.pop(context, true),
          child: Text(confirmLabel),
        ),
      ],
    ),
  );
  return ok ?? false;
}

/// Hộp thoại form nhiều trường chữ; trả về giá trị theo key hoặc null nếu hủy.
Future<Map<String, String>?> formDialog(
  BuildContext context, {
  required String title,
  required List<(String key, String label, String initial, bool required)>
  fields,
  String confirmLabel = 'Lưu',
}) {
  final controllers = {
    for (final f in fields) f.$1: TextEditingController(text: f.$3),
  };
  final formKey = GlobalKey<FormState>();
  return showDialog<Map<String, String>>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(title),
      content: SizedBox(
        width: 420,
        child: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (final (key, label, _, required) in fields) ...[
                TextFormField(
                  controller: controllers[key],
                  decoration: InputDecoration(
                    labelText: required ? '$label *' : label,
                  ),
                  validator: required
                      ? (v) => (v == null || v.trim().isEmpty)
                            ? 'Vui lòng nhập $label'
                            : null
                      : null,
                ),
                const SizedBox(height: 12),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: () {
            if (formKey.currentState!.validate()) {
              Navigator.pop(context, {
                for (final e in controllers.entries) e.key: e.value.text.trim(),
              });
            }
          },
          child: Text(confirmLabel),
        ),
      ],
    ),
  );
}
