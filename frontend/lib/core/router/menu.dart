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
    icon: Icons.home_outlined,
    path: '/app/dashboard',
  ),
  MenuGroup(
    label: 'Sản phẩm',
    icon: Icons.inventory_2_outlined,
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
    icon: Icons.move_to_inbox_outlined,
    roles: {_tk, _kt, _ql, _qt},
    children: [
      MenuLink(
        'Tạo phiếu nhập',
        '/app/import-receipts/new',
        roles: {_tk, _ql, _qt},
      ),
      MenuLink('Danh sách phiếu nhập', '/app/import-receipts'),
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
    icon: Icons.logout,
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
    icon: Icons.swap_horiz,
    roles: {_tk, _ql, _qt},
    children: [MenuLink('Danh sách phiếu chuyển kho', '/app/transfers')],
  ),
  MenuGroup(
    label: 'Cho thuê',
    icon: Icons.description_outlined,
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
    icon: Icons.outbox_outlined,
    roles: {_tk, _bh, _ql, _qt},
    children: [
      MenuLink('Danh sách phiếu mượn', '/app/lends'),
      MenuLink('Tạo phiếu cho mượn', '/app/lends/new', roles: {_bh, _ql, _qt}),
    ],
  ),
  // Bảo hành
  MenuGroup(
    label: 'Bảo hành',
    icon: Icons.build_circle_outlined,
    roles: {_tk, _bh, _ql, _qt},
    children: [
      MenuLink('Danh sách phiếu bảo hành', '/app/warranty-claims'),
      MenuLink(
        'Tiếp nhận bảo hành',
        '/app/warranty-claims/new',
        roles: {_tk, _bh, _ql, _qt},
      ),
    ],
  ),
  // hết Bảo hành
  MenuGroup(
    label: 'Kho',
    icon: Icons.warehouse_outlined,
    roles: {_tk, _kt, _ql, _qt},
    children: [
      MenuLink('Danh sách kho', '/app/warehouses', roles: {_ql, _qt}),
      MenuLink('Kiểm kê', '/app/stocktakes', roles: {_tk, _ql, _qt}),
    ],
  ),
  // Thu chi
  MenuGroup(
    label: 'Thu chi',
    icon: Icons.payments_outlined,
    roles: {_kt, _bh, _ql, _qt},
    children: [
      // Bán hàng chỉ xem phiếu thu (màn danh sách tự lọc theo vai trò)
      MenuLink('Phiếu thu / chi', '/app/payments'),
      MenuLink(
        'Tạo phiếu thu',
        '/app/payments/new?direction=IN',
        roles: {_kt, _ql, _qt},
      ),
      MenuLink(
        'Tạo phiếu chi',
        '/app/payments/new?direction=OUT',
        roles: {_kt, _ql, _qt},
      ),
    ],
  ),
  MenuGroup(
    label: 'Báo cáo',
    icon: Icons.bar_chart,
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
      // Đối chiếu công nợ
      MenuLink(
        'Đối chiếu công nợ',
        '/app/reports/reconciliation',
        roles: {_kt, _ql, _qt},
      ),
    ],
  ),
];

const kAdminMenu = <MenuGroup>[
  MenuGroup(
    label: 'Người dùng',
    icon: Icons.group_outlined,
    path: '/app/users',
    roles: {_ql, _qt},
  ),
  MenuGroup(
    label: 'Cài đặt',
    icon: Icons.settings_outlined,
    path: '/app/settings',
  ),
];

bool canSee(Set<AppRole>? roles, AppRole role) =>
    roles == null || role == AppRole.unknown || roles.contains(role);
