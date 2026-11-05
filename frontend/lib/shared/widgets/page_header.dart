import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_colors.dart';

class Crumb {
  const Crumb(this.label, [this.path]);
  final String label;
  final String? path;
}

/// Breadcrumb + tiêu đề trang (28px, đậm) + mô tả + nút thao tác bên phải.
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
    final crumbStyle = TextStyle(fontSize: 12, color: c.muted);
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (crumbs.isNotEmpty)
            Wrap(
              children: [
                for (var i = 0; i < crumbs.length; i++) ...[
                  if (i > 0) Text('  /  ', style: crumbStyle),
                  InkWell(
                    onTap: crumbs[i].path == null
                        ? null
                        : () => context.go(crumbs[i].path!),
                    child: Text(crumbs[i].label, style: crumbStyle),
                  ),
                ],
              ],
            ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              runSpacing: 12,
              spacing: 24,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        color: c.text,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        subtitle!,
                        style: TextStyle(fontSize: 14, color: c.muted),
                      ),
                    ],
                  ],
                ),
                if (actions.isNotEmpty)
                  Wrap(spacing: 12, runSpacing: 8, children: actions),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
