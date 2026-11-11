import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_theme.dart';

enum BrandLogoSize { compact, regular, large }

/// Logo chữ "TIGERNET / INVENTORY" dùng chung cho sidebar, header mobile và màn đăng nhập
/// (một nhận diện duy nhất: font Audiowide, màu token brandInk).
class BrandLogo extends StatelessWidget {
  const BrandLogo({super.key, this.size = BrandLogoSize.regular, this.color});

  final BrandLogoSize size;

  /// Ghi đè màu khi logo nằm trên nền thương hiệu cố định (khối vàng màn đăng nhập).
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final ink = color ?? context.colors.brandInk;
    TextStyle style(double s) => TextStyle(
      fontFamily: AppTheme.brandFontFamily,
      fontSize: s,
      height: 1.1,
      color: ink,
    );
    return Semantics(
      label: 'TIGERNET Inventory',
      excludeSemantics: true,
      child: switch (size) {
        BrandLogoSize.compact => Text('TIGERNET', style: style(16)),
        BrandLogoSize.regular => Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('TIGERNET', style: style(16)),
            Text('INVENTORY', style: style(20)),
          ],
        ),
        BrandLogoSize.large => IntrinsicWidth(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('TIGERNET', style: style(32)),
              Text('INVENTORY', style: style(48)),
            ],
          ),
        ),
      },
    );
  }
}
