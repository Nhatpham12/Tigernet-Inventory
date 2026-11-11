import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';

typedef RowData = Map<String, dynamic>;

/// Cột bảng: [flex] chia bề rộng, [minWidth] để bảng cuộn ngang khi màn hẹp (Figma Flow §04).
class TableColumnDef {
  const TableColumnDef({
    required this.label,
    required this.cell,
    this.flex = 2,
    this.minWidth = 100,
    this.align = Alignment.centerLeft,
    this.sortKey,
  });

  final String label;
  final Widget Function(RowData row) cell;
  final int flex;
  final double minWidth;
  final Alignment align;
  final String? sortKey;

  /// Cột chữ đơn giản lấy từ một trường.
  factory TableColumnDef.text(
    String label,
    String Function(RowData row) value, {
    int flex = 2,
    double minWidth = 100,
    Alignment align = Alignment.centerLeft,
    String? sortKey,
    bool bold = false,
  }) => TableColumnDef(
    label: label,
    flex: flex,
    minWidth: minWidth,
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
    style: bold ? context.text.label : context.text.body1,
  );
}

/// Bảng dữ liệu dùng chung: header cố định nền xám, kẻ dòng, sort khi bấm tiêu đề (NOTE UXUI #1, #13).
class AppDataTable extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final c = context.colors;
    final minTableWidth =
        columns.fold<double>(0, (s, col) => s + col.minWidth) + 16;
    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth < minTableWidth
            ? minTableWidth
            : constraints.maxWidth;
        final table = SizedBox(
          width: width,
          child: Column(
            children: [
              Container(
                height: 48,
                padding: const EdgeInsets.symmetric(horizontal: 8),
                decoration: BoxDecoration(
                  color: c.surfaceMuted,
                  border: Border(bottom: BorderSide(color: c.border)),
                ),
                child: Row(
                  children: [for (final col in columns) _header(context, col)],
                ),
              ),
              for (var i = 0; i < rows.length; i++) _row(context, i),
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
          child: width > constraints.maxWidth
              ? Scrollbar(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: table,
                  ),
                )
              : table,
        );
      },
    );
  }

  Widget _header(BuildContext context, TableColumnDef col) {
    final c = context.colors;
    final active = col.sortKey != null && col.sortKey == sortKey;
    final label = Text(
      col.label,
      style: context.text.label.copyWith(fontWeight: FontWeight.w700),
    );
    return Expanded(
      flex: col.flex,
      child: Align(
        alignment: col.align,
        child: col.sortKey == null || onSort == null
            ? Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: label,
              )
            : InkWell(
                onTap: () => onSort!(col.sortKey!),
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 4,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Flexible(child: label),
                      const SizedBox(width: 4),
                      Icon(
                        active
                            ? (sortAsc
                                  ? Icons.arrow_upward
                                  : Icons.arrow_downward)
                            : Icons.unfold_more,
                        size: 14,
                        color: active ? c.accent : c.muted,
                      ),
                    ],
                  ),
                ),
              ),
      ),
    );
  }

  Widget _row(BuildContext context, int index) {
    final c = context.colors;
    final row = rows[index];
    return Material(
      color: index == selectedIndex ? c.rowSelected : c.surface,
      child: InkWell(
        onTap: onRowTap == null ? null : () => onRowTap!(row),
        hoverColor: c.surfaceMuted,
        child: Container(
          constraints: BoxConstraints(minHeight: rowHeight),
          padding: const EdgeInsets.symmetric(horizontal: 8),
          decoration: BoxDecoration(
            border: Border(bottom: BorderSide(color: c.divider)),
          ),
          child: Row(
            children: [
              for (final col in columns)
                Expanded(
                  flex: col.flex,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 8,
                    ),
                    child: Align(alignment: col.align, child: col.cell(row)),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
