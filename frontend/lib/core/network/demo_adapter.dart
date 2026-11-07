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

    if (method == 'POST' && path == '/bff/api/payments') {
      final data = options.data is Map ? options.data as Map : const {};
      return _json({
        'code': 1000,
        'result': {
          'id': 9001,
          'paymentCode': data['direction'] == 'OUT'
              ? 'PC-2026-9001'
              : 'PT-2026-9001',
          'status': 'ACTIVE',
          ...data,
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

    // Phiếu thu/chi + đối chiếu công nợ (dữ liệu giả, không nằm trong fixtures.json)
    if (path == '/bff/payments') {
      return _json({'code': 1000, 'result': _page(_demoPayments, query)});
    }
    if (path == '/bff/api/reports/reconciliation') {
      return _json({'code': 1000, 'result': _demoReconciliation(query)});
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

  static final _demoPayments = <Map<String, dynamic>>[
    for (final (i, p) in [
      ('IN', 'Nguyễn Minh Anh', 'PX-HCM-2026-0002', 650000, 'CASH', 'ACTIVE'),
      (
        'IN',
        'Công ty An Phát',
        'PX-MAIN-2026-0004',
        4200000,
        'TRANSFER',
        'ACTIVE',
      ),
      (
        'OUT',
        'Công ty TNHH Viễn Thông Á Châu',
        'PN-MAIN-2026-0003',
        8500000,
        'TRANSFER',
        'ACTIVE',
      ),
      ('IN', 'Trường ĐH Bách Khoa', null, 1500000, 'CASH', 'CANCELLED'),
      (
        'OUT',
        'Công ty TNHH Viễn Thông Á Châu',
        null,
        2000000,
        'CASH',
        'ACTIVE',
      ),
    ].indexed)
      {
        'id': 101 + i,
        'paymentCode':
            '${p.$1 == 'IN' ? 'PT' : 'PC'}-2026-${(i + 1).toString().padLeft(4, '0')}',
        'direction': p.$1,
        'partyId': 3 + i,
        'partyName': p.$2,
        'partyType': p.$1 == 'IN' ? 'CUSTOMER' : 'SUPPLIER',
        'referenceType': p.$3 == null
            ? null
            : (p.$1 == 'IN' ? 'Export_Receipts' : 'Import_Receipts'),
        'referenceId': p.$3 == null ? null : 10 + i,
        'referenceCode': p.$3,
        'amount': p.$4,
        'method': p.$5,
        'paidAt': '2026-11-0${i + 1}T09:30:00',
        'status': p.$6,
        'note': null,
        'recordedByName': 'Nguyễn Văn A',
      },
  ];

  static Map<String, dynamic> _demoReconciliation(Map<String, String> q) {
    final from = q['from'] ?? '2026-11-01';
    return {
      'partyId': int.tryParse(q['partyId'] ?? ''),
      'partyName': 'Công ty An Phát',
      'from': from,
      'to': q['to'] ?? '2026-11-30',
      'openingBalance': 3000000,
      'increase': 6850000,
      'decrease': 4200000,
      'closingBalance': 5650000,
      'lines': [
        {
          'date': '2026-11-02',
          'code': 'PX-MAIN-2026-0004',
          'description': 'Xuất hàng theo phiếu xuất',
          'increase': 4200000,
          'decrease': 0,
        },
        {
          'date': '2026-11-03',
          'code': 'PT-2026-0002',
          'description': 'Thu tiền chuyển khoản',
          'increase': 0,
          'decrease': 4200000,
        },
        {
          'date': '2026-11-05',
          'code': 'PX-MAIN-2026-0007',
          'description': 'Xuất hàng theo phiếu xuất',
          'increase': 2650000,
          'decrease': 0,
        },
      ],
    };
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
