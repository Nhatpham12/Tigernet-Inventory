import 'package:intl/intl.dart';

/// Định dạng hiển thị theo Figma: "1.250.000 ₫", "07/10/2026 09:15".
class Fmt {
  const Fmt._();

  static final _money = NumberFormat.decimalPattern('vi_VN');
  static final _date = DateFormat('dd/MM/yyyy');
  static final _dateTime = DateFormat('dd/MM/yyyy HH:mm');

  static num? toNum(Object? value) => switch (value) {
    null => null,
    num n => n,
    String s => num.tryParse(s),
    _ => null,
  };

  static String money(Object? value) {
    final n = toNum(value);
    return n == null ? '—' : '${_money.format(n)} ₫';
  }

  static String number(Object? value) {
    final n = toNum(value);
    return n == null ? '—' : _money.format(n);
  }

  static DateTime? parseDate(Object? value) =>
      value is String && value.isNotEmpty ? DateTime.tryParse(value) : null;

  static String date(Object? value) {
    final d = value is DateTime ? value : parseDate(value);
    return d == null ? '—' : _date.format(d);
  }

  static String dateTime(Object? value) {
    final d = value is DateTime ? value : parseDate(value);
    return d == null ? '—' : _dateTime.format(d);
  }

  /// LocalDateTime cho backend: 2026-10-09T00:00:00
  static String? apiDateTime(DateTime? value) =>
      value == null ? null : DateFormat("yyyy-MM-dd'T'HH:mm:ss").format(value);

  /// Mã hiển thị khi backend chưa có SKU (khớp BFF: SP-00124).
  static String productCode(Object? id) =>
      id == null ? '' : 'SP-${id.toString().padLeft(5, '0')}';
}
