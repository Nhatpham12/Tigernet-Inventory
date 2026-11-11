import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/role.dart';
import '../../core/auth/session_controller.dart';
import '../../core/providers.dart';
import '../../core/router/menu.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
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
    final role = ref.watch(sessionProvider).value?.role ?? AppRole.unknown;
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
          data: (d) => _DashboardBody(data: d, role: role),
        ),
      ],
    );
  }
}

class _DashboardBody extends StatelessWidget {
  const _DashboardBody({required this.data, required this.role});

  final Map<String, dynamic> data;
  final AppRole role;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final kpis = Map<String, dynamic>.from(data['kpis'] as Map);
    final attention = Map<String, dynamic>.from(data['attention'] as Map);
    final recent = ((data['recentTransactions'] as List?) ?? const [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
    final sectionTitle = context.text.h2;

    // Chỉ hiện lối tắt mà vai trò hiện tại mở được (tránh bấm vào rơi trang 403).
    final quickActions = [
      const _QuickAction(
        icon: AppIcons.importGoods,
        title: 'Tạo phiếu nhập',
        subtitle: 'Nhập hàng vào kho',
        path: '/app/import-receipts/new',
      ),
      const _QuickAction(
        icon: AppIcons.exportGoods,
        title: 'Tạo phiếu xuất',
        subtitle: 'Xuất hàng khỏi kho',
        path: '/app/export-receipts/new',
      ),
      const _QuickAction(
        icon: AppIcons.rental,
        title: 'Tạo phiếu thuê',
        subtitle: 'Cho thuê tài sản',
        path: '/app/rentals/new',
      ),
      const _QuickAction(
        icon: AppIcons.lend,
        title: 'Tạo phiếu mượn',
        subtitle: 'Cho mượn tài sản',
        path: '/app/lends/new',
      ),
    ].where((a) => canAccessPath(a.path, role)).toList();

    // Màu theo đúng ý nghĩa trạng thái: đỏ = tồn thấp/quá hạn, vàng = chờ duyệt (như chip).
    final attentionCards = [
      _AttentionCard(
        color: c.danger,
        bg: c.dangerBg,
        icon: Icons.warning_amber_rounded,
        count: attention['lowStockCount'],
        title: 'SKU dưới ngưỡng tồn',
        subtitle: 'Sản phẩm cần nhập bổ sung',
        path: '/app/products?stockStatus=LOW',
      ),
      _AttentionCard(
        color: c.warning,
        bg: c.warningBg,
        icon: Icons.schedule,
        count: attention['pendingApprovalCount'],
        title: 'Yêu cầu chờ phê duyệt',
        subtitle: 'Cần xem xét và phê duyệt',
        path: '/app/alerts',
      ),
      _AttentionCard(
        color: c.danger,
        bg: c.dangerBg,
        icon: Icons.event_busy_outlined,
        count: attention['overdueRentalCount'],
        title: 'Phiếu thuê quá hạn',
        subtitle: 'Cần xử lý hoàn trả',
        path: '/app/rentals?state=OVERDUE',
      ),
    ].where((a) => canAccessPath(a.path, role)).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ResponsiveRow(
          children: [
            _KpiCard(
              icon: AppIcons.products,
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
              tone: c.warning,
              toneBg: c.warningBg,
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
        if (quickActions.isNotEmpty) ...[
          const SizedBox(height: AppSpace.s32),
          Text('Thao tác nhanh', style: sectionTitle),
          const SizedBox(height: AppSpace.s16),
          _ResponsiveRow(children: quickActions),
        ],
        if (attentionCards.isNotEmpty) ...[
          const SizedBox(height: AppSpace.s32),
          Text('Cần xử lý', style: sectionTitle),
          const SizedBox(height: AppSpace.s16),
          _ResponsiveRow(maxColumns: 3, children: attentionCards),
        ],
        const SizedBox(height: AppSpace.s32),
        Row(
          children: [
            Expanded(child: Text('Giao dịch gần nhất', style: sectionTitle)),
            if (canAccessPath('/app/reports/movements', role))
              TextButton(
                onPressed: () => context.go('/app/reports/movements'),
                child: Text('Xem tất cả', style: TextStyle(color: c.info)),
              ),
          ],
        ),
        const SizedBox(height: AppSpace.s8),
        if (recent.isEmpty)
          const StateView.empty(message: 'Chưa có phiếu nhập/xuất nào.')
        else
          AppDataTable(
            // Mở thẳng phiếu được bấm: danh sách lọc sẵn theo mã phiếu.
            onRowTap: (row) {
              final base = row['type'] == 'IMPORT'
                  ? '/app/import-receipts'
                  : '/app/export-receipts';
              if (!canAccessPath(base, role)) return;
              final code = '${row['receiptCode'] ?? ''}';
              context.go(
                Uri(
                  path: base,
                  queryParameters: code.isEmpty ? null : {'q': code},
                ).toString(),
              );
            },
            columns: [
              TableColumnDef(
                label: '#',
                flex: 1,
                minWidth: 40,
                cell: (row) => Text(
                  '${recent.indexOf(row) + 1}',
                  style: context.text.body1,
                ),
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
                bold: true,
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
      const gap = AppSpace.s16;
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
      borderRadius: BorderRadius.circular(8),
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
      padding: const EdgeInsets.all(AppSpace.s16),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border.all(color: c.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          _IconTile(icon: icon, color: tone, bg: toneBg),
          const SizedBox(width: AppSpace.s16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  label,
                  style: context.text.caption.copyWith(color: c.muted),
                ),
                const SizedBox(height: AppSpace.s4),
                FittedBox(
                  fit: BoxFit.scaleDown,
                  alignment: Alignment.centerLeft,
                  child: Text(
                    value,
                    style: context.text.kpi.copyWith(
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

/// Thẻ thao tác nhanh: một tông trung tính (màu trạng thái để dành cho trạng thái).
/// Hover: While Hovering → Change To (viền accent) → animate 150ms Ease In-Out.
class _QuickAction extends StatefulWidget {
  const _QuickAction({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.path,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String path;

  @override
  State<_QuickAction> createState() => _QuickActionState();
}

class _QuickActionState extends State<_QuickAction> {
  bool _hover = false;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      curve: Curves.easeInOut,
      decoration: BoxDecoration(
        color: c.surface,
        border: Border.all(color: _hover ? c.accent : c.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Material(
        type: MaterialType.transparency,
        child: InkWell(
          borderRadius: BorderRadius.circular(8),
          onHover: (v) => setState(() => _hover = v),
          onTap: () => context.go(widget.path),
          child: Padding(
            padding: const EdgeInsets.all(AppSpace.s16),
            child: Row(
              children: [
                _IconTile(
                  icon: widget.icon,
                  color: c.text,
                  bg: c.surfaceMuted,
                  size: 40,
                ),
                const SizedBox(width: AppSpace.s16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(widget.title, style: context.text.label),
                      Text(
                        widget.subtitle,
                        style: context.text.caption.copyWith(color: c.muted),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.chevron_right,
                  color: _hover ? c.accentText : c.muted,
                ),
              ],
            ),
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
          padding: const EdgeInsets.all(AppSpace.s16),
          child: Row(
            children: [
              _IconTile(icon: icon, color: color, bg: bg, size: 40),
              const SizedBox(width: AppSpace.s16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      Fmt.number(count),
                      style: context.text.h2.copyWith(
                        fontWeight: FontWeight.w800,
                        color: color,
                      ),
                    ),
                    Text(title, style: context.text.label),
                    Text(
                      subtitle,
                      style: context.text.caption.copyWith(color: c.muted),
                    ),
                  ],
                ),
              ),
              // Màu chữ trạng thái (đỏ/vàng đậm) đều ≥ 4,5:1 trên nền trắng.
              Text(
                'Xem chi tiết',
                style: context.text.caption.copyWith(
                  color: color,
                  fontWeight: FontWeight.w600,
                ),
              ),
              Icon(Icons.chevron_right, size: 20, color: color),
            ],
          ),
        ),
      ),
    );
  }
}
