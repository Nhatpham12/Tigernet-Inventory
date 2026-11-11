import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../feedback/dialogs.dart';

/// Cờ "form đang nhập dở" dùng chung cho cả app.
/// Form gọi [mark] khi người dùng sửa, [clear] khi lưu xong hoặc rời trang.
class UnsavedChanges {
  const UnsavedChanges._();

  static final ValueNotifier<bool> dirty = ValueNotifier(false);

  static void mark() {
    if (!dirty.value) dirty.value = true;
  }

  static void clear() => dirty.value = false;

  /// true = được phép rời trang (không có thay đổi, hoặc người dùng chọn bỏ).
  static Future<bool> confirmLeave(BuildContext context) async {
    if (!dirty.value) return true;
    final ok = await confirmDialog(
      context,
      title: 'Bỏ thay đổi?',
      message:
          'Thông tin bạn đang nhập chưa được lưu. Rời trang sẽ mất các thay đổi này.',
      confirmLabel: 'Bỏ thay đổi',
      danger: true,
    );
    if (ok) clear();
    return ok;
  }
}

/// `context.go` có hỏi xác nhận khi form đang nhập dở.
Future<void> guardedGo(BuildContext context, String location) async {
  if (await UnsavedChanges.confirmLeave(context) && context.mounted) {
    context.go(location);
  }
}
