import 'package:flutter/material.dart';

import '../auth/role.dart';

class MenuLink {
  const MenuLink(this.label, this.path, {this.roles});
  final String label;
  final String path;

  /// null = mọi vai trò
  final Set<AppRole>? roles;
}

class MenuGroup {
  const MenuGroup({
    required this.label,
    required this.icon,
    this.path,
    this.children = const [],
    this.roles,
  });

  final String label;
  final IconData icon;
  final String? path;
  final List<MenuLink> children;
  final Set<AppRole>? roles;
}

/// Một nghiệp vụ = một icon, dùng chung cho sidebar, Dashboard và nơi khác.
class AppIcons {
  const AppIcons._();

  static const dashboard = Icons.home_outlined;
  static const products = Icons.inventory_2_outlined;
  static const importGoods = Icons.move_to_inbox_outlined;
  static const exportGoods = Icons.local_shipping_outlined;
  static const transfer = Icons.swap_horiz;
  static const rental = Icons.vpn_key_outlined;
  static const lend = Icons.handshake_outlined;
  static const warehouse = Icons.warehouse_outlined;
  static const reports = Icons.bar_chart;
  static const users = Icons.group_outlined;
  static const settings = Icons.settings_outlined;
}

const _tk = AppRole.warehouse;
const _kt = AppRole.accountant;
const _bh = AppRole.sales;
const _ql = AppRole.manager;
const _qt = AppRole.admin;

/// Menu bên trái theo Figma (ERP / Sidebar) + ma trận quyền ở Frontend_Structure.md §3.
/// Chỉ để ẩn/hiện; backend vẫn là nơi chặn quyền.
const kMainMenu = <MenuGroup>[
  MenuGroup(
    label: 'Tổng quan',
    icon: AppIcons.dashboard,
    path: '/app/dashboard',
  ),
  MenuGroup(
    label: 'Sản phẩm',
    icon: AppIcons.products,
    children: [
      MenuLink('Danh sách sản phẩm', '/app/products'),
      MenuLink(
        'Danh mục sản phẩm',
        '/app/product-groups',
        roles: {_tk, _ql, _qt},
      ),
      MenuLink('Thêm sản phẩm', '/app/products/new', roles: {_tk, _ql, _qt}),
    ],
  ),
  MenuGroup(
    label: 'Nhập hàng',
    icon: AppIcons.importGoods,
    roles: {_tk, _kt, _ql, _qt},
    children: [
      MenuLink('Danh sách phiếu nhập', '/app/import-receipts'),
      MenuLink(
        'Tạo phiếu nhập',
        '/app/import-receipts/new',
        roles: {_tk, _ql, _qt},
      ),
      MenuLink('Danh sách nhà cung cấp', '/app/suppliers'),
      MenuLink(
        'Thêm nhà cung cấp',
        '/app/suppliers/new',
        roles: {_tk, _ql, _qt},
      ),
    ],
  ),
  MenuGroup(
    label: 'Xuất kho',
    icon: AppIcons.exportGoods,
    roles: {_tk, _kt, _bh, _ql, _qt},
    children: [
      MenuLink('Danh sách phiếu xuất', '/app/export-receipts'),
      MenuLink(
        'Tạo phiếu xuất',
        '/app/export-receipts/new',
        roles: {_bh, _ql, _qt},
      ),
      MenuLink('Khách hàng', '/app/customers', roles: {_bh, _ql, _qt, _kt}),
      MenuLink(
        'Danh sách đổi trả',
        '/app/sales-returns',
        roles: {_bh, _ql, _qt},
      ),
    ],
  ),
  MenuGroup(
    label: 'Chuyển kho',
    icon: AppIcons.transfer,
    roles: {_tk, _ql, _qt},
    children: [MenuLink('Danh sách phiếu chuyển kho', '/app/transfers')],
  ),
  MenuGroup(
    label: 'Cho thuê',
    icon: AppIcons.rental,
    roles: {_tk, _bh, _ql, _qt},
    children: [
      MenuLink('Danh sách phiếu thuê', '/app/rentals'),
      MenuLink(
        'Tạo phiếu cho thuê',
        '/app/rentals/new',
        roles: {_bh, _ql, _qt},
      ),
    ],
  ),
  MenuGroup(
    label: 'Cho mượn',
    icon: AppIcons.lend,
    roles: {_tk, _bh, _ql, _qt},
    children: [
      MenuLink('Danh sách phiếu mượn', '/app/lends'),
      MenuLink('Tạo phiếu cho mượn', '/app/lends/new', roles: {_bh, _ql, _qt}),
    ],
  ),
  MenuGroup(
    label: 'Kho',
    icon: AppIcons.warehouse,
    roles: {_tk, _kt, _ql, _qt},
    children: [
      MenuLink('Danh sách kho', '/app/warehouses', roles: {_ql, _qt}),
      MenuLink('Kiểm kê', '/app/stocktakes', roles: {_tk, _ql, _qt}),
    ],
  ),
  MenuGroup(
    label: 'Báo cáo',
    icon: AppIcons.reports,
    roles: {_tk, _kt, _ql, _qt},
    children: [
      MenuLink('Tồn kho', '/app/reports/stock'),
      MenuLink(
        'Nhập xuất kho',
        '/app/reports/movements',
        roles: {_kt, _ql, _qt},
      ),
      MenuLink('Công nợ', '/app/reports/receivables', roles: {_kt, _ql, _qt}),
      MenuLink('Cho thuê', '/app/reports/rentals', roles: {_ql, _qt}),
    ],
  ),
];

const kAdminMenu = <MenuGroup>[
  MenuGroup(
    label: 'Người dùng',
    icon: AppIcons.users,
    path: '/app/users',
    roles: {_ql, _qt},
  ),
  MenuGroup(
    label: 'Cài đặt',
    icon: AppIcons.settings,
    path: '/app/settings',
  ),
];

bool canSee(Set<AppRole>? roles, AppRole role) =>
    roles == null || role == AppRole.unknown || roles.contains(role);

/// Vai trò có được mở [location] không (theo ma trận quyền của menu).
/// Dùng cho router (chặn gõ URL trực tiếp) và để ẩn lối tắt trên Dashboard.
bool canAccessPath(String location, AppRole role) {
  final path = Uri.parse(location).path;
  for (final group in [...kMainMenu, ...kAdminMenu]) {
    if (group.path != null &&
        path.startsWith(group.path!) &&
        !canSee(group.roles, role)) {
      return false;
    }
    for (final link in group.children) {
      if (path == link.path) {
        return canSee(group.roles, role) && canSee(link.roles, role);
      }
    }
  }
  return true;
}
