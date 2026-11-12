import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';

/// Khung trắng viền xám bo 8px, tiêu đề Heading 2 (Figma: "Thông tin phiếu", "Danh sách hàng hóa").
class SectionCard extends StatelessWidget {
  const SectionCard({
    super.key,
    this.title,
    this.trailing,
    required this.child,
    this.padding,
  });

  final String? title;
  final Widget? trailing;
  final Widget child;
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Container(
      width: double.infinity,
      padding: padding ?? const EdgeInsets.all(AppSpace.s20),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border.all(color: c.divider),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title != null) ...[
            Row(
              children: [
                Expanded(
                  child: Text(title!, style: context.text.h2),
                ),
                ?trailing,
              ],
            ),
            const SizedBox(height: AppSpace.s16),
          ],
          child,
        ],
      ),
    );
  }
}

/// Lưới form 3 cột (desktop) → 2 → 1 cột theo bề rộng (Figma Flow §04).
class FormGrid extends StatelessWidget {
  const FormGrid({super.key, required this.children, this.maxColumns = 3});

  final List<Widget> children;
  final int maxColumns;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final w = constraints.maxWidth;
        final cols = w >= 900
            ? maxColumns
            : (w >= 560 ? 2.clamp(1, maxColumns) : 1);
        const gap = AppSpace.s16;
        final itemWidth = (w - gap * (cols - 1)) / cols;
        return Wrap(
          spacing: gap,
          runSpacing: AppSpace.s16,
          children: [
            for (final child in children)
              SizedBox(width: itemWidth, child: child),
          ],
        );
      },
    );
  }
}
