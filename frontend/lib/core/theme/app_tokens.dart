import 'package:flutter/material.dart';

import 'app_colors.dart';

/// Thang khoảng cách duy nhất của app (HCEM: 1/2/4/8/16/20/24/32px).
/// Trong nhóm dùng [s8], giữa các nhóm dùng [s16], giữa các section lớn dùng [s32].
class AppSpace {
  const AppSpace._();

  static const double s2 = 2;
  static const double s4 = 4;
  static const double s8 = 8;
  static const double s16 = 16;
  static const double s20 = 20;
  static const double s24 = 24;
  static const double s32 = 32;

  /// Vùng bấm tối thiểu cho nút icon, mục menu.
  static const double touch = 44;
}

/// 10 Text Style đặt tên, ánh xạ vào [TextTheme] của Material:
///
/// | Tên         | Size/Weight | TextTheme      | Dùng cho                         |
/// |-------------|-------------|----------------|----------------------------------|
/// | Display     | 32 / 800    | displaySmall   | Khẩu hiệu màn đăng nhập          |
/// | Heading 1   | 28 / 800    | headlineMedium | Tiêu đề trang                    |
/// | KPI         | 24 / 800    | headlineSmall  | Số liệu thẻ KPI                  |
/// | Heading 2   | 20 / 700    | titleLarge     | Tiêu đề khối (SectionCard)       |
/// | Heading 3   | 16 / 700    | titleMedium    | Tiêu đề thẻ, menu chính          |
/// | Body Large  | 16 / 400    | bodyLarge      | Đoạn văn dài                     |
/// | Label       | 14 / 600    | labelLarge     | Nhãn field, nút, tiêu đề cột     |
/// | Body 1      | 14 / 400    | bodyMedium     | Nội dung bảng, input             |
/// | Body 2      | 13 / 400    | bodySmall      | Mô tả phụ, menu con              |
/// | Caption     | 12 / 400    | labelSmall     | Breadcrumb, chip, chú thích      |
class AppText {
  const AppText._();

  static const _family = 'OpenSans';

  static TextStyle _s(double size, FontWeight weight, double height, Color c) =>
      TextStyle(
        fontFamily: _family,
        fontSize: size,
        fontWeight: weight,
        height: height,
        color: c,
        letterSpacing: 0,
      );

  static TextTheme theme(AppColors c) => TextTheme(
    displaySmall: _s(32, FontWeight.w800, 1.25, c.text),
    headlineMedium: _s(28, FontWeight.w800, 1.3, c.text),
    headlineSmall: _s(24, FontWeight.w800, 1.2, c.text),
    titleLarge: _s(20, FontWeight.w700, 1.4, c.text),
    titleMedium: _s(16, FontWeight.w700, 1.5, c.text),
    bodyLarge: _s(16, FontWeight.w400, 1.5, c.text),
    labelLarge: _s(14, FontWeight.w600, 1.4, c.text),
    bodyMedium: _s(14, FontWeight.w400, 1.5, c.text),
    bodySmall: _s(13, FontWeight.w400, 1.5, c.text),
    labelSmall: _s(12, FontWeight.w400, 1.4, c.text),
  );
}

/// Gọi Text Style theo tên thiết kế: `context.text.h2`, `context.text.caption`...
class AppTextStyles {
  const AppTextStyles(this._t);

  final TextTheme _t;

  TextStyle get display => _t.displaySmall!;
  TextStyle get h1 => _t.headlineMedium!;
  TextStyle get kpi => _t.headlineSmall!;
  TextStyle get h2 => _t.titleLarge!;
  TextStyle get h3 => _t.titleMedium!;
  TextStyle get bodyLarge => _t.bodyLarge!;
  TextStyle get label => _t.labelLarge!;
  TextStyle get body1 => _t.bodyMedium!;
  TextStyle get body2 => _t.bodySmall!;
  TextStyle get caption => _t.labelSmall!;
}

extension AppTextX on BuildContext {
  AppTextStyles get text => AppTextStyles(Theme.of(this).textTheme);
}
