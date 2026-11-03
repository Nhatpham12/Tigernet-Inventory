import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/status_chip.dart';

/// Figma 1334:1464 "Cảnh báo và thông báo" + các tab 1903:1628 / 1887 / 2146.
class AlertsPage extends ConsumerStatefulWidget {
  const AlertsPage({super.key});

  @override
  ConsumerState<AlertsPage> createState() => _AlertsPageState();
}

class _AlertsPageState extends ConsumerState<AlertsPage> {
  final _controller = PagedListController();
  String? _type;

  static const _tabs = <(String?, String)>[
    (null, 'Tất cả'),
    ('LOW_STOCK', 'Tồn kho thấp'),
    ('RENT_DUE', 'Sắp hết hạn'),
    ('PENDING_APPROVAL', 'Chờ phê duyệt'),
    ('DEBT_OVERDUE', 'Công nợ'),
    ('TRANSFER_PENDING', 'Chuyển kho'),
  ];

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _markRead(Map<String, dynamic> alert) async {
    try {
      await ref
          .read(apiClientProvider)
          .put('/bff/api/alerts/${alert['id']}/read');
      await ref.read(sessionProvider.notifier).refreshUnread();
      _controller.reload();
      if (mounted) Toast.success(context, 'Đã đánh dấu đã xử lý.');
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final unread = ref.watch(sessionProvider).value?.unreadAlerts ?? 0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final (value, label) in _tabs)
              ChoiceChip(
                label: Text(label),
                selected: _type == value,
                selectedColor: c.navActive,
                onSelected: (_) => setState(() => _type = value),
              ),
          ],
        ),
        const SizedBox(height: 8),
        PagedListView(
          title: 'Cảnh báo và thông báo',
          subtitle: '$unread cảnh báo chưa xử lý',
          crumbs: const [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Cảnh báo'),
          ],
          controller: _controller,
          endpoint: '/bff/alerts',
          extraQuery: {'alertType': _type},
          defaultSort: 'createdAt,desc',
          searchHint: 'Tìm nội dung cảnh báo, kho...',
          itemLabel: 'cảnh báo',
          filters: const [
            ListFilter(
              key: 'isResolved',
              label: 'Trạng thái',
              options: [
                (null, 'Tất cả'),
                ('false', 'Chưa xử lý'),
                ('true', 'Đã xử lý'),
              ],
            ),
          ],
          columns: [
            TableColumnDef.text(
              'Thời gian',
              (r) => Fmt.dateTime(r['createdAt']),
              minWidth: 140,
              sortKey: 'createdAt',
            ),
            TableColumnDef(
              label: 'Loại',
              minWidth: 150,
              cell: (r) => StatusChip(
                label: Statuses.alertType(r['alertType'] as String?),
                tone: switch (r['alertType']) {
                  'LOW_STOCK' => ChipTone.danger,
                  'RENT_DUE' || 'DEBT_OVERDUE' => ChipTone.warning,
                  _ => ChipTone.info,
                },
              ),
            ),
            TableColumnDef.text(
              'Nội dung',
              (r) => '${r['message'] ?? ''}',
              flex: 5,
              minWidth: 260,
            ),
            TableColumnDef.text(
              'Kho',
              (r) => '${r['warehouseCode'] ?? '—'}',
              flex: 1,
              minWidth: 80,
            ),
            TableColumnDef(
              label: 'Thao tác',
              minWidth: 140,
              align: Alignment.center,
              cell: (r) => r['isResolved'] == true
                  ? const StatusChip(
                      label: 'Đã xử lý',
                      tone: ChipTone.success,
                      icon: Icons.check,
                    )
                  : OutlinedButton(
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size(0, 32),
                      ),
                      onPressed: () => _markRead(r),
                      child: const Text('Đánh dấu đã xử lý'),
                    ),
            ),
          ],
        ),
      ],
    );
  }
}
