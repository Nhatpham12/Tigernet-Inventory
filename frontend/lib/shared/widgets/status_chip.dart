import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';

enum ChipTone { success, warning, danger, info, neutral, purple }

/// Badge trạng thái bo tròn (Figma: Hoàn tất / Đang xử lý / Chờ nhận / Đã hủy...).
class StatusChip extends StatelessWidget {
  const StatusChip({
    super.key,
    required this.label,
    required this.tone,
    this.icon,
  });

  final String label;
  final ChipTone tone;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final (fg, bg) = switch (tone) {
      ChipTone.success => (c.success, c.successBg),
      ChipTone.warning => (c.warning, c.warningBg),
      ChipTone.danger => (c.danger, c.dangerBg),
      ChipTone.info => (c.info, c.infoBg),
      ChipTone.purple => (c.purple, c.purpleBg),
      ChipTone.neutral => (c.muted, c.surfaceMuted),
    };
    return Container(
      constraints: const BoxConstraints(minWidth: 96),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 13, color: fg),
            const SizedBox(width: 4),
          ],
          Flexible(
            child: Text(
              label,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: fg,
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Nhãn và màu cho các enum trạng thái của backend (ERD v7).
class Statuses {
  const Statuses._();

  static StatusChip receipt(String? status) => switch (status) {
    'DRAFT' => const StatusChip(label: 'Nháp', tone: ChipTone.neutral),
    'PENDING' => const StatusChip(
      label: 'Chờ duyệt',
      tone: ChipTone.warning,
      icon: Icons.schedule,
    ),
    'CONFIRMED' => const StatusChip(
      label: 'Đã xác nhận',
      tone: ChipTone.info,
      icon: Icons.schedule,
    ),
    'EXPORTED' => const StatusChip(
      label: 'Đã xuất',
      tone: ChipTone.success,
      icon: Icons.check,
    ),
    'APPROVED' => const StatusChip(
      label: 'Đã duyệt',
      tone: ChipTone.success,
      icon: Icons.check,
    ),
    'REJECTED' => const StatusChip(label: 'Từ chối', tone: ChipTone.danger),
    'IN_TRANSIT' => const StatusChip(label: 'Đang chuyển', tone: ChipTone.info),
    'RECEIVED' => const StatusChip(
      label: 'Đã nhận',
      tone: ChipTone.success,
      icon: Icons.check,
    ),
    'COMPLETED' => const StatusChip(
      label: 'Hoàn tất',
      tone: ChipTone.success,
      icon: Icons.check,
    ),
    'CANCELLED' => const StatusChip(
      label: 'Đã hủy',
      tone: ChipTone.danger,
      icon: Icons.warning_amber,
    ),
    _ => StatusChip(label: status ?? '—', tone: ChipTone.neutral),
  };

  /// Trạng thái phiếu nhập: CONFIRMED = đã nhập kho.
  static StatusChip importReceipt(String? status) => status == 'CONFIRMED'
      ? const StatusChip(
          label: 'Đã nhập kho',
          tone: ChipTone.success,
          icon: Icons.check,
        )
      : receipt(status);

  static StatusChip stock(String? status) => switch (status) {
    'OUT_OF_STOCK' => const StatusChip(
      label: 'Hết hàng',
      tone: ChipTone.danger,
    ),
    'LOW' => const StatusChip(label: 'Tồn thấp', tone: ChipTone.danger),
    'NEAR_LOW' => const StatusChip(label: 'Sắp hết', tone: ChipTone.warning),
    _ => const StatusChip(label: 'Bình thường', tone: ChipTone.success),
  };

  static StatusChip rental(String? state) => switch (state) {
    'RETURNED' => const StatusChip(
      label: 'Đã trả',
      tone: ChipTone.success,
      icon: Icons.check,
    ),
    'OVERDUE' => const StatusChip(
      label: 'Quá hạn',
      tone: ChipTone.danger,
      icon: Icons.warning_amber,
    ),
    _ => const StatusChip(label: 'Đang thuê', tone: ChipTone.info),
  };

  static StatusChip payment(Object? remain, Object? paid) {
    final r = num.tryParse('$remain') ?? 0;
    final p = num.tryParse('$paid') ?? 0;
    if (r <= 0) {
      return const StatusChip(
        label: 'Đã thanh toán',
        tone: ChipTone.success,
        icon: Icons.check,
      );
    }
    if (p > 0) {
      return const StatusChip(
        label: 'Thanh toán một phần',
        tone: ChipTone.warning,
      );
    }
    return const StatusChip(label: 'Chưa thanh toán', tone: ChipTone.danger);
  }

  static String alertType(String? type) => switch (type) {
    'LOW_STOCK' => 'Tồn kho thấp',
    'RENT_DUE' => 'Sắp hết hạn thuê',
    'PENDING_APPROVAL' => 'Chờ phê duyệt',
    'DEBT_OVERDUE' => 'Công nợ quá hạn',
    'TRANSFER_PENDING' => 'Chuyển kho chờ nhận',
    _ => type ?? '—',
  };
}
