import 'package:flutter/gestures.dart' show PointerDeviceKind;
import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

typedef RowData = Map<String, dynamic>;

/// Cột bảng: rộng theo nội dung trong khoảng [minWidth]–[maxWidth]; bảng rộng hơn màn hình
/// thì cuộn ngang (kéo thanh cuộn, Shift + lăn chuột hoặc kéo chuột), còn thừa chỗ thì chia theo [flex].
class TableColumnDef {
  const TableColumnDef({
    required this.label,
    required this.cell,
    this.flex = 2,
    this.minWidth = 100,
    this.maxWidth,
    this.align = Alignment.centerLeft,
    this.sortKey,
  });

  final String label;
  final Widget Function(RowData row) cell;
  final int flex;
  final double minWidth;

  /// null = tự tính (gấp ~2,5 lần [minWidth], tối thiểu 260); chữ dài hơn sẽ xuống dòng.
  final double? maxWidth;
  final Alignment align;
  final String? sortKey;

  double get effectiveMaxWidth =>
      maxWidth ?? (minWidth * 2.5 < 260 ? 260 : minWidth * 2.5);

  /// Cột chữ đơn giản lấy từ một trường.
  factory TableColumnDef.text(
    String label,
    String Function(RowData row) value, {
    int flex = 2,
    double minWidth = 100,
    double? maxWidth,
    Alignment align = Alignment.centerLeft,
    String? sortKey,
    bool bold = false,
  }) => TableColumnDef(
    label: label,
    flex: flex,
    minWidth: minWidth,
    maxWidth: maxWidth,
    align: align,
    sortKey: sortKey,
    cell: (row) => _CellText(value(row), bold: bold, align: align),
  );
}

class _CellText extends StatelessWidget {
  const _CellText(
    this.text, {
    this.bold = false,
    this.align = Alignment.centerLeft,
  });

  final String text;
  final bool bold;
  final Alignment align;

  @override
  Widget build(BuildContext context) => Text(
    text,
    maxLines: 2,
    overflow: TextOverflow.ellipsis,
    textAlign: align == Alignment.centerRight
        ? TextAlign.right
        : (align == Alignment.center ? TextAlign.center : TextAlign.left),
    style: TextStyle(
      fontSize: 14,
      color: context.colors.text,
      fontWeight: bold ? FontWeight.w600 : FontWeight.w400,
    ),
  );
}

/// Bảng dữ liệu dùng chung: header nền xám, kẻ dòng, sort khi bấm tiêu đề (NOTE UXUI #1, #13).
/// Cột co giãn theo nội dung; bảng rộng hơn chỗ hiển thị thì cuộn ngang, luôn hiện thanh cuộn.
class AppDataTable extends StatefulWidget {
  const AppDataTable({
    super.key,
    required this.columns,
    required this.rows,
    this.onRowTap,
    this.sortKey,
    this.sortAsc = true,
    this.onSort,
    this.rowHeight = 56,
    this.selectedIndex,
  });

  final List<TableColumnDef> columns;
  final List<RowData> rows;
  final ValueChanged<RowData>? onRowTap;
  final String? sortKey;
  final bool sortAsc;
  final ValueChanged<String>? onSort;
  final double rowHeight;
  final int? selectedIndex;

  @override
  State<AppDataTable> createState() => _AppDataTableState();
}

class _AppDataTableState extends State<AppDataTable> {
  final _horizontal = ScrollController();

  @override
  void dispose() {
    _horizontal.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final columns = widget.columns;
    final widths = <int, TableColumnWidth>{
      for (var i = 0; i < columns.length; i++)
        i: MaxColumnWidth(
          FixedColumnWidth(columns[i].minWidth),
          MinColumnWidth(
            IntrinsicColumnWidth(flex: columns[i].flex.toDouble()),
            FixedColumnWidth(columns[i].effectiveMaxWidth),
          ),
        ),
    };
    return LayoutBuilder(
      builder: (context, constraints) {
        final table = ConstrainedBox(
          // thừa chỗ thì các cột giãn theo flex cho kín bề ngang
          // trừ 0,5px: tránh sai số làm tròn khiến bảng "tràn" vài phần nghìn px và hiện thanh cuộn thừa
          constraints: BoxConstraints(
            minWidth: (constraints.maxWidth - 0.5).clamp(0, double.infinity),
          ),
          child: Table(
            columnWidths: widths,
            defaultVerticalAlignment: TableCellVerticalAlignment.middle,
            children: [
              TableRow(
                decoration: BoxDecoration(
                  color: c.surfaceMuted,
                  border: Border(bottom: BorderSide(color: c.border)),
                ),
                children: [
                  for (var i = 0; i < columns.length; i++)
                    _header(context, columns[i], i),
                ],
              ),
              for (var i = 0; i < widget.rows.length; i++) _row(context, i),
            ],
          ),
        );
        return Container(
          decoration: BoxDecoration(
            border: Border.all(color: c.border),
            borderRadius: BorderRadius.circular(4),
            color: c.surface,
          ),
          clipBehavior: Clip.antiAlias,
          child: Material(
            color: c.surface,
            child: Scrollbar(
              controller: _horizontal,
              thumbVisibility: true,
              child: ScrollConfiguration(
                // cho phép kéo chuột / bút / cảm ứng để cuộn ngang trên web
                behavior: ScrollConfiguration.of(context).copyWith(
                  dragDevices: {
                    PointerDeviceKind.touch,
                    PointerDeviceKind.mouse,
                    PointerDeviceKind.trackpad,
                    PointerDeviceKind.stylus,
                  },
                  scrollbars: false,
                ),
                child: SingleChildScrollView(
                  controller: _horizontal,
                  scrollDirection: Axis.horizontal,
                  // chừa chỗ cho thanh cuộn ngang ở đáy bảng
                  padding: const EdgeInsets.only(bottom: 10),
                  child: table,
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  EdgeInsets _cellPadding(int index) => EdgeInsets.fromLTRB(
    index == 0 ? 16 : 8,
    8,
    index == widget.columns.length - 1 ? 16 : 8,
    8,
  );

  Widget _header(BuildContext context, TableColumnDef col, int index) {
    final c = context.colors;
    final active = col.sortKey != null && col.sortKey == widget.sortKey;
    final label = Text(
      col.label,
      softWrap: false,
      style: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w700,
        color: c.text,
      ),
    );
    final content = col.sortKey == null || widget.onSort == null
        ? label
        : InkWell(
            onTap: () => widget.onSort!(col.sortKey!),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  label,
                  const SizedBox(width: 4),
                  Icon(
                    active
                        ? (widget.sortAsc
                              ? Icons.arrow_upward
                              : Icons.arrow_downward)
                        : Icons.unfold_more,
                    size: 14,
                    color: active ? c.accent : c.muted,
                  ),
                ],
              ),
            ),
          );
    return Container(
      constraints: const BoxConstraints(minHeight: 48),
      padding: _cellPadding(index),
      alignment: col.align,
      child: content,
    );
  }

  TableRow _row(BuildContext context, int index) {
    final c = context.colors;
    final row = widget.rows[index];
    final onTap = widget.onRowTap == null ? null : () => widget.onRowTap!(row);
    return TableRow(
      decoration: BoxDecoration(
        // không tô nền dòng thường để hiệu ứng hover/bấm (vẽ trên Material bên dưới) còn thấy được
        color: index == widget.selectedIndex ? c.rowSelected : null,
        border: Border(bottom: BorderSide(color: c.divider)),
      ),
      children: [
        for (var i = 0; i < widget.columns.length; i++)
          // TableRowInkWell: bấm/hover cả dòng như một nút
          TableRowInkWell(
            onTap: onTap,
            child: Container(
              constraints: BoxConstraints(minHeight: widget.rowHeight),
              padding: _cellPadding(i),
              alignment: widget.columns[i].align,
              child: widget.columns[i].cell(row),
            ),
          ),
      ],
    );
  }
}
