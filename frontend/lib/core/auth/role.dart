/// 5 vai trò theo DB (Roles.Role_Name).
enum AppRole {
  admin('ADMIN', 'Quản trị viên'),
  manager('MANAGER', 'Quản lý'),
  warehouse('WAREHOUSE', 'Thủ kho'),
  accountant('ACCOUNTANT', 'Kế toán'),
  sales('SALES', 'Bán hàng'),
  unknown('', 'Người dùng');

  const AppRole(this.code, this.label);

  final String code;
  final String label;

  static AppRole fromCode(String? code) => AppRole.values.firstWhere(
    (r) => r.code == code,
    orElse: () => AppRole.unknown,
  );
}
