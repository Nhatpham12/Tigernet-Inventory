import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../network/api_exception.dart';
import '../providers.dart';
import 'session.dart';

/// Trạng thái đăng nhập. `null` = chưa đăng nhập.
class SessionController extends AsyncNotifier<Session?> {
  @override
  Future<Session?> build() async {
    final api = ref.watch(apiClientProvider);
    api.onSessionExpired = () => state = const AsyncData(null);
    final token = await api.tokens.readAccessToken();
    if (token == null) return null;
    try {
      return await _loadSession();
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await api.tokens.clear();
        return null;
      }
      rethrow;
    }
  }

  Future<Session> _loadSession() async {
    final api = ref.read(apiClientProvider);
    final result = await api.get('/bff/session');
    return Session.fromJson(Map<String, dynamic>.from(result as Map));
  }

  Future<void> login(String username, String password) async {
    final api = ref.read(apiClientProvider);
    final result = await api.post(
      '/bff/auth/login',
      data: {'username': username.trim(), 'password': password},
    );
    final tokens = Map<String, dynamic>.from(result as Map);
    await api.tokens.save(
      accessToken: tokens['token'] as String,
      refreshToken: tokens['refreshToken'] as String?,
    );
    state = AsyncData(await _loadSession());
  }

  Future<void> logout() async {
    await ref.read(apiClientProvider).tokens.clear();
    state = const AsyncData(null);
  }

  /// Cập nhật số chuông sau khi đánh dấu đã đọc cảnh báo.
  Future<void> refreshUnread() async {
    final current = state.value;
    if (current == null) return;
    try {
      final count = await ref
          .read(apiClientProvider)
          .get('/bff/api/alerts/unread-count');
      state = AsyncData(
        current.copyWith(unreadAlerts: (count as num?)?.toInt() ?? 0),
      );
    } on ApiException catch (e) {
      debugPrint('refreshUnread: $e');
    }
  }
}

final sessionProvider = AsyncNotifierProvider<SessionController, Session?>(
  SessionController.new,
);

/// Kho đang làm việc (bộ chọn trên header). `null` = Tất cả kho.
class WarehouseScope extends Notifier<int?> {
  @override
  int? build() => ref.watch(sessionProvider).value?.defaultWarehouseId;

  void select(int? warehouseId) => state = warehouseId;
}

final warehouseScopeProvider = NotifierProvider<WarehouseScope, int?>(
  WarehouseScope.new,
);
