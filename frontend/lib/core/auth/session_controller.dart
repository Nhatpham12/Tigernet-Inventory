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
  /// Trả về [MfaChallenge] khi Quản trị còn bước mã 6 số (chưa đăng nhập xong); null = đã đăng nhập.
  Future<MfaChallenge?> login(
    String username,
    String password, {
    bool remember = true,
  }) async {
    final api = ref.read(apiClientProvider);
    final result = Map<String, dynamic>.from(
      (await api.post(
                '/bff/auth/login',
                data: {
                  'username': username.trim(),
                  'password': password,
                  'remember': remember,
                },
              )
              as Map?) ??
          const {},
    );
    final mfaToken = result['mfaToken'] as String?;
    if (result['authenticated'] != true && mfaToken != null) {
      return MfaChallenge(
        token: mfaToken,
        setupRequired: result['mfaSetupRequired'] == true,
        remember: remember,
      );
    }
    await finishLogin();
    return null;
  }

  /// Cài xác thực 2 bước lần đầu: khoá Base32 + link otpauth:// để vẽ QR.
  Future<({String secret, String otpauthUri})> mfaSetup(
    MfaChallenge challenge,
  ) async {
    final r = Map<String, dynamic>.from(
      await ref
              .read(apiClientProvider)
              .post('/bff/auth/mfa/setup', data: {'mfaToken': challenge.token})
          as Map,
    );
    return (
      secret: r['secret'] as String,
      otpauthUri: r['otpauthUri'] as String,
    );
  }

  /// Gửi mã 6 số (hoặc mã khôi phục). BFF đặt cookie token khi đúng.
  /// Lần cài đầu trả 8 mã khôi phục để hiện cho người dùng TRƯỚC khi gọi [finishLogin].
  Future<List<String>> mfaSubmit(
    MfaChallenge challenge, {
    String? code,
    String? recoveryCode,
  }) async {
    final r = Map<String, dynamic>.from(
      await ref
              .read(apiClientProvider)
              .post(
                challenge.setupRequired
                    ? '/bff/auth/mfa/confirm'
                    : '/bff/auth/mfa/verify',
                data: {
                  'mfaToken': challenge.token,
                  'code': code,
                  'recoveryCode': recoveryCode,
                  'remember': challenge.remember,
                },
              )
          as Map,
    );
    return [for (final c in (r['recoveryCodes'] as List?) ?? const []) '$c'];
  }

  /// Đã có cookie token: tải phiên và vào app.
  Future<void> finishLogin() async {
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

/// Quản trị đã đúng mật khẩu, còn bước mã 6 số. [token] sống 5 phút, chỉ dùng cho /bff/auth/mfa/**.
class MfaChallenge {
  const MfaChallenge({
    required this.token,
    required this.setupRequired,
    required this.remember,
  });

  final String token;

  /// true = lần đầu, phải quét QR cài app Authenticator.
  final bool setupRequired;
  final bool remember;
}
