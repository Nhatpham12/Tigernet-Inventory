import 'package:flutter/material.dart';

/// Token màu lấy từ Figma "TIGERNET INVENTORY" (biến --ux-*), có bản Light/Dark.
@immutable
class AppColors extends ThemeExtension<AppColors> {
  const AppColors({
    required this.page,
    required this.surface,
    required this.surfaceMuted,
    required this.text,
    required this.muted,
    required this.border,
    required this.divider,
    required this.brand,
    required this.onBrand,
    required this.accent,
    required this.navActive,
    required this.navActiveText,
    required this.link,
    required this.success,
    required this.successBg,
    required this.warning,
    required this.warningBg,
    required this.danger,
    required this.dangerBg,
    required this.info,
    required this.infoBg,
    required this.purple,
    required this.purpleBg,
    required this.rowSelected,
  });

  final Color page;
  final Color surface;
  final Color surfaceMuted; // nền header bảng, ô chỉ đọc
  final Color text;
  final Color muted;
  final Color border;
  final Color divider;
  final Color brand; // vàng thương hiệu – nút chính
  final Color onBrand;
  final Color accent; // cam logo, vạch chọn menu, nút giao diện
  final Color navActive;
  final Color navActiveText;
  final Color link;
  final Color success;
  final Color successBg;
  final Color warning;
  final Color warningBg;
  final Color danger;
  final Color dangerBg;
  final Color info;
  final Color infoBg;
  final Color purple;
  final Color purpleBg;
  final Color rowSelected;

  static const light = AppColors(
    page: Color(0xFFFFFFFF),
    surface: Color(0xFFFFFFFF),
    surfaceMuted: Color(0xFFF2F4F7),
    text: Color(0xFF1F2937),
    muted: Color(0xFF596579),
    border: Color(0xFFCBD2DC),
    divider: Color(0xFFE1E7F0),
    brand: Color(0xFFFFC107),
    onBrand: Color(0xFF1F2937),
    accent: Color(0xFFF4511E),
    navActive: Color(0xFFFFF1B5),
    navActiveText: Color(0xFF17233B),
    link: Color(0xFF21385E),
    success: Color(0xFF1E7B3A),
    successBg: Color(0xFFE3F5E8),
    warning: Color(0xFFB45309),
    warningBg: Color(0xFFFFF4D6),
    danger: Color(0xFFC62828),
    dangerBg: Color(0xFFFDE2E4),
    info: Color(0xFF1D5FD1),
    infoBg: Color(0xFFE3EEFF),
    purple: Color(0xFF7C3AED),
    purpleBg: Color(0xFFF1EAFE),
    rowSelected: Color(0xFFFFE9A8),
  );

  static const dark = AppColors(
    page: Color(0xFF0F1623),
    surface: Color(0xFF172131),
    surfaceMuted: Color(0xFF1F2B3D),
    text: Color(0xFFE8EDF5),
    muted: Color(0xFFA3AEC0),
    border: Color(0xFF34435A),
    divider: Color(0xFF2A374B),
    brand: Color(0xFFFFC107),
    onBrand: Color(0xFF1F2937),
    accent: Color(0xFFFF7043),
    navActive: Color(0xFF3B3216),
    navActiveText: Color(0xFFFFE08A),
    link: Color(0xFF9DB8E8),
    success: Color(0xFF6FD08E),
    successBg: Color(0xFF173A26),
    warning: Color(0xFFF5B95A),
    warningBg: Color(0xFF3D2E12),
    danger: Color(0xFFFF8A8A),
    dangerBg: Color(0xFF452024),
    info: Color(0xFF8AB4FF),
    infoBg: Color(0xFF1B2D4D),
    purple: Color(0xFFC4A5FF),
    purpleBg: Color(0xFF2E2347),
    rowSelected: Color(0xFF3B3216),
  );

  @override
  AppColors copyWith() => this;

  @override
  AppColors lerp(ThemeExtension<AppColors>? other, double t) =>
      t < 0.5 ? this : (other as AppColors? ?? this);
}

extension AppColorsX on BuildContext {
  AppColors get colors => Theme.of(this).extension<AppColors>()!;
}
