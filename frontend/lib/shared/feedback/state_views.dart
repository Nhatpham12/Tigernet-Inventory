import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';

/// Trạng thái chung theo Figma "Trạng thái / error | 403 | 404 | Đang tải".
class StateView extends StatelessWidget {
  const StateView({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.action,
    this.tone,
  });

  final IconData icon;
  final String title;
  final String? message;
  final Widget? action;
  final Color? tone;

  factory StateView.error(Object error, {VoidCallback? onRetry}) {
    if (error is ApiException && error.isForbidden) {
      return const StateView(
        icon: Icons.lock_outline,
        title: 'Bạn không có quyền truy cập',
        message: 'Liên hệ quản trị viên nếu bạn cần quyền xem nội dung này.',
      );
    }
    return StateView(
      icon: Icons.cloud_off_outlined,
      title: 'Không tải được dữ liệu',
      message: userMessage(error),
      action: onRetry == null
          ? null
          : OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 18),
              label: const Text('Thử lại'),
            ),
    );
  }

  const StateView.empty({super.key, this.message, this.action})
    : icon = Icons.inventory_2_outlined,
      title = 'Chưa có dữ liệu',
      tone = null;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppSpace.s32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                color: c.surfaceMuted,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Icon(icon, size: 32, color: tone ?? c.muted),
            ),
            const SizedBox(height: AppSpace.s16),
            Text(title, style: context.text.h2, textAlign: TextAlign.center),
            if (message != null) ...[
              const SizedBox(height: AppSpace.s8),
              ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Text(
                  message!,
                  textAlign: TextAlign.center,
                  style: context.text.body1.copyWith(color: c.muted),
                ),
              ),
            ],
            if (action != null) ...[
              const SizedBox(height: AppSpace.s16),
              action!,
            ],
          ],
        ),
      ),
    );
  }
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key});

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(32),
      child: CircularProgressIndicator(color: context.colors.accent),
    ),
  );
}

class ForbiddenPage extends StatelessWidget {
  const ForbiddenPage({super.key});

  @override
  Widget build(BuildContext context) => StateView(
    icon: Icons.lock_outline,
    title: 'Bạn không có quyền truy cập',
    message: 'Trang này không thuộc vai trò của bạn.',
    action: ElevatedButton(
      onPressed: () => context.go('/app/dashboard'),
      child: const Text('Về Tổng quan'),
    ),
  );
}

class NotFoundPage extends StatelessWidget {
  const NotFoundPage({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    body: StateView(
      icon: Icons.search_off,
      title: 'Không tìm thấy nội dung',
      message: 'Đường dẫn không tồn tại hoặc đã bị xóa.',
      action: ElevatedButton(
        onPressed: () => context.go('/app/dashboard'),
        child: const Text('Về Tổng quan'),
      ),
    ),
  );
}
