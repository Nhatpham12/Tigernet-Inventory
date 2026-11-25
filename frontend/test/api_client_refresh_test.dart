import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tigernet_app/core/network/api_client.dart';
import 'package:tigernet_app/core/network/api_exception.dart';

/// Trả status cố định cho từng path, ghi lại các path đã gọi.
class _FakeAdapter implements HttpClientAdapter {
  _FakeAdapter(this.statusByPath);
  final Map<String, int> statusByPath;
  final calls = <String>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    calls.add(options.path);
    final status = statusByPath[options.path] ?? 200;
    return ResponseBody.fromString(
      jsonEncode({'code': status, 'message': 'x'}),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  test(
    'chưa đăng nhập: session 401 + refresh 401 → báo 401, không treo',
    () async {
      final adapter = _FakeAdapter({
        '/bff/session': 401,
        '/bff/auth/refresh': 401,
      });
      final api = ApiClient(baseUrl: 'http://x', adapter: adapter);
      var expired = false;
      api.onSessionExpired = () => expired = true;

      await expectLater(
        api.get('/bff/session').timeout(const Duration(seconds: 5)),
        throwsA(
          isA<ApiException>().having((e) => e.isUnauthorized, '401', true),
        ),
      );
      expect(adapter.calls, ['/bff/session', '/bff/auth/refresh']);
      expect(expired, isTrue);
    },
  );

  test(
    'refresh OK nhưng session vẫn 401 → gửi lại đúng 1 lần rồi báo lỗi',
    () async {
      final adapter = _FakeAdapter({
        '/bff/session': 401,
        '/bff/auth/refresh': 200,
      });
      final api = ApiClient(baseUrl: 'http://x', adapter: adapter);

      await expectLater(
        api.get('/bff/session').timeout(const Duration(seconds: 5)),
        throwsA(isA<ApiException>()),
      );
      expect(adapter.calls, [
        '/bff/session',
        '/bff/auth/refresh',
        '/bff/session',
      ]);
    },
  );
}
