import 'package:dio/dio.dart';

import '../config/env.dart';
import '../storage/token_storage.dart';
import 'api_exception.dart';
import 'page_data.dart';

/// Điểm gọi API duy nhất của FE. Mọi request đi tới BFF (`/bff/**`), không gọi thẳng backend.
class ApiClient {
  ApiClient({required TokenStorage tokenStorage, String? baseUrl})
    : _tokens = tokenStorage,
      _dio = Dio(
        BaseOptions(
          baseUrl: baseUrl ?? Env.bffBaseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 30),
          contentType: Headers.jsonContentType,
        ),
      ) {
    _dio.interceptors.add(_AuthInterceptor(this));
  }

  final Dio _dio;
  final TokenStorage _tokens;

  /// Gọi khi refresh token cũng hết hạn → SessionController chuyển về màn đăng nhập.
  void Function()? onSessionExpired;

  TokenStorage get tokens => _tokens;

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) =>
      _send(() => _dio.get(path, queryParameters: _clean(query)));

  Future<dynamic> post(String path, {Object? data}) =>
      _send(() => _dio.post(path, data: data));

  Future<dynamic> put(String path, {Object? data}) =>
      _send(() => _dio.put(path, data: data));

  Future<dynamic> delete(String path) => _send(() => _dio.delete(path));

  /// GET một danh sách đã phân trang từ BFF.
  Future<PageData<Map<String, dynamic>>> getPage(
    String path, {
    Map<String, dynamic>? query,
  }) async {
    final result = await get(path, query: query);
    return PageData.fromJson(
      Map<String, dynamic>.from(result as Map),
      (e) => e,
    );
  }

  /// GET một danh sách không phân trang (qua proxy /bff/api/**).
  Future<List<Map<String, dynamic>>> getList(
    String path, {
    Map<String, dynamic>? query,
  }) async {
    final result = await get(path, query: query);
    return ((result as List?) ?? const [])
        .map((e) => Map<String, dynamic>.from(e as Map))
        .toList();
  }

  Future<Map<String, dynamic>?> refreshTokens() async {
    final refresh = await _tokens.readRefreshToken();
    if (refresh == null) return null;
    try {
      final res = await _dio.post(
        '/bff/auth/refresh',
        data: {'refreshToken': refresh},
        options: Options(extra: {_AuthInterceptor.skipAuth: true}),
      );
      final result = Map<String, dynamic>.from(res.data['result'] as Map);
      await _tokens.save(
        accessToken: result['token'] as String,
        refreshToken: result['refreshToken'] as String?,
      );
      return result;
    } on DioException {
      return null;
    }
  }

  Future<dynamic> _send(Future<Response<dynamic>> Function() request) async {
    try {
      final res = await request();
      final body = res.data;
      if (body is Map && body.containsKey('code')) {
        return body['result'];
      }
      return body;
    } on DioException catch (e) {
      throw _toApiException(e);
    }
  }

  static Map<String, dynamic>? _clean(Map<String, dynamic>? query) {
    if (query == null) return null;
    return Map.of(query)
      ..removeWhere((_, v) => v == null || (v is String && v.trim().isEmpty));
  }

  static ApiException _toApiException(DioException e) {
    final res = e.response;
    if (res == null) {
      return ApiException(
        statusCode: 0,
        code: 9503,
        message: kErrorMessages[9503]!,
      );
    }
    final data = res.data;
    int? code;
    String? message;
    var fieldErrors = <String, String>{};
    if (data is Map) {
      code = (data['code'] as num?)?.toInt();
      message = data['message'] as String?;
      if (data['errors'] is Map) {
        fieldErrors = Map<String, String>.from(
          (data['errors'] as Map).map((k, v) => MapEntry('$k', '$v')),
        );
      }
    }
    final status = res.statusCode ?? 0;
    final vi =
        (code != null ? kErrorMessages[code] : null) ??
        switch (status) {
          401 => 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
          403 => 'Bạn không có quyền thực hiện thao tác này.',
          404 => 'Không tìm thấy dữ liệu.',
          409 => 'Trạng thái phiếu không cho phép thao tác này.',
          >= 500 => 'Máy chủ gặp lỗi, vui lòng thử lại sau.',
          _ => message ?? 'Dữ liệu không hợp lệ.',
        };
    return ApiException(
      statusCode: status,
      code: code,
      message: vi,
      fieldErrors: fieldErrors,
    );
  }
}

/// Gắn access token; gặp 401 thì làm mới token một lần rồi gửi lại request.
class _AuthInterceptor extends QueuedInterceptor {
  _AuthInterceptor(this._client);

  static const skipAuth = 'skipAuth';
  static const _retried = 'retried';
  final ApiClient _client;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (options.extra[skipAuth] != true) {
      final token = await _client._tokens.readAccessToken();
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final options = err.requestOptions;
    final isAuthCall = options.path.startsWith('/bff/auth/');
    if (err.response?.statusCode != 401 ||
        isAuthCall ||
        options.extra[skipAuth] == true ||
        options.extra[_retried] == true) {
      return handler.next(err);
    }
    final refreshed = await _client.refreshTokens();
    if (refreshed == null) {
      await _client._tokens.clear();
      _client.onSessionExpired?.call();
      return handler.next(err);
    }
    options.extra[_retried] = true;
    options.headers['Authorization'] = 'Bearer ${refreshed['token']}';
    try {
      handler.resolve(await _client._dio.fetch(options));
    } on DioException catch (e) {
      handler.next(e);
    }
  }
}
