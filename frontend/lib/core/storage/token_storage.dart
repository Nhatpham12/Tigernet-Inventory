import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Lưu access + refresh token (Web dùng IndexedDB mã hóa của flutter_secure_storage).
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
    : _storage = storage ?? const FlutterSecureStorage();

  static const _accessKey = 'tigernet.access_token';
  static const _refreshKey = 'tigernet.refresh_token';

  final FlutterSecureStorage _storage;
  String? _accessCache;

  Future<String?> readAccessToken() async =>
      _accessCache ??= await _storage.read(key: _accessKey);

  Future<String?> readRefreshToken() => _storage.read(key: _refreshKey);

  Future<void> save({required String accessToken, String? refreshToken}) async {
    _accessCache = accessToken;
    await _storage.write(key: _accessKey, value: accessToken);
    if (refreshToken != null) {
      await _storage.write(key: _refreshKey, value: refreshToken);
    }
  }

  Future<void> clear() async {
    _accessCache = null;
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
