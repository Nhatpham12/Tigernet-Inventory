import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

/// Thông báo góc dưới (NOTE UXUI #14).
class Toast {
  const Toast._();

  static void success(BuildContext context, String message) => _show(
    context,
    message,
    Icons.check_circle_outline,
    context.colors.success,
  );

  static void error(BuildContext context, Object error) =>
      _show(context, '$error', Icons.error_outline, context.colors.danger);

  static void info(BuildContext context, String message) =>
      _show(context, message, Icons.info_outline, context.colors.info);

  static void _show(
    BuildContext context,
    String message,
    IconData icon,
    Color color,
  ) {
    final c = context.colors;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          width: 420,
          backgroundColor: c.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: BorderSide(color: color),
          ),
          content: Row(
            children: [
              Icon(icon, color: color),
              const SizedBox(width: 12),
              Expanded(
                child: Text(message, style: TextStyle(color: c.text)),
              ),
            ],
          ),
        ),
      );
  }
}
