import 'package:flutter/material.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';

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
      _show(context, userMessage(error), Icons.error_outline, context.colors.danger);

  static void info(BuildContext context, String message) =>
      _show(context, message, Icons.info_outline, context.colors.info);

  static void _show(
    BuildContext context,
    String message,
    IconData icon,
    Color color,
  ) {
    final c = context.colors;
    // Không vượt quá bề rộng màn: SnackBar floating rộng hơn màn sẽ lỗi trên điện thoại.
    final screen = MediaQuery.sizeOf(context).width;
    final width = (screen - AppSpace.s32).clamp(0.0, 420.0);
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          width: width,
          backgroundColor: c.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: BorderSide(color: color),
          ),
          content: Row(
            children: [
              Icon(icon, color: color),
              const SizedBox(width: AppSpace.s8),
              Expanded(
                child: Text(
                  message,
                  style: context.text.body1.copyWith(color: c.text),
                ),
              ),
            ],
          ),
        ),
      );
  }
}
