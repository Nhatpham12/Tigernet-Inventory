import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show visibleForTesting;

import '../config/env.dart';
import 'api_exception.dart';
import 'demo_adapter.dart';
import 'page_data.dart';

/// Điểm gọi API duy nhất của FE. Mọi request đi tới BFF (`/bff/**`), không gọi thẳng backend.
class ApiClient {
  ApiClient({String? baseUrl, @visibleForTesting HttpClientAdapter? adapter})
    : _dio = Dio(
        BaseOptions(
          baseUrl: baseUrl ?? Env.bffBaseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 30),
          contentType: Headers.jsonContentType,
          // BFF chặn request ghi không có header này (chống CSRF; trang web khác không tự thêm được)
          headers: {'X-TigerNet-Request': '1'},
          // Web: gửi kèm cookie token HttpOnly khi BFF khác cổng (dev). Nền tảng khác bỏ qua.
          extra: {'withCredentials': true},
        ),
      ) {
    if (Env.demoMode) _dio.httpClientAdapter = DemoAdapter();
    if (adapter != null) _dio.httpClientAdapter = adapter;
    _plainDio = Dio(_dio.options)..httpClientAdapter = _dio.httpClientAdapter;
    _dio.interceptors.add(_AuthInterceptor(this));
  }

  final Dio _dio;

  /// Cùng cấu hình nhưng KHÔNG có [_AuthInterceptor]: dùng cho làm mới token và gửi lại request
  /// từ bên trong interceptor. Gọi qua [_dio] ở đó sẽ treo vĩnh viễn khi gặp lỗi: QueuedInterceptor
  /// xếp onError của request mới sau onError đang chạy, mà onError đang chạy lại chờ request mới.
  late final Dio _plainDio;

  /// Gọi khi refresh token cũng hết hạn → SessionController chuyển về màn đăng nhập.
  void Function()? onSessionExpired;

  Future<dynamic> get(String path, {Map<String, dynamic>? query}) =>
      _send(() => _dio.get(path, queryParameters: _clean(query)));

  Future<dynamic> post(String path, {Object? data}) =>
      _send(() => _dio.post(path, data: data));

  Future<dynamic> put(String path, {Object? data}) =>
      _send(() => _dio.put(path, data: data));

  Future<dynamic> delete(String path) => _send(() => _dio.delete(path));

  /// GET một file (PDF in giấy tờ). Tên file lấy từ header Content-Disposition của backend.
  Future<DownloadedFile> getFile(
    String path, {
    Map<String, dynamic>? query,
    String fallbackName = 'tai_lieu.pdf',
  }) async {
    try {
      final res = await _dio.get<List<int>>(
        path,
        queryParameters: _clean(query),
        options: Options(responseType: ResponseType.bytes),
      );
      final name =
          fileNameFromDisposition(res.headers.value('content-disposition')) ??
          fallbackName;
      return DownloadedFile(
        name: name,
        bytes: Uint8List.fromList(res.data ?? const []),
        contentType: res.headers.value('content-type') ?? 'application/pdf',
      );
    } on DioException catch (e) {
      // lỗi trả về JSON nhưng đang đọc dạng bytes: giải mã lại để lấy mã lỗi
      final data = e.response?.data;
      if (data is List<int>) {
        try {
          e.response!.data = jsonDecode(utf8.decode(data));
        } catch (_) {}
      }
      throw _toApiException(e);
    }
  }

  /// filename*=UTF-8''... được ưu tiên hơn filename="...".
  static String? fileNameFromDisposition(String? header) {
    if (header == null) return null;
    final star = RegExp(
      r"filename\*=UTF-8''([^;]+)",
      caseSensitive: false,
    ).firstMatch(header);
    if (star != null) return Uri.decodeComponent(star.group(1)!.trim());
    final plain = RegExp(
      r'filename="?([^";]+)"?',
      caseSensitive: false,
    ).firstMatch(header);
    return plain?.group(1)?.trim();
  }

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

  /// Làm mới token (refresh token nằm trong cookie HttpOnly, BFF đặt lại cookie mới).
  /// Trả false khi server từ chối (hết hạn, bị thu hồi) → phải đăng nhập lại.
  /// Lỗi tạm thời (429 quá nhiều request, 5xx, mất mạng) thì ném DioException để giữ phiên đăng nhập.
  Future<bool> refreshTokens() async {
    try {
      await _plainDio.post('/bff/auth/refresh');
      return true;
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      if (status == 400 || status == 401 || status == 403) return false;
      rethrow;
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

/// Gặp 401 thì làm mới token (cookie) một lần rồi gửi lại request. Token nằm trong cookie HttpOnly
/// do BFF đặt, trình duyệt tự gửi kèm; FE không đọc, không lưu token.
class _AuthInterceptor extends QueuedInterceptor {
  _AuthInterceptor(this._client);

  static const _sentAt = 'sentAt';
  final ApiClient _client;

  /// Thời điểm làm mới token thành công gần nhất.
  DateTime? _lastRefresh;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    options.extra[_sentAt] = DateTime.now();
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final options = err.requestOptions;
    final isAuthCall = options.path.startsWith('/bff/auth/');
    if (err.response?.statusCode != 401 || isAuthCall) {
      return handler.next(err);
    }
    // Nhiều request cùng gặp 401 được xếp hàng (QueuedInterceptor): request gửi trước lần làm mới
    // vừa thành công thì chỉ cần gửi lại (cookie đã mới), không gọi /bff/auth/refresh thêm.
    final sentAt = options.extra[_sentAt] as DateTime?;
    final alreadyRefreshed =
        _lastRefresh != null &&
        sentAt != null &&
        sentAt.isBefore(_lastRefresh!);
    if (!alreadyRefreshed) {
      final bool ok;
      try {
        ok = await _client.refreshTokens();
      } on DioException {
        return handler.next(
          err,
        ); // lỗi tạm thời: giữ phiên, báo lỗi request này
      }
      if (!ok) {
        _client.onSessionExpired?.call();
        return handler.next(err);
      }
      _lastRefresh = DateTime.now();
    }
    try {
      handler.resolve(await _client._plainDio.fetch(options));
    } on DioException catch (e) {
      handler.next(e);
    }
  }
}

class DownloadedFile {
  const DownloadedFile({
    required this.name,
    required this.bytes,
    required this.contentType,
  });

  final String name;
  final Uint8List bytes;
  final String contentType;
}
