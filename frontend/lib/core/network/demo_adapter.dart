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

    final warranty = _warranty(method, path, query, options.data);
    if (warranty != null) return _json({'code': 1000, 'result': warranty});
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

  // ---- Bảo hành (dữ liệu giả, giữ trong bộ nhớ để thử luồng tiếp nhận/đổi trạng thái) ----
  static final List<Map<String, dynamic>> _sold = [
    _soldLine(
      1,
      'PX-HCM-2026-0001',
      'Router Wifi 6 AX3000',
      'SN-RT-0001',
      2,
      'Nguyễn Minh Anh',
      '2026-03-10',
      24,
    ),
    _soldLine(
      2,
      'PX-HCM-2026-0001',
      'Camera IP 4MP',
      'SN-CAM-0002',
      2,
      'Nguyễn Minh Anh',
      '2026-03-10',
      12,
    ),
    _soldLine(
      3,
      'PX-MAIN-2026-0007',
      'Switch 8 Port',
      'SN-SW-0003',
      1,
      'Công ty Hoàng Long',
      '2025-01-15',
      12,
    ),
    _soldLine(
      4,
      'PX-HN-2026-0003',
      'Bộ phát 4G',
      'SN-4G-0004',
      3,
      'Trần Quốc Bảo',
      '2026-08-20',
      18,
    ),
  ];

  static Map<String, dynamic> _soldLine(
    int id,
    String receipt,
    String product,
    String serial,
    int customerId,
    String customer,
    String dateOut,
    int months,
  ) {
    final d = DateTime.parse(dateOut);
    final expiry = DateTime(d.year, d.month + months, d.day);
    return {
      'exportDetailId': id,
      'exportReceiptId': id <= 2 ? 1 : id,
      'exportReceiptCode': receipt,
      'productId': id,
      'productName': product,
      'serial': serial,
      'quantity': 1,
      'customerId': customerId,
      'customerName': customer,
      'dateOut': '${dateOut}T09:00:00',
      'warrantyMonths': months,
      'warrantyExpiry': expiry.toIso8601String().substring(0, 10),
      'inWarranty': expiry.isAfter(DateTime.now()),
      'inventoryId': id,
    };
  }

  static final List<Map<String, dynamic>> _claims = [
    _claim(
      1,
      'BH-HCM-2026-0001',
      _sold[0],
      'SENT',
      '2026-10-28T10:00:00',
      'Router mất sóng 5GHz',
    ),
    _claim(
      2,
      'BH-HCM-2026-0002',
      _sold[1],
      'RECEIVED',
      '2026-11-05T14:30:00',
      'Camera không lên hình',
    ),
    _claim(
      3,
      'BH-MAIN-2026-0001',
      _sold[2],
      'REJECTED',
      '2026-11-02T09:15:00',
      'Cháy nguồn, hết hạn bảo hành',
    ),
    _claim(
      4,
      'BH-HN-2026-0001',
      _sold[3],
      'CLOSED',
      '2026-09-10T08:00:00',
      'Không nhận SIM',
    ),
  ];

  static Map<String, dynamic> _claim(
    int id,
    String code,
    Map<String, dynamic> sold,
    String status,
    String at,
    String note,
  ) => {
    'id': id,
    'claimCode': code,
    'exportDetailId': sold['exportDetailId'],
    'exportReceiptCode': sold['exportReceiptCode'],
    'customerId': sold['customerId'],
    'customerName': sold['customerName'],
    'productName': sold['productName'],
    'serial': sold['serial'],
    'receivedWarehouseId': 2,
    'receivedWarehouseName': 'Kho TP.HCM',
    'status': status,
    'receivedAt': at,
    'sentAt': status == 'RECEIVED' || status == 'REJECTED' ? null : at,
    'returnedAt': status == 'CLOSED' ? at : null,
    'result': status == 'CLOSED' ? 'REPAIRED' : null,
    'cost': status == 'CLOSED' ? 0 : null,
    'replacementSerial': null,
    'newWarrantyExpiry': null,
    'note': note,
    'handledByName': 'Nguyễn Văn A',
    'warrantyExpiry': sold['warrantyExpiry'],
  };

  /// Trả về null nếu không phải đường dẫn bảo hành.
  static Object? _warranty(
    String method,
    String path,
    Map<String, String> q,
    Object? data,
  ) {
    const base = '/bff/api/warranty-claims';
    if (path == '/bff/warranty-claims' && method == 'GET') {
      return _page(_claims, q);
    }
    if (path == '/bff/api/export-receipts' && method == 'GET') {
      return [
        {
          'id': 1,
          'receiptCode': 'PX-HCM-2026-0001',
          'customerName': 'Nguyễn Minh Anh',
        },
        {
          'id': 3,
          'receiptCode': 'PX-MAIN-2026-0007',
          'customerName': 'Công ty Hoàng Long',
        },
      ];
    }
    if (!path.startsWith(base)) return null;
    final rest = path.substring(base.length);
    if (rest == '/lookup') {
      if (q['exportReceiptId'] != null) {
        return _sold
            .where((s) => '${s['exportReceiptId']}' == q['exportReceiptId'])
            .toList();
      }
      return _sold.firstWhere(
        (s) =>
            s['serial'].toString().toLowerCase() ==
            (q['serial'] ?? '').toLowerCase(),
        orElse: () => _sold[0],
      );
    }
    if (rest.endsWith('/print')) return null;
    if (rest.isEmpty) {
      if (method == 'POST' && data is Map) {
        final sold = _sold.firstWhere(
          (s) => s['exportDetailId'] == data['exportDetailId'],
        );
        final id = _claims.length + 1;
        final c = _claim(
          id,
          'BH-HCM-2026-${id.toString().padLeft(4, '0')}',
          sold,
          'RECEIVED',
          '${data['receivedAt'] ?? DateTime.now().toIso8601String()}',
          '${data['note'] ?? ''}',
        );
        _claims.add(c);
        return c;
      }
      return _claims.where((c) {
        for (final k in ['status', 'customerId']) {
          if (q[k] != null && '${c[k]}' != q[k]) return false;
        }
        return true;
      }).toList();
    }
    final m = RegExp(r'^/(\d+)(/status)?$').firstMatch(rest);
    if (m == null) return null;
    final claim = _claims.firstWhere((c) => '${c['id']}' == m.group(1));
    if (m.group(2) != null && data is Map) {
      final now = DateTime.now().toIso8601String();
      claim['status'] = data['status'];
      if (data['status'] == 'SENT') claim['sentAt'] = now;
      if (data['status'] == 'RETURNED') {
        claim['returnedAt'] = now;
        for (final k in [
          'result',
          'cost',
          'replacementSerial',
          'newWarrantyExpiry',
        ]) {
          claim[k] = data[k];
        }
      }
    }
    return claim;
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
