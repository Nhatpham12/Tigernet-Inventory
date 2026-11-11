import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../config/env.dart';

/// Lưu access + refresh token (Web dùng IndexedDB mã hóa của flutter_secure_storage).
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
    : _storage = storage ?? const FlutterSecureStorage();

  static const _accessKey = 'tigernet.access_token';
  static const _refreshKey = 'tigernet.refresh_token';

  final FlutterSecureStorage _storage;
  String? _accessCache;

  String? _refreshCache;

  // Bản demo chỉ giữ token trong bộ nhớ: trang demo có thể chạy trong khung bị chặn storage.
  static const _memoryOnly = Env.demoMode;

  /// true khi người dùng bỏ chọn "Ghi nhớ đăng nhập": token chỉ sống trong phiên này.
  bool _sessionOnly = false;

  Future<String?> readAccessToken() async => _memoryOnly || _sessionOnly
      ? _accessCache
      : _accessCache ??= await _storage.read(key: _accessKey);

  Future<String?> readRefreshToken() async => _memoryOnly || _sessionOnly
      ? _refreshCache
      : _refreshCache ?? await _storage.read(key: _refreshKey);

  /// [persist] = "Ghi nhớ đăng nhập". false thì không ghi xuống storage
  /// (đóng/tải lại trang là phải đăng nhập lại); refresh token khi đang dùng vẫn giữ phiên này.
  Future<void> save({
    required String accessToken,
    String? refreshToken,
    bool? persist,
  }) async {
    if (persist != null) _sessionOnly = !persist;
    _accessCache = accessToken;
    if (refreshToken != null) _refreshCache = refreshToken;
    if (_memoryOnly) return;
    if (_sessionOnly) {
      await _storage.delete(key: _accessKey);
      await _storage.delete(key: _refreshKey);
      return;
    }
    await _storage.write(key: _accessKey, value: accessToken);
    if (refreshToken != null) {
      await _storage.write(key: _refreshKey, value: refreshToken);
    }
  }

  Future<void> clear() async {
    _accessCache = null;
    _refreshCache = null;
    _sessionOnly = false;
    if (_memoryOnly) return;
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
