import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/section_card.dart';
import 'warranty_common.dart';
import 'warranty_status_dialog.dart';

/// Chi tiết phiếu bảo hành — GET /bff/api/warranty-claims/{id}.
class WarrantyClaimDetailPage extends ConsumerStatefulWidget {
  const WarrantyClaimDetailPage({super.key, required this.claimId});

  final int claimId;

  @override
  ConsumerState<WarrantyClaimDetailPage> createState() =>
      _WarrantyClaimDetailPageState();
}

class _WarrantyClaimDetailPageState
    extends ConsumerState<WarrantyClaimDetailPage> {
  late Future<Map<String, dynamic>> _future = _load();

  Future<Map<String, dynamic>> _load() async => Map<String, dynamic>.from(
    await ref
            .read(apiClientProvider)
            .get('/bff/api/warranty-claims/${widget.claimId}')
        as Map,
  );

  void _reload() => setState(() => _future = _load());

  /// In chung biên bản của mọi phiếu cùng khách và cùng ngày nhận.
  Future<void> _printSameDay(Map<String, dynamic> claim) async {
    try {
      final day = Fmt.apiDateTime(
        Fmt.parseDate(claim['receivedAt']),
      )!.substring(0, 10);
      final list = await ref
          .read(apiClientProvider)
          .getList(
            '/bff/api/warranty-claims',
            query: {'customerId': claim['customerId'], 'from': day, 'to': day},
          );
      final ids = [
        for (final c in list)
          if (c['customerId'] == claim['customerId']) c['id'],
      ];
      if (ids.isEmpty) ids.add(claim['id']);
      if (!mounted) return;
      await PrintButton.run(
        context,
        ref,
        warrantyPrintDoc(ids),
        download: false,
      );
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, dynamic>>(
      future: _future,
      builder: (context, snap) {
        if (snap.hasError) {
          return StateView.error(snap.error!, onRetry: _reload);
        }
        final r = snap.data;
        if (r == null) return const LoadingView();
        final c = context.colors;
        final canChange = kWarrantyTransitions.containsKey(r['status']);
        final expiry = Fmt.parseDate(r['warrantyExpiry']);
        final expired = expiry != null && expiry.isBefore(DateTime.now());
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PageHeader(
              title: '${r['claimCode']}',
              crumbs: const [
                Crumb('Trang chủ', '/app/dashboard'),
                Crumb('Bảo hành', '/app/warranty-claims'),
                Crumb('Chi tiết phiếu bảo hành'),
              ],
              actions: [
                warrantyStatusChip(r['status'] as String?),
                PrintButton(
                  docs: [
                    warrantyPrintDoc([r['id']]),
                  ],
                ),
                OutlinedButton.icon(
                  onPressed: () => _printSameDay(r),
                  icon: const Icon(Icons.library_books_outlined),
                  label: const Text('In chung khách/ngày'),
                ),
                if (canChange)
                  ElevatedButton(
                    onPressed: () async {
                      if (await showWarrantyStatusDialog(context, r)) _reload();
                    },
                    child: const Text('Đổi trạng thái'),
                  ),
              ],
            ),
            SectionCard(
              title: 'Thiết bị và khách hàng',
              child: _Grid([
                ('Khách hàng', '${r['customerName'] ?? '—'}'),
                ('Thiết bị', '${r['productName'] ?? '—'}'),
                ('Số seri', '${r['serial'] ?? '—'}'),
                ('Phiếu xuất gốc', '${r['exportReceiptCode'] ?? '—'}'),
                (
                  'Hạn bảo hành',
                  '${Fmt.date(r['warrantyExpiry'])}${expired ? ' (đã hết hạn)' : ''}',
                ),
                ('Kho tiếp nhận', '${r['receivedWarehouseName'] ?? '—'}'),
                ('Người xử lý', '${r['handledByName'] ?? '—'}'),
              ]),
            ),
            const SizedBox(height: 16),
            SectionCard(
              title: 'Tiến trình',
              child: _Grid([
                ('Ngày nhận', Fmt.dateTime(r['receivedAt'])),
                (
                  'Ngày gửi đi',
                  r['sentAt'] == null ? '—' : Fmt.dateTime(r['sentAt']),
                ),
                (
                  'Ngày hàng về',
                  r['returnedAt'] == null ? '—' : Fmt.dateTime(r['returnedAt']),
                ),
                (
                  'Kết quả',
                  r['result'] == null
                      ? '—'
                      : warrantyResultLabel(r['result'] as String?),
                ),
                ('Chi phí', r['cost'] == null ? '—' : Fmt.money(r['cost'])),
                ('Serial thay thế', '${r['replacementSerial'] ?? '—'}'),
                (
                  'Hạn BH mới',
                  r['newWarrantyExpiry'] == null
                      ? '—'
                      : Fmt.date(r['newWarrantyExpiry']),
                ),
              ]),
            ),
            const SizedBox(height: 16),
            SectionCard(
              title: 'Tình trạng / ghi chú',
              child: Text(
                (r['note'] as String?)?.isNotEmpty == true
                    ? '${r['note']}'
                    : '—',
                style: TextStyle(fontSize: 14, color: c.text),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _Grid extends StatelessWidget {
  const _Grid(this.items);
  final List<(String, String)> items;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Wrap(
      spacing: 32,
      runSpacing: 16,
      children: [
        for (final (label, value) in items)
          SizedBox(
            width: 240,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(fontSize: 12, color: c.muted)),
                const SizedBox(height: 4),
                Text(
                  value,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: c.text,
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
