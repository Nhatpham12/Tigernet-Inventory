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

  Future<String?> readAccessToken() async => _memoryOnly
      ? _accessCache
      : _accessCache ??= await _storage.read(key: _accessKey);

  Future<String?> readRefreshToken() async =>
      _memoryOnly ? _refreshCache : _storage.read(key: _refreshKey);

  Future<void> save({required String accessToken, String? refreshToken}) async {
    _accessCache = accessToken;
    if (refreshToken != null) _refreshCache = refreshToken;
    if (_memoryOnly) return;
    await _storage.write(key: _accessKey, value: accessToken);
    if (refreshToken != null) {
      await _storage.write(key: _refreshKey, value: refreshToken);
    }
  }

  Future<void> clear() async {
    _accessCache = null;
    _refreshCache = null;
    if (_memoryOnly) return;
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
