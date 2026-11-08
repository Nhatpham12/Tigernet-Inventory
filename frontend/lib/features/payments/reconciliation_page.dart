import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import '../common/lookups.dart';

String _ymd(DateTime d) =>
    '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

/// Đối chiếu công nợ — GET /bff/api/reports/reconciliation?partyId&from&to
/// và in biên bản /bff/api/reports/reconciliation/print.
class ReconciliationPage extends ConsumerStatefulWidget {
  const ReconciliationPage({super.key});

  @override
  ConsumerState<ReconciliationPage> createState() => _ReconciliationPageState();
}

class _ReconciliationPageState extends ConsumerState<ReconciliationPage> {
  String _kind = 'CUSTOMER';
  int? _partyId;
  DateTime? _from = DateTime(DateTime.now().year, DateTime.now().month, 1);
  DateTime? _to = DateTime.now();
  bool _loading = false;
  Object? _error;
  Map<String, dynamic>? _data;
  // tham số của lần xem gần nhất, để nút in khớp với số liệu đang hiện
  Map<String, dynamic>? _printQuery;

  Future<void> _view() async {
    if (_partyId == null || _from == null || _to == null) {
      Toast.error(context, 'Chọn đối tác và khoảng ngày.');
      return;
    }
    if (_from!.isAfter(_to!)) {
      Toast.error(context, 'Từ ngày phải trước hoặc bằng đến ngày.');
      return;
    }
    final query = {'partyId': _partyId, 'from': _ymd(_from!), 'to': _ymd(_to!)};
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final r = await ref
          .read(apiClientProvider)
          .get('/bff/api/reports/reconciliation', query: query);
      if (!mounted) return;
      setState(() {
        _data = Map<String, dynamic>.from(r as Map);
        _printQuery = query;
      });
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final parties = _kind == 'SUPPLIER'
        ? ref.watch(suppliersLookupProvider)
        : ref.watch(customersLookupProvider);
    final c = context.colors;
    final data = _data;
    final lines = ((data?['lines'] as List?) ?? const [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: 'Đối chiếu công nợ',
          subtitle: 'Số dư đầu kỳ, phát sinh và cuối kỳ theo từng đối tác',
          crumbs: const [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Báo cáo'),
            Crumb('Đối chiếu công nợ'),
          ],
          actions: [
            if (_printQuery != null)
              PrintButton(
                docs: [
                  PrintDoc(
                    'Biên bản đối chiếu công nợ',
                    '/bff/api/reports/reconciliation/print',
                    query: _printQuery!,
                  ),
                ],
              ),
          ],
        ),
        SectionCard(
          title: 'Điều kiện',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FormGrid(
                children: [
                  AppDropdown<String>(
                    label: 'Loại đối tác',
                    value: _kind,
                    items: const [
                      ('CUSTOMER', 'Khách hàng'),
                      ('SUPPLIER', 'Nhà cung cấp'),
                    ],
                    onChanged: (v) => setState(() {
                      _kind = v ?? _kind;
                      _partyId = null;
                    }),
                  ),
                  parties.when(
                    loading: () => const LinearProgressIndicator(),
                    error: (e, _) => Text('$e'),
                    data: (list) => AppDropdown<int>(
                      label: 'Đối tác',
                      required: true,
                      hint: 'Chọn đối tác',
                      value: _partyId,
                      items: [
                        for (final p in list)
                          (
                            ((p['party'] as Map)['id'] as num).toInt(),
                            partyName(p),
                          ),
                      ],
                      onChanged: (v) => setState(() => _partyId = v),
                    ),
                  ),
                  DateField(
                    label: 'Từ ngày',
                    required: true,
                    value: _from,
                    onChanged: (d) => setState(() => _from = d),
                  ),
                  DateField(
                    label: 'Đến ngày',
                    required: true,
                    value: _to,
                    onChanged: (d) => setState(() => _to = d),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: _loading ? null : _view,
                icon: const Icon(Icons.search),
                label: Text(_loading ? 'Đang tải...' : 'Xem đối chiếu'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        if (_loading && data == null)
          const LoadingView()
        else if (_error != null)
          StateView.error(_error!)
        else if (data != null) ...[
          SectionCard(
            title: '${data['partyName'] ?? ''}',
            trailing: Text(
              '${Fmt.date(data['from'])} – ${Fmt.date(data['to'])}',
              style: TextStyle(color: c.muted),
            ),
            child: Wrap(
              spacing: 16,
              runSpacing: 12,
              children: [
                _Stat('Số dư đầu kỳ', data['openingBalance'], c.text),
                _Stat('Phát sinh tăng', data['increase'], c.danger),
                _Stat('Phát sinh giảm', data['decrease'], c.success),
                _Stat(
                  'Số dư cuối kỳ',
                  data['closingBalance'],
                  c.text,
                  emphasize: true,
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          SectionCard(
            title: 'Chi tiết phát sinh',
            child: _LinesTable(lines: lines),
          ),
        ],
      ],
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat(this.label, this.value, this.color, {this.emphasize = false});

  final String label;
  final Object? value;
  final Color color;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Container(
      width: 220,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: c.surfaceMuted,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: TextStyle(fontSize: 13, color: c.muted)),
          const SizedBox(height: 6),
          Text(
            Fmt.money(value),
            style: TextStyle(
              fontSize: emphasize ? 22 : 18,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}

class _LinesTable extends StatelessWidget {
  const _LinesTable({required this.lines});

  final List<Map<String, dynamic>> lines;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    if (lines.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(24),
        child: Center(
          child: Text(
            'Không có phát sinh trong kỳ.',
            style: TextStyle(color: c.muted),
          ),
        ),
      );
    }
    final head = TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w700,
      color: c.muted,
    );
    Widget cell(String t, {double? w, bool right = false, TextStyle? st}) =>
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          child: SizedBox(
            width: w,
            child: Text(
              t,
              textAlign: right ? TextAlign.right : TextAlign.left,
              style: st ?? TextStyle(fontSize: 14, color: c.text),
            ),
          ),
        );
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Table(
        defaultColumnWidth: const IntrinsicColumnWidth(),
        border: TableBorder(horizontalInside: BorderSide(color: c.divider)),
        children: [
          TableRow(
            decoration: BoxDecoration(color: c.surfaceMuted),
            children: [
              cell('Ngày', st: head),
              cell('Chứng từ', st: head),
              cell('Diễn giải', w: 260, st: head),
              cell('Phát sinh tăng', right: true, st: head),
              cell('Phát sinh giảm', right: true, st: head),
            ],
          ),
          for (final l in lines)
            TableRow(
              children: [
                cell(Fmt.date(l['date'])),
                cell(
                  '${l['code'] ?? ''}',
                  st: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: c.text,
                  ),
                ),
                cell('${l['description'] ?? ''}', w: 260),
                cell(
                  (Fmt.toNum(l['increase']) ?? 0) == 0
                      ? ''
                      : Fmt.money(l['increase']),
                  right: true,
                ),
                cell(
                  (Fmt.toNum(l['decrease']) ?? 0) == 0
                      ? ''
                      : Fmt.money(l['decrease']),
                  right: true,
                ),
              ],
            ),
        ],
      ),
    );
  }
}
