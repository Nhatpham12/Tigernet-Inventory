import 'package:flutter/gestures.dart' show PointerDeviceKind;
import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';

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
    style: bold ? context.text.label : context.text.body1,
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

  /// Thanh cuộn ngang thứ hai ở ĐẦU bảng: bảng dài thì thanh ở đáy nằm khuất dưới cùng trang,
  /// người dùng không biết là cuộn ngang được. Hai thanh đồng bộ vị trí với nhau.
  final _topBar = ScrollController();
  bool _syncing = false;

  /// Bề rộng thật của bảng (lấy từ thanh cuộn đáy sau khi dựng); 0 = chưa biết / không tràn.
  double _contentWidth = 0;

  @override
  void initState() {
    super.initState();
    _horizontal.addListener(() => _sync(_horizontal, _topBar));
    _topBar.addListener(() => _sync(_topBar, _horizontal));
  }

  /// Kéo thanh này thì thanh kia theo; cờ [_syncing] chặn hai listener gọi qua lại vô hạn.
  void _sync(ScrollController from, ScrollController to) {
    if (_syncing || !to.hasClients || !from.hasClients) return;
    final target = from.offset.clamp(0.0, to.position.maxScrollExtent);
    if (target == to.offset) return;
    _syncing = true;
    to.jumpTo(target);
    _syncing = false;
  }

  /// Ghi lại bề rộng bảng khi kích thước thay đổi (đổi cột, đổi cỡ cửa sổ) để vẽ thanh đầu bảng.
  bool _onMetrics(ScrollMetricsNotification n) {
    final m = n.metrics;
    final overflow = m.maxScrollExtent > 0.5;
    final width = overflow ? m.maxScrollExtent + m.viewportDimension : 0.0;
    if ((width - _contentWidth).abs() > 0.5) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) setState(() => _contentWidth = width);
      });
    }
    return false;
  }

  @override
  void dispose() {
    _horizontal.dispose();
    _topBar.dispose();
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
          // trừ viền khung (1px mỗi bên) + 0,5px sai số làm tròn: không thì bảng luôn "tràn" ~1,5px
          // và hiện thanh cuộn ngang thừa dù bảng vừa khung
          constraints: BoxConstraints(
            minWidth: (constraints.maxWidth - 2.5).clamp(0, double.infinity),
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
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_contentWidth > 0)
                    // chỉ là bản sao để nhìn/kéo của thanh đáy: ẩn khỏi trình đọc màn hình cho khỏi báo 2 vùng cuộn
                    ExcludeSemantics(
                      child: Scrollbar(
                        key: const ValueKey('app-data-table-top-scrollbar'),
                        controller: _topBar,
                        thumbVisibility: true,
                        child: SingleChildScrollView(
                          controller: _topBar,
                          scrollDirection: Axis.horizontal,
                          child: SizedBox(width: _contentWidth, height: 10),
                        ),
                      ),
                    ),
                  Scrollbar(
                    controller: _horizontal,
                    thumbVisibility: true,
                    child: NotificationListener<ScrollMetricsNotification>(
                      onNotification: _onMetrics,
                      child: SingleChildScrollView(
                        controller: _horizontal,
                        scrollDirection: Axis.horizontal,
                        // chừa chỗ cho thanh cuộn ngang ở đáy bảng
                        padding: const EdgeInsets.only(bottom: 10),
                        child: table,
                      ),
                    ),
                  ),
                ],
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
      style: context.text.label.copyWith(fontWeight: FontWeight.w700),
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
