import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../config/env.dart';
import '../network/api_exception.dart';
import '../providers.dart';
import 'session.dart';

/// Trạng thái đăng nhập. `null` = chưa đăng nhập.
class SessionController extends AsyncNotifier<Session?> {
  /// Mở app: token nằm trong cookie HttpOnly (FE không đọc được), nên hỏi thẳng BFF phiên hiện tại;
  /// 401 (và làm mới token cũng thất bại) = chưa đăng nhập.
  @override
  Future<Session?> build() async {
    final api = ref.watch(apiClientProvider);
    api.onSessionExpired = null; // không đổi state khi đang build
    Session? session;
    if (!Env.demoMode) {
      try {
        session = await _loadSession();
      } on ApiException catch (e) {
        if (!e.isUnauthorized) rethrow;
      }
    }
    api.onSessionExpired = () => state = const AsyncData(null);
    return session;
  }

  Future<Session> _loadSession() async {
    final api = ref.read(apiClientProvider);
    final result = await api.get('/bff/session');
    return Session.fromJson(Map<String, dynamic>.from(result as Map));
  }

  /// [remember] = "Ghi nhớ đăng nhập": false thì phiên hết khi đóng trình duyệt.
  /// BFF đặt token vào cookie HttpOnly, body trả về không có token.
  Future<void> login(
    String username,
    String password, {
    bool remember = true,
  }) async {
    final api = ref.read(apiClientProvider);
    await api.post(
      '/bff/auth/login',
      data: {
        'username': username.trim(),
        'password': password,
        'remember': remember,
      },
    );
    state = AsyncData(await _loadSession());
  }

  /// Báo server thu hồi mọi token của user (đăng xuất mọi thiết bị) và xoá cookie.
  /// Server lỗi hoặc mất mạng thì vẫn về màn đăng nhập trên máy này.
  Future<void> logout() async {
    final api = ref.read(apiClientProvider);
    try {
      await api.post('/bff/auth/logout');
    } on ApiException catch (e) {
      debugPrint('logout: $e');
    }
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
