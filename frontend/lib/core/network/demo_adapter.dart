import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart' show rootBundle;

/// Chế độ demo (`--dart-define=DEMO_MODE=true`): thay BFF bằng dữ liệu ghi sẵn
/// trong `assets/demo/fixtures.json` để chạy Flutter Web mà không cần server.
///
/// - GET khớp đúng đường dẫn đã ghi (session, dashboard, `/bff/api/**`).
/// - Danh sách phân trang: lọc, tìm kiếm, sắp xếp, phân trang ngay trên máy.
/// - POST/PUT/DELETE: trả về thành công nhưng không lưu gì.
class DemoAdapter implements HttpClientAdapter {
  Map<String, dynamic>? _routes;
  Map<String, dynamic>? _lists;

  Future<void> _load() async {
    if (_routes != null) return;
    final raw =
        jsonDecode(await rootBundle.loadString('assets/demo/fixtures.json'))
            as Map<String, dynamic>;
    _routes = Map<String, dynamic>.from(raw['routes'] as Map);
    _lists = Map<String, dynamic>.from(raw['lists'] as Map);
  }

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    await _load();
    // giả lập độ trễ mạng để thấy trạng thái đang tải
    await Future<void>.delayed(const Duration(milliseconds: 150));
    final path = options.uri.path;
    final query = options.uri.queryParameters;
    final method = options.method.toUpperCase();

    if (path.startsWith('/bff/auth/')) {
      return _json({
        'code': 1000,
        'result': {
          'token': 'demo-access-token',
          'refreshToken': 'demo-refresh-token',
          'expiresIn': 3600,
          'authenticated': true,
        },
      });
    }

    if (method != 'GET') {
      final data = options.data;
      return _json({
        'code': 1000,
        'result': data is Map ? {'id': 9999, ...data} : null,
      });
    }

    final wh = query['warehouseId'];
    final exact =
        _routes![wh == null ? path : '$path?warehouseId=$wh'] ?? _routes![path];
    if (exact != null) return _json(exact);

    if (_lists!.containsKey(path)) {
      final source =
          (_lists![wh == null ? path : '$path?warehouseId=$wh'] ??
                  _lists![path])
              as List;
      return _json({'code': 1000, 'result': _page(source, query)});
    }

    if (path.startsWith('/bff/api/')) {
      return _json({'code': 1000, 'result': <Object>[]});
    }
    return _json({
      'code': 1000,
      'result': {
        'items': <Object>[],
        'page': 1,
        'size': 20,
        'totalItems': 0,
        'totalPages': 1,
      },
    });
  }

  static Map<String, dynamic> _page(List source, Map<String, String> q) {
    var items = source.cast<Map<String, dynamic>>().where((it) {
      for (final MapEntry(:key, :value) in q.entries) {
        switch (key) {
          case 'page' || 'size' || 'sort' || 'warehouseId':
            continue;
          case 'keyword':
            final kw = value.toLowerCase();
            final hit = it.values.any(
              (v) => v is String && v.toLowerCase().contains(kw),
            );
            if (!hit) return false;
          case 'state':
            final s = it['rentalState'];
            if (value == 'NOT_RETURNED' ? s == 'RETURNED' : s != value) {
              return false;
            }
          case 'onlyOutstanding':
            if (value == 'true' && ((it['remain'] as num?) ?? 0) <= 0) {
              return false;
            }
          case 'from' || 'to':
            final d =
                '${it['dateIn'] ?? it['dateOut'] ?? it['createdAt'] ?? ''}';
            if (d.isEmpty) continue;
            final bound = value.length >= 10 ? value.substring(0, 10) : value;
            final day = d.length >= 10 ? d.substring(0, 10) : d;
            if (key == 'from' && day.compareTo(bound) < 0) return false;
            if (key == 'to' && day.compareTo(bound) > 0) return false;
          default:
            final field = key == 'type' ? 'rentalType' : key;
            if (it.containsKey(field) && '${it[field]}' != value) return false;
        }
      }
      return true;
    }).toList();

    final sort = q['sort'];
    if (sort != null && sort.isNotEmpty) {
      final parts = sort.split(',');
      final field = parts.first;
      final desc = parts.length > 1 && parts[1].toLowerCase() == 'desc';
      items.sort((a, b) {
        final x = a[field], y = b[field];
        final c = (x is num && y is num)
            ? x.compareTo(y)
            : '${x ?? ''}'.toLowerCase().compareTo('${y ?? ''}'.toLowerCase());
        return desc ? -c : c;
      });
    }

    final size = int.tryParse(q['size'] ?? '') ?? 20;
    final total = items.length;
    final pages = total == 0 ? 1 : (total / size).ceil();
    final page = (int.tryParse(q['page'] ?? '') ?? 1).clamp(1, pages);
    final from = (page - 1) * size;
    items = items.sublist(from, (from + size).clamp(0, total));
    return {
      'items': items,
      'page': page,
      'size': size,
      'totalItems': total,
      'totalPages': pages,
    };
  }

  static ResponseBody _json(Object body) => ResponseBody.fromString(
    jsonEncode(body),
    200,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );

  @override
  void close({bool force = false}) {}
}
