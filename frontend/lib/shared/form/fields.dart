import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import '../../core/utils/formatters.dart';

/// Nhãn đặt trên, input cao 44px (Figma "List UI/Input").
class LabeledField extends StatelessWidget {
  const LabeledField({
    super.key,
    required this.label,
    required this.child,
    this.required = false,
  });

  final String label;
  final bool required;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(required ? '$label *' : label, style: context.text.label),
        const SizedBox(height: AppSpace.s8),
        child,
      ],
    );
  }
}

class AppTextField extends StatelessWidget {
  const AppTextField({
    super.key,
    required this.label,
    this.controller,
    this.initialValue,
    this.hint,
    this.required = false,
    this.readOnly = false,
    this.obscure = false,
    this.keyboardType,
    this.inputFormatters,
    this.validator,
    this.suffix,
    this.onChanged,
    this.onSubmitted,
    this.maxLines = 1,
    this.autofillHints,
  });

  final String label;
  final TextEditingController? controller;

  /// Giá trị cố định cho ô chỉ đọc (không cần tạo controller trong build).
  final String? initialValue;
  final String? hint;
  final bool required;
  final bool readOnly;
  final bool obscure;
  final TextInputType? keyboardType;
  final List<TextInputFormatter>? inputFormatters;
  final String? Function(String?)? validator;
  final Widget? suffix;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final int maxLines;
  final Iterable<String>? autofillHints;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return LabeledField(
      label: label,
      required: required,
      child: TextFormField(
        controller: controller,
        initialValue: controller == null ? initialValue : null,
        readOnly: readOnly,
        obscureText: obscure,
        keyboardType: keyboardType,
        inputFormatters: inputFormatters,
        maxLines: maxLines,
        autofillHints: autofillHints,
        onChanged: onChanged,
        onFieldSubmitted: onSubmitted,
        style: context.text.body1,
        validator:
            validator ??
            (required
                ? (v) => (v == null || v.trim().isEmpty)
                      ? 'Vui lòng nhập $label'
                      : null
                : null),
        decoration: InputDecoration(
          hintText: hint,
          suffixIcon: suffix,
          fillColor: readOnly ? c.surfaceMuted : c.surface,
        ),
      ),
    );
  }
}

/// Dropdown có nhãn trên; [items] là cặp (giá trị, nhãn).
class AppDropdown<T> extends StatelessWidget {
  const AppDropdown({
    super.key,
    required this.label,
    required this.items,
    required this.value,
    required this.onChanged,
    this.required = false,
    this.hint,
  });

  final String label;
  final List<(T, String)> items;
  final T? value;
  final ValueChanged<T?> onChanged;
  final bool required;
  final String? hint;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final hasValue = items.any((e) => e.$1 == value);
    return LabeledField(
      label: label,
      required: required,
      child: DropdownButtonFormField<T>(
        initialValue: hasValue ? value : null,
        isExpanded: true,
        hint: hint == null
            ? null
            : Text(
                hint!,
                style: context.text.body1.copyWith(color: c.muted),
              ),
        style: context.text.body1,
        icon: Icon(Icons.keyboard_arrow_down, color: c.muted),
        validator: required
            ? (v) => v == null ? 'Vui lòng chọn $label' : null
            : null,
        items: [
          for (final (v, text) in items)
            DropdownMenuItem<T>(
              value: v,
              child: Text(text, overflow: TextOverflow.ellipsis),
            ),
        ],
        onChanged: onChanged,
      ),
    );
  }
}

class DateField extends StatelessWidget {
  const DateField({
    super.key,
    required this.label,
    required this.value,
    required this.onChanged,
    this.required = false,
  });

  final String label;
  final DateTime? value;
  final ValueChanged<DateTime?> onChanged;
  final bool required;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return LabeledField(
      label: label,
      required: required,
      child: FormField<DateTime>(
        initialValue: value,
        validator: required
            ? (_) => value == null ? 'Vui lòng chọn $label' : null
            : null,
        builder: (state) => InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: () async {
            final picked = await showDatePicker(
              context: context,
              initialDate: value ?? DateTime.now(),
              firstDate: DateTime(2020),
              lastDate: DateTime(2100),
            );
            if (picked != null) {
              onChanged(picked);
              state.didChange(picked);
            }
          },
          child: InputDecorator(
            decoration: InputDecoration(
              errorText: state.errorText,
              suffixIcon: Icon(
                Icons.calendar_today_outlined,
                size: 18,
                color: c.muted,
              ),
            ),
            child: Text(
              value == null ? 'dd/mm/yyyy' : Fmt.date(value),
              style: context.text.body1.copyWith(
                color: value == null ? c.muted : c.text,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Chỉ cho nhập chữ số (giá tiền, số lượng).
final digitsOnly = <TextInputFormatter>[FilteringTextInputFormatter.digitsOnly];

num? parseNumber(String? text) => text == null || text.trim().isEmpty
    ? null
    : num.tryParse(text.replaceAll('.', '').trim());
