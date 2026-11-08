import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/section_card.dart';

/// Chọn ngày + giờ (form kiểm kê, đổi trả, trả hàng thuê).
class DateTimeField extends StatelessWidget {
  const DateTimeField({
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
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: () async {
          final day = await showDatePicker(
            context: context,
            initialDate: value ?? DateTime.now(),
            firstDate: DateTime(2020),
            lastDate: DateTime(2100),
          );
          if (day == null || !context.mounted) return;
          final time = await showTimePicker(
            context: context,
            initialTime: TimeOfDay.fromDateTime(value ?? DateTime.now()),
          );
          if (time == null) return;
          onChanged(
            DateTime(day.year, day.month, day.day, time.hour, time.minute),
          );
        },
        child: InputDecorator(
          decoration: InputDecoration(
            suffixIcon: Icon(Icons.event_outlined, size: 18, color: c.muted),
          ),
          child: Text(
            value == null ? 'dd/mm/yyyy hh:mm' : Fmt.dateTime(value),
            style: TextStyle(
              fontSize: 14,
              color: value == null ? c.muted : c.text,
            ),
          ),
        ),
      ),
    );
  }
}

/// Lưới "nhãn: giá trị" cho khối thông tin ở màn chi tiết.
class InfoGrid extends StatelessWidget {
  const InfoGrid({super.key, required this.items});

  final List<(String label, Widget value)> items;

  static Widget text(BuildContext context, String? value) => Text(
    value == null || value.isEmpty ? '—' : value,
    style: TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.w600,
      color: context.colors.text,
    ),
  );

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return FormGrid(
      children: [
        for (final (label, value) in items)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(fontSize: 12, color: c.muted)),
              const SizedBox(height: 4),
              value,
            ],
          ),
      ],
    );
  }
}

/// Bảng dòng hàng đơn giản (chi tiết phiếu): header xám, mỗi ô là widget.
class DocTable extends StatelessWidget {
  const DocTable({
    super.key,
    required this.headers,
    required this.rows,
    this.flex,
    this.rightAligned = const {},
    this.minWidth = 640,
  });

  final List<String> headers;
  final List<List<Widget>> rows;
  final List<int>? flex;
  final Set<int> rightAligned;
  final double minWidth;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    Widget cell(int i, Widget child) => Expanded(
      flex: flex?[i] ?? 1,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 8),
        child: Align(
          alignment: rightAligned.contains(i)
              ? Alignment.centerRight
              : Alignment.centerLeft,
          child: child,
        ),
      ),
    );
    final table = Column(
      children: [
        Container(
          color: c.surfaceMuted,
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: Row(
            children: [
              for (var i = 0; i < headers.length; i++)
                cell(
                  i,
                  Text(
                    headers[i],
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                      color: c.text,
                    ),
                  ),
                ),
            ],
          ),
        ),
        for (final row in rows)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: c.divider)),
            ),
            child: Row(
              children: [for (var i = 0; i < row.length; i++) cell(i, row[i])],
            ),
          ),
      ],
    );
    return LayoutBuilder(
      builder: (context, constraints) => constraints.maxWidth < minWidth
          ? SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: SizedBox(width: minWidth, child: table),
            )
          : table,
    );
  }
}

Widget docText(BuildContext context, Object? value, {bool bold = false}) =>
    Text(
      '${value ?? '—'}',
      style: TextStyle(
        fontSize: 13,
        fontWeight: bold ? FontWeight.w600 : FontWeight.w400,
        color: context.colors.text,
      ),
    );

/// Danh sách không phân trang (API chưa có view BFF): tìm kiếm tại máy.
class ClientTable extends StatefulWidget {
  const ClientTable({
    super.key,
    required this.rows,
    required this.columns,
    this.onRowTap,
    this.searchHint = 'Tìm kiếm...',
  });

  final List<RowData> rows;
  final List<TableColumnDef> columns;
  final ValueChanged<RowData>? onRowTap;
  final String searchHint;

  @override
  State<ClientTable> createState() => _ClientTableState();
}

class _ClientTableState extends State<ClientTable> {
  String _kw = '';

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final kw = _kw.toLowerCase();
    final rows = kw.isEmpty
        ? widget.rows
        : widget.rows
              .where(
                (r) => r.values.any(
                  (v) => v is String && v.toLowerCase().contains(kw),
                ),
              )
              .toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 360,
          child: TextField(
            onChanged: (v) => setState(() => _kw = v),
            decoration: InputDecoration(
              hintText: widget.searchHint,
              prefixIcon: Icon(Icons.search, color: c.muted),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Container(
          decoration: BoxDecoration(
            color: c.surface,
            border: Border.all(color: c.divider),
            borderRadius: BorderRadius.circular(8),
          ),
          child: rows.isEmpty
              ? const Padding(
                  padding: EdgeInsets.all(32),
                  child: Center(child: Text('Chưa có dữ liệu')),
                )
              : AppDataTable(
                  columns: widget.columns,
                  rows: rows,
                  onRowTap: widget.onRowTap,
                ),
        ),
        const SizedBox(height: 8),
        Text(
          '${rows.length} phiếu',
          style: TextStyle(fontSize: 12, color: c.muted),
        ),
      ],
    );
  }
}

/// Khối ghi chú nhỏ (xám) dưới form.
class HintText extends StatelessWidget {
  const HintText(this.text, {super.key});
  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 8),
    child: Text(
      text,
      style: TextStyle(fontSize: 12, color: context.colors.muted),
    ),
  );
}

/// Thẻ "Thông tin" dùng lại giữa các màn chi tiết.
Widget infoCard(String title, List<(String, Widget)> items) => SectionCard(
  title: title,
  child: InfoGrid(items: items),
);
