import 'role.dart';

class WarehouseOption {
  const WarehouseOption({
    required this.id,
    required this.code,
    required this.name,
    required this.isDefault,
  });

  final int id;
  final String code;
  final String name;
  final bool isDefault;

  factory WarehouseOption.fromJson(Map<String, dynamic> json) =>
      WarehouseOption(
        id: (json['id'] as num).toInt(),
        code: json['code'] as String? ?? '',
        name: json['name'] as String? ?? '',
        isDefault: json['isDefault'] as bool? ?? false,
      );
}

/// Kết quả GET /bff/session.
class Session {
  const Session({
    required this.userId,
    required this.username,
    required this.fullName,
    required this.email,
    required this.role,
    required this.canViewAllWarehouses,
    required this.warehouses,
    required this.defaultWarehouseId,
    required this.unreadAlerts,
  });

  final int userId;
  final String username;
  final String fullName;
  final String? email;
  final AppRole role;
  final bool canViewAllWarehouses;
  final List<WarehouseOption> warehouses;
  final int? defaultWarehouseId;
  final int unreadAlerts;

  String get displayName => fullName.isNotEmpty ? fullName : username;

  /// "Nguyễn Văn A" → "NA" (avatar trên header).
  String get initials {
    final parts = displayName.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return '?';
    if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
    return (parts.first[0] + parts.last[0]).toUpperCase();
  }

  factory Session.fromJson(Map<String, dynamic> json) => Session(
    userId: (json['userId'] as num).toInt(),
    username: json['username'] as String? ?? '',
    fullName: json['fullName'] as String? ?? '',
    email: json['email'] as String?,
    role: AppRole.fromCode(json['role'] as String?),
    canViewAllWarehouses: json['canViewAllWarehouses'] as bool? ?? false,
    warehouses: ((json['warehouses'] as List?) ?? const [])
        .map(
          (e) => WarehouseOption.fromJson(Map<String, dynamic>.from(e as Map)),
        )
        .toList(),
    defaultWarehouseId: (json['defaultWarehouseId'] as num?)?.toInt(),
    unreadAlerts: (json['unreadAlerts'] as num?)?.toInt() ?? 0,
  );

  Session copyWith({int? unreadAlerts}) => Session(
    userId: userId,
    username: username,
    fullName: fullName,
    email: email,
    role: role,
    canViewAllWarehouses: canViewAllWarehouses,
    warehouses: warehouses,
    defaultWarehouseId: defaultWarehouseId,
    unreadAlerts: unreadAlerts ?? this.unreadAlerts,
  );
}
