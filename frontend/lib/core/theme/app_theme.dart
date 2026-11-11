import 'package:flutter/material.dart';

import 'app_colors.dart';
import 'app_tokens.dart';

/// ThemeData theo Figma: Open Sans, input cao 44px bo 8px, nút 40px.
class AppTheme {
  const AppTheme._();

  static const radius = 8.0;
  static const fontFamily = 'OpenSans';
  static const brandFontFamily = 'Audiowide';

  static ThemeData light() => _build(AppColors.light, Brightness.light);

  static ThemeData dark() => _build(AppColors.dark, Brightness.dark);

  static ThemeData _build(AppColors c, Brightness brightness) {
    final base = ThemeData(
      brightness: brightness,
      useMaterial3: true,
      fontFamily: fontFamily,
    );
    // Text Styles đặt tên (AppText) đè lên bộ mặc định của Material.
    final textTheme = base.textTheme
        .apply(bodyColor: c.text, displayColor: c.text)
        .merge(AppText.theme(c));
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(radius),
      borderSide: BorderSide(color: c.muted),
    );
    return base.copyWith(
      colorScheme: ColorScheme.fromSeed(
        seedColor: c.brand,
        brightness: brightness,
        primary: c.brand,
        onPrimary: c.onBrand,
        secondary: c.accent,
        surface: c.surface,
        onSurface: c.text,
        error: c.danger,
      ),
      // giữ kích thước Figma (nút 40px, input 44px) trên cả desktop/web
      visualDensity: VisualDensity.standard,
      materialTapTargetSize: MaterialTapTargetSize.padded,
      scaffoldBackgroundColor: c.page,
      canvasColor: c.surface,
      dividerColor: c.divider,
      textTheme: textTheme,
      extensions: [c],
      inputDecorationTheme: InputDecorationTheme(
        isDense: true,
        filled: true,
        fillColor: c.surface,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 12, // chữ 14px × 1,5 + 2×12 ≈ 44px: chiều cao input theo Figma
        ),
        hintStyle: TextStyle(color: c.muted, fontSize: 14),
        border: border,
        enabledBorder: border,
        focusedBorder: border.copyWith(
          borderSide: BorderSide(color: c.accent, width: 1.5),
        ),
        errorBorder: border.copyWith(borderSide: BorderSide(color: c.danger)),
        disabledBorder: border.copyWith(
          borderSide: BorderSide(color: c.border),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: c.brand,
          foregroundColor: c.onBrand,
          elevation: 0,
          minimumSize: const Size(92, 40),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          textStyle: const TextStyle(
            fontFamily: fontFamily,
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radius),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: c.text,
          minimumSize: const Size(92, 40),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          side: BorderSide(color: c.muted),
          textStyle: const TextStyle(
            fontFamily: fontFamily,
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radius),
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: c.text,
          minimumSize: const Size(64, 40),
          textStyle: const TextStyle(
            fontFamily: fontFamily,
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radius),
          ),
        ),
      ),
      checkboxTheme: CheckboxThemeData(
        side: BorderSide(color: c.muted, width: 1.5),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5)),
        fillColor: WidgetStateProperty.resolveWith(
          (s) =>
              s.contains(WidgetState.selected) ? c.accent : Colors.transparent,
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: c.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radius),
        ),
      ),
      snackBarTheme: const SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
      ),
      tooltipTheme: const TooltipThemeData(
        waitDuration: Duration(milliseconds: 400),
      ),
    );
  }
}
