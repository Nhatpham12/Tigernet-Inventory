import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import '../form/unsaved_changes.dart';

class Crumb {
  const Crumb(this.label, [this.path]);
  final String label;
  final String? path;
}

/// Breadcrumb + tiêu đề trang (Heading 1) + mô tả + nút thao tác bên phải.
class PageHeader extends StatelessWidget {
  const PageHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.crumbs = const [],
    this.actions = const [],
  });

  final String title;
  final String? subtitle;
  final List<Crumb> crumbs;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final crumbStyle = context.text.caption.copyWith(color: c.muted);
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpace.s24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (crumbs.isNotEmpty)
            Wrap(
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                for (var i = 0; i < crumbs.length; i++) ...[
                  if (i > 0) Text('  /  ', style: crumbStyle),
                  // Mục cuối = trang hiện tại: chữ đậm, không bấm được.
                  i == crumbs.length - 1
                      ? Text(
                          crumbs[i].label,
                          style: crumbStyle.copyWith(
                            color: c.text,
                            fontWeight: FontWeight.w600,
                          ),
                        )
                      : InkWell(
                          onTap: crumbs[i].path == null
                              ? null
                              : () => guardedGo(context, crumbs[i].path!),
                          child: Text(
                            crumbs[i].label,
                            style: crumbs[i].path == null
                                ? crumbStyle
                                : crumbStyle.copyWith(
                                    decoration: TextDecoration.underline,
                                    decorationColor: c.muted,
                                  ),
                          ),
                        ),
                ],
              ],
            ),
          const SizedBox(height: AppSpace.s8),
          SizedBox(
            width: double.infinity,
            child: Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              runSpacing: AppSpace.s16,
              spacing: AppSpace.s24,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(title, style: context.text.h1),
                    if (subtitle != null) ...[
                      const SizedBox(height: AppSpace.s4),
                      Text(
                        subtitle!,
                        style: context.text.body1.copyWith(color: c.muted),
                      ),
                    ],
                  ],
                ),
                if (actions.isNotEmpty)
                  Wrap(
                    spacing: AppSpace.s8,
                    runSpacing: AppSpace.s8,
                    children: actions,
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
