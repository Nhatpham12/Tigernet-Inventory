import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/session_controller.dart';
import '../../core/network/page_data.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../feedback/state_views.dart';
import '../widgets/page_header.dart';
import 'app_data_table.dart';

/// Bộ lọc dạng nút "Trạng thái: Tất cả ▾".
class ListFilter {
  const ListFilter({
    required this.key,
    required this.label,
    required this.options,
  });

  final String key;
  final String label;

  /// (giá trị gửi BFF, nhãn). Giá trị null = Tất cả.
  final List<(String?, String)> options;
}

/// Cho phép trang cha yêu cầu tải lại (sau khi duyệt, ghi nhận thanh toán...).
class PagedListController extends ChangeNotifier {
  void reload() => notifyListeners();
}

/// Trang danh sách chuẩn theo Figma: tiêu đề + nút, ô tìm kiếm, bộ lọc, sắp xếp, bảng, phân trang.
/// Dữ liệu lấy từ một endpoint phân trang của BFF (keyword, sort, page, size, warehouseId).
class PagedListView extends ConsumerStatefulWidget {
  const PagedListView({
    super.key,
    required this.title,
    required this.endpoint,
    required this.columns,
    this.subtitle,
    this.crumbs = const [],
    this.actions = const [],
    this.filters = const [],
    this.initialFilters = const {},
    this.extraQuery = const {},
    this.searchHint = 'Tìm kiếm...',
    this.defaultSort,
    this.onRowTap,
    this.useWarehouseScope = true,
    this.itemLabel = 'bản ghi',
    this.controller,
    this.emptyMessage,
    this.banner,
  });

  final String title;
  final String? subtitle;
  final List<Crumb> crumbs;
  final List<Widget> actions;
  final String endpoint;
  final List<TableColumnDef> columns;
  final List<ListFilter> filters;

  /// Giá trị lọc ban đầu theo key của [filters] (vd mở từ đường dẫn có ?userId=).
  final Map<String, String?> initialFilters;
  final Map<String, dynamic> extraQuery;
  final String searchHint;
  final String? defaultSort;
  final ValueChanged<RowData>? onRowTap;
  final bool useWarehouseScope;
  final String itemLabel;
  final PagedListController? controller;
  final String? emptyMessage;
  final Widget Function(PageData<RowData> data)? banner;

  @override
  ConsumerState<PagedListView> createState() => _PagedListViewState();
}

class _PagedListViewState extends ConsumerState<PagedListView> {
  static const pageSizes = [6, 12, 20, 50];

  final _search = TextEditingController();
  Timer? _debounce;
  late Map<String, String?> _filterValues = {
    for (final f in widget.filters) f.key: widget.initialFilters[f.key],
  };
  String? _sortKey;
  bool _sortAsc = true;
  int _page = 1;
  int _size = 20;
  Future<PageData<RowData>>? _future;
  int? _warehouseId;

  @override
  void initState() {
    super.initState();
    final sort = widget.defaultSort?.split(',');
    if (sort != null) {
      _sortKey = sort.first;
      _sortAsc = sort.length < 2 || sort[1] != 'desc';
    }
    widget.controller?.addListener(_load);
    _warehouseId = ref.read(warehouseScopeProvider);
    _load();
  }

  @override
  void didUpdateWidget(covariant PagedListView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.endpoint != widget.endpoint ||
        '${oldWidget.extraQuery}' != '${widget.extraQuery}') {
      _filterValues = {for (final f in widget.filters) f.key: null};
      _page = 1;
      _load();
    }
  }

  @override
  void dispose() {
    widget.controller?.removeListener(_load);
    _debounce?.cancel();
    _search.dispose();
    super.dispose();
  }

  void _load() {
    final api = ref.read(apiClientProvider);
    setState(() {
      _future = api.getPage(
        widget.endpoint,
        query: {
          ...widget.extraQuery,
          ..._filterValues,
          if (widget.useWarehouseScope) 'warehouseId': _warehouseId,
          'keyword': _search.text,
          if (_sortKey != null)
            'sort': '$_sortKey,${_sortAsc ? 'asc' : 'desc'}',
          'page': _page,
          'size': _size,
        },
      );
    });
  }

  void _onSearchChanged(String _) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), () {
      _page = 1;
      _load();
    });
  }

  void _onSort(String key) {
    if (_sortKey == key) {
      _sortAsc = !_sortAsc;
    } else {
      _sortKey = key;
      _sortAsc = true;
    }
    _load();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<int?>(warehouseScopeProvider, (_, next) {
      _warehouseId = next;
      _page = 1;
      _load();
    });
    final c = context.colors;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: widget.title,
          subtitle: widget.subtitle,
          crumbs: widget.crumbs,
          actions: widget.actions,
        ),
        _toolbar(context),
        const SizedBox(height: 16),
        FutureBuilder<PageData<RowData>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.hasError) {
              return StateView.error(snapshot.error!, onRetry: _load);
            }
            final data = snapshot.data;
            if (data == null) return const LoadingView();
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (widget.banner != null) ...[
                  widget.banner!(data),
                  const SizedBox(height: 12),
                ],
                if (data.items.isEmpty)
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: c.border),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: StateView.empty(
                      message:
                          widget.emptyMessage ??
                          (_search.text.isNotEmpty ||
                                  _filterValues.values.any((v) => v != null)
                              ? 'Không có kết quả phù hợp. Thử đổi từ khóa hoặc xóa bộ lọc.'
                              : null),
                    ),
                  )
                else
                  Opacity(
                    opacity: snapshot.connectionState == ConnectionState.waiting
                        ? 0.5
                        : 1,
                    child: AppDataTable(
                      columns: widget.columns,
                      rows: data.items,
                      onRowTap: widget.onRowTap,
                      sortKey: _sortKey,
                      sortAsc: _sortAsc,
                      onSort: _onSort,
                    ),
                  ),
                const SizedBox(height: 16),
                _pagination(context, data),
              ],
            );
          },
        ),
      ],
    );
  }

  Widget _toolbar(BuildContext context) {
    final c = context.colors;
    final sortable = widget.columns
        .where((col) => col.sortKey != null)
        .toList();
    return Wrap(
      spacing: 12,
      runSpacing: 12,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        SizedBox(
          width: 560,
          child: TextField(
            controller: _search,
            onChanged: _onSearchChanged,
            style: TextStyle(fontSize: 14, color: c.text),
            decoration: InputDecoration(
              hintText: widget.searchHint,
              prefixIcon: Icon(Icons.search, size: 20, color: c.muted),
            ),
          ),
        ),
        for (final filter in widget.filters)
          PopupMenuButton<String?>(
            tooltip: filter.label,
            onSelected: (value) {
              _filterValues[filter.key] = value;
              _page = 1;
              _load();
            },
            itemBuilder: (_) => [
              for (final (value, label) in filter.options)
                PopupMenuItem<String?>(value: value, child: Text(label)),
            ],
            child: _ToolbarButton(
              label:
                  '${filter.label}: ${filter.options.firstWhere((o) => o.$1 == _filterValues[filter.key], orElse: () => filter.options.first).$2}',
            ),
          ),
        if (sortable.isNotEmpty)
          PopupMenuButton<(String, bool)>(
            tooltip: 'Sắp xếp',
            onSelected: (choice) {
              _sortKey = choice.$1;
              _sortAsc = choice.$2;
              _load();
            },
            itemBuilder: (_) => [
              for (final col in sortable) ...[
                PopupMenuItem(
                  value: (col.sortKey!, true),
                  child: Text('${col.label} – tăng dần'),
                ),
                PopupMenuItem(
                  value: (col.sortKey!, false),
                  child: Text('${col.label} – giảm dần'),
                ),
              ],
            ],
            child: const _ToolbarButton(label: 'Sắp xếp'),
          ),
        if (_search.text.isNotEmpty ||
            _filterValues.values.any((v) => v != null))
          TextButton(
            onPressed: () {
              _search.clear();
              _filterValues = {for (final f in widget.filters) f.key: null};
              _page = 1;
              _load();
            },
            child: const Text('Xóa lọc'),
          ),
      ],
    );
  }

  Widget _pagination(BuildContext context, PageData<RowData> data) {
    final c = context.colors;
    final from = data.totalItems == 0 ? 0 : (data.page - 1) * data.size + 1;
    final to = (from + data.items.length - 1).clamp(0, data.totalItems);
    final pages = _visiblePages(data.page, data.totalPages);
    return Wrap(
      alignment: WrapAlignment.spaceBetween,
      crossAxisAlignment: WrapCrossAlignment.center,
      runSpacing: 12,
      spacing: 16,
      children: [
        Text(
          'Hiển thị $from–$to / ${data.totalItems} ${widget.itemLabel}',
          style: TextStyle(fontSize: 14, color: c.muted),
        ),
        Wrap(
          spacing: 8,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text(
              'Trang ${data.page} / ${data.totalPages}',
              style: TextStyle(fontSize: 14, color: c.text),
            ),
            PopupMenuButton<int>(
              onSelected: (size) {
                _size = size;
                _page = 1;
                _load();
              },
              itemBuilder: (_) => [
                for (final s in pageSizes)
                  PopupMenuItem(value: s, child: Text('$s dòng/trang')),
              ],
              child: _ToolbarButton(label: '$_size dòng/trang', compact: true),
            ),
            OutlinedButton(
              onPressed: data.page > 1
                  ? () {
                      _page = data.page - 1;
                      _load();
                    }
                  : null,
              style: OutlinedButton.styleFrom(minimumSize: const Size(64, 36)),
              child: const Text('Trước'),
            ),
            for (final p in pages)
              p == null
                  ? Text('…', style: TextStyle(color: c.muted))
                  : SizedBox(
                      width: 36,
                      height: 36,
                      child: p == data.page
                          ? ElevatedButton(
                              onPressed: () {},
                              style: ElevatedButton.styleFrom(
                                minimumSize: Size.zero,
                                padding: EdgeInsets.zero,
                              ),
                              child: Text('$p'),
                            )
                          : OutlinedButton(
                              onPressed: () {
                                _page = p;
                                _load();
                              },
                              style: OutlinedButton.styleFrom(
                                minimumSize: Size.zero,
                                padding: EdgeInsets.zero,
                              ),
                              child: Text('$p'),
                            ),
                    ),
            OutlinedButton(
              onPressed: data.page < data.totalPages
                  ? () {
                      _page = data.page + 1;
                      _load();
                    }
                  : null,
              style: OutlinedButton.styleFrom(minimumSize: const Size(64, 36)),
              child: const Text('Sau'),
            ),
          ],
        ),
      ],
    );
  }

  /// 1 … 4 5 6 … 10
  static List<int?> _visiblePages(int current, int total) {
    if (total <= 7) return [for (var i = 1; i <= total; i++) i];
    final result = <int?>[1];
    final start = (current - 1).clamp(2, total - 3);
    final end = (current + 1).clamp(4, total - 1);
    if (start > 2) result.add(null);
    for (var i = start; i <= end; i++) {
      result.add(i);
    }
    if (end < total - 1) result.add(null);
    result.add(total);
    return result;
  }
}

class _ToolbarButton extends StatelessWidget {
  const _ToolbarButton({required this.label, this.compact = false});

  final String label;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Container(
      height: compact ? 36 : 44,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        border: Border.all(color: c.muted),
        borderRadius: BorderRadius.circular(8),
        color: c.surface,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: c.text,
            ),
          ),
          const SizedBox(width: 4),
          Icon(Icons.arrow_drop_down, size: 18, color: c.text),
        ],
      ),
    );
  }
}
