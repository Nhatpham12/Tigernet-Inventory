import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import 'unsaved_changes.dart';

/// Khung trang form: nội dung cuộn riêng, thanh [footer] luôn dính đáy màn
/// (Figma: Lưu/Hủy cố định cuối màn). Tự đánh dấu "đang nhập dở" khi người dùng sửa field.
///
/// AppShell không bọc các route form ([handles]) trong vùng cuộn chung,
/// nên trang form phải dùng widget này.
class FormPageLayout extends StatefulWidget {
  const FormPageLayout({
    super.key,
    required this.formKey,
    required this.children,
    required this.footer,
  });

  final GlobalKey<FormState> formKey;
  final List<Widget> children;
  final Widget footer;

  /// Route tạo/sửa dùng khung form tự cuộn.
  static bool handles(String location) =>
      location.endsWith('/new') || location.endsWith('/edit');

  /// Lề ngang nội dung, khớp với AppShell.
  static double horizontalPadding(BuildContext context) =>
      MediaQuery.sizeOf(context).width < 900 ? AppSpace.s16 : AppSpace.s24;

  @override
  State<FormPageLayout> createState() => _FormPageLayoutState();
}

class _FormPageLayoutState extends State<FormPageLayout> {
  @override
  void initState() {
    super.initState();
    UnsavedChanges.clear();
  }

  @override
  void dispose() {
    UnsavedChanges.clear();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final h = FormPageLayout.horizontalPadding(context);
    return Form(
      key: widget.formKey,
      onChanged: UnsavedChanges.mark,
      child: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: EdgeInsets.fromLTRB(h, AppSpace.s16, h, AppSpace.s24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: widget.children,
              ),
            ),
          ),
          widget.footer,
        ],
      ),
    );
  }
}

/// Thanh Lưu/Hủy của phiếu, dính đáy màn trong [FormPageLayout].
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
    final h = FormPageLayout.horizontalPadding(context);
    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(horizontal: h, vertical: AppSpace.s16),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border(top: BorderSide(color: c.divider)),
      ),
      child: Wrap(
        alignment: WrapAlignment.end,
        spacing: AppSpace.s8,
        runSpacing: AppSpace.s8,
        children: [
          TextButton(
            onPressed: saving ? null : onCancel,
            child: const Text('Hủy'),
          ),
          if (onSaveDraft != null)
            OutlinedButton(
              onPressed: saving ? null : onSaveDraft,
              child: const Text('Lưu nháp'),
            ),
          ConstrainedBox(
            constraints: const BoxConstraints(minWidth: 140),
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
