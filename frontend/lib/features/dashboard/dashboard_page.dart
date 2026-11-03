import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/status_chip.dart';

/// GET /bff/dashboard theo kho đang chọn.
final dashboardProvider = FutureProvider.autoDispose<Map<String, dynamic>>((
  ref,
) async {
  final warehouseId = ref.watch(warehouseScopeProvider);
  final result = await ref
      .watch(apiClientProvider)
      .get('/bff/dashboard', query: {'warehouseId': warehouseId});
  return Map<String, dynamic>.from(result as Map);
});

/// Figma 965:1641 "Tổng quan / Dashboard".
class DashboardPage extends ConsumerWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final data = ref.watch(dashboardProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const PageHeader(
          title: 'Tổng quan kho',
          subtitle:
              'Theo dõi tình trạng tồn kho, giao dịch và các công việc cần xử lý trong hệ thống',
          crumbs: [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Tổng quan kho'),
          ],
        ),
        data.when(
          loading: () => const LoadingView(),
          error: (e, _) => StateView.error(
            e,
            onRetry: () => ref.invalidate(dashboardProvider),
          ),
          data: (d) => _DashboardBody(data: d),
        ),
      ],
    );
  }
}

class _DashboardBody extends StatelessWidget {
  const _DashboardBody({required this.data});

  final Map<String, dynamic> data;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final kpis = Map<String, dynamic>.from(data['kpis'] as Map);
    final attention = Map<String, dynamic>.from(data['attention'] as Map);
    final recent = ((data['recentTransactions'] as List?) ?? const [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
    final sectionTitle = TextStyle(
      fontSize: 18,
      fontWeight: FontWeight.w700,
      color: c.text,
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ResponsiveRow(
          children: [
            _KpiCard(
              icon: Icons.inventory_2_outlined,
              tone: c.accent,
              toneBg: c.warningBg,
              label: 'Tổng số sản phẩm',
              value: Fmt.number(kpis['totalProducts']),
            ),
            _KpiCard(
              icon: Icons.storage_outlined,
              tone: c.info,
              toneBg: c.infoBg,
              label: 'Giá trị tồn theo giá bán',
              value: Fmt.money(kpis['stockValue']),
            ),
            _KpiCard(
              icon: Icons.receipt_long_outlined,
              tone: c.info,
              toneBg: c.infoBg,
              label: 'Công nợ phải thu',
              value: Fmt.money(kpis['receivables']),
            ),
            _KpiCard(
              icon: Icons.warning_amber_rounded,
              tone: c.danger,
              toneBg: c.dangerBg,
              label: 'SKU dưới ngưỡng',
              value: Fmt.number(kpis['lowStockCount']),
              valueColor: c.danger,
            ),
          ],
        ),
        const SizedBox(height: 20),
        Text('Thao tác nhanh', style: sectionTitle),
        const SizedBox(height: 12),
        _ResponsiveRow(
          children: [
            _QuickAction(
              icon: Icons.add,
              tone: c.warning,
              toneBg: c.warningBg,
              title: 'Tạo phiếu nhập',
              subtitle: 'Nhập hàng vào kho',
              path: '/app/import-receipts/new',
            ),
            _QuickAction(
              icon: Icons.description_outlined,
              tone: c.info,
              toneBg: c.infoBg,
              title: 'Tạo phiếu xuất',
              subtitle: 'Xuất hàng khỏi kho',
              path: '/app/export-receipts/new',
            ),
            _QuickAction(
              icon: Icons.local_shipping_outlined,
              tone: c.success,
              toneBg: c.successBg,
              title: 'Tạo phiếu thuê',
              subtitle: 'Cho thuê tài sản',
              path: '/app/rentals/new',
            ),
            _QuickAction(
              icon: Icons.home_work_outlined,
              tone: c.purple,
              toneBg: c.purpleBg,
              title: 'Tạo phiếu mượn',
              subtitle: 'Cho mượn tài sản',
              path: '/app/lends/new',
            ),
          ],
        ),
        const SizedBox(height: 20),
        Text('Cần xử lý', style: sectionTitle),
        const SizedBox(height: 12),
        _ResponsiveRow(
          maxColumns: 3,
          children: [
            _AttentionCard(
              color: c.accent,
              bg: c.warningBg,
              icon: Icons.warning_amber_rounded,
              count: attention['lowStockCount'],
              title: 'SKU dưới ngưỡng tồn',
              subtitle: 'Sản phẩm cần nhập bổ sung',
              path: '/app/products?stockStatus=LOW',
            ),
            _AttentionCard(
              color: c.info,
              bg: c.infoBg,
              icon: Icons.description_outlined,
              count: attention['pendingApprovalCount'],
              title: 'Yêu cầu chờ phê duyệt',
              subtitle: 'Cần xem xét và phê duyệt',
              path: '/app/alerts',
            ),
            _AttentionCard(
              color: c.danger,
              bg: c.dangerBg,
              icon: Icons.schedule,
              count: attention['overdueRentalCount'],
              title: 'Phiếu thuê quá hạn',
              subtitle: 'Cần xử lý hoàn trả',
              path: '/app/rentals?state=OVERDUE',
            ),
          ],
        ),
        const SizedBox(height: 20),
        Row(
          children: [
            Expanded(child: Text('Giao dịch gần nhất', style: sectionTitle)),
            TextButton(
              onPressed: () => context.go('/app/reports/movements'),
              child: Text('Xem tất cả', style: TextStyle(color: c.info)),
            ),
          ],
        ),
        const SizedBox(height: 8),
        if (recent.isEmpty)
          const StateView.empty(message: 'Chưa có phiếu nhập/xuất nào.')
        else
          AppDataTable(
            onRowTap: (row) => context.go(
              row['type'] == 'IMPORT'
                  ? '/app/import-receipts'
                  : '/app/export-receipts',
            ),
            columns: [
              TableColumnDef(
                label: '#',
                flex: 1,
                minWidth: 40,
                cell: (row) => Text('${recent.indexOf(row) + 1}'),
              ),
              TableColumnDef.text(
                'Thời gian',
                (r) => Fmt.dateTime(r['time']),
                minWidth: 140,
              ),
              TableColumnDef(
                label: 'Loại',
                minWidth: 90,
                cell: (r) => r['type'] == 'IMPORT'
                    ? const StatusChip(label: 'Nhập', tone: ChipTone.success)
                    : const StatusChip(label: 'Xuất', tone: ChipTone.info),
              ),
              TableColumnDef.text(
                'Mã phiếu',
                (r) => '${r['receiptCode'] ?? ''}',
                minWidth: 110,
              ),
              TableColumnDef.text(
                'Đối tượng',
                (r) => '${r['partnerName'] ?? '—'}',
                flex: 3,
                minWidth: 160,
              ),
              TableColumnDef.text(
                'Kho',
                (r) => '${r['warehouseName'] ?? '—'}',
                minWidth: 110,
              ),
              TableColumnDef.text(
                'SL',
                (r) => Fmt.number(r['quantity']),
                flex: 1,
                minWidth: 50,
                align: Alignment.centerRight,
              ),
              TableColumnDef.text(
                'Giá trị',
                (r) => Fmt.money(r['amount']),
                minWidth: 120,
                align: Alignment.centerRight,
              ),
              TableColumnDef(
                label: 'Trạng thái',
                minWidth: 130,
                cell: (r) => r['type'] == 'IMPORT'
                    ? Statuses.importReceipt(r['status'] as String?)
                    : Statuses.receipt(r['status'] as String?),
              ),
            ],
            rows: recent,
          ),
      ],
    );
  }
}

/// 4 cột trên desktop, 2 cột khi hẹp (Figma: KPI reflow 2 cột ở 1280px), 1 cột trên mobile.
class _ResponsiveRow extends StatelessWidget {
  const _ResponsiveRow({required this.children, this.maxColumns = 4});

  final List<Widget> children;
  final int maxColumns;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      final w = constraints.maxWidth;
      final cols = w >= 1100 ? maxColumns : (w >= 600 ? 2 : 1);
      const gap = 12.0;
      final itemWidth = (w - gap * (cols - 1)) / cols;
      return Wrap(
        spacing: gap,
        runSpacing: gap,
        children: [
          for (final child in children)
            SizedBox(width: itemWidth, child: child),
        ],
      );
    },
  );
}

class _IconTile extends StatelessWidget {
  const _IconTile({
    required this.icon,
    required this.color,
    required this.bg,
    this.size = 52,
  });

  final IconData icon;
  final Color color;
  final Color bg;
  final double size;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      color: bg,
      borderRadius: BorderRadius.circular(10),
    ),
    child: Icon(icon, color: color, size: size * 0.45),
  );
}

class _KpiCard extends StatelessWidget {
  const _KpiCard({
    required this.icon,
    required this.tone,
    required this.toneBg,
    required this.label,
    required this.value,
    this.valueColor,
  });

  final IconData icon;
  final Color tone;
  final Color toneBg;
  final String label;
  final String value;
  final Color? valueColor;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Container(
      height: 112,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border.all(color: c.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          _IconTile(icon: icon, color: tone, bg: toneBg),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(label, style: TextStyle(fontSize: 12, color: c.muted)),
                const SizedBox(height: 4),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    value,
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                      color: valueColor ?? c.text,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickAction extends StatelessWidget {
  const _QuickAction({
    required this.icon,
    required this.tone,
    required this.toneBg,
    required this.title,
    required this.subtitle,
    required this.path,
  });

  final IconData icon;
  final Color tone;
  final Color toneBg;
  final String title;
  final String subtitle;
  final String path;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Material(
      color: c.surface,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: c.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: () => context.go(path),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              _IconTile(icon: icon, color: tone, bg: toneBg, size: 36),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: c.text,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: TextStyle(fontSize: 12, color: c.muted),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: tone),
            ],
          ),
        ),
      ),
    );
  }
}

class _AttentionCard extends StatelessWidget {
  const _AttentionCard({
    required this.color,
    required this.bg,
    required this.icon,
    required this.count,
    required this.title,
    required this.subtitle,
    required this.path,
  });

  final Color color;
  final Color bg;
  final IconData icon;
  final Object? count;
  final String title;
  final String subtitle;
  final String path;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Material(
      color: c.surface,
      shape: RoundedRectangleBorder(
        side: BorderSide(color: color.withValues(alpha: 0.4)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: () => context.go(path),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              _IconTile(icon: icon, color: color, bg: bg, size: 40),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      Fmt.number(count),
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: color,
                      ),
                    ),
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: c.text,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: TextStyle(fontSize: 11, color: c.muted),
                    ),
                  ],
                ),
              ),
              Text(
                'Xem chi tiết',
                style: TextStyle(fontSize: 12, color: color),
              ),
              Icon(Icons.chevron_right, size: 18, color: color),
            ],
          ),
        ),
      ),
    );
  }
}
