import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session.dart';
import '../../core/auth/session_controller.dart';
import '../../core/router/menu.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/theme_mode_provider.dart';

/// Khung ứng dụng (Figma "ERP / Responsive Shell"): sidebar trái + header trên,
/// header/sidebar đứng yên, phần nội dung cuộn riêng.
class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key, required this.child, required this.location});

  final Widget child;
  final String location;

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  /// null = tự theo bề rộng màn hình (thu gọn khi < 1200px); khác null = người dùng đã bấm nút menu.
  bool? _userCollapsed;

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionProvider).value;
    final c = context.colors;
    final width = MediaQuery.sizeOf(context).width;
    final mobile = width < 900;
    final collapsed = !mobile && (_userCollapsed ?? width < 1200);
    final sidebar = _Sidebar(
      location: widget.location,
      session: session,
      collapsed: collapsed,
      // trong drawer (mobile) nút menu đóng drawer
      onToggle: mobile
          ? () => Scaffold.maybeOf(context)?.closeDrawer()
          : () => setState(() => _userCollapsed = !collapsed),
    );
    return Scaffold(
      backgroundColor: c.page,
      drawer: mobile ? Drawer(width: 260, child: sidebar) : null,
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (!mobile) sidebar,
          Expanded(
            child: Column(
              children: [
                _Header(session: session, showMenuButton: mobile),
                // Navigator lồng của ShellRoute cần chiều cao hữu hạn: mỗi trang tự cuộn qua PageBody.
                Expanded(child: widget.child),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Khung nội dung của một trang trong AppShell: cuộn dọc và lề theo kích thước màn hình.
class PageBody extends StatelessWidget {
  const PageBody({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final mobile = MediaQuery.sizeOf(context).width < 900;
    return SingleChildScrollView(
      padding: EdgeInsets.symmetric(horizontal: mobile ? 16 : 24, vertical: 16),
      child: child,
    );
  }
}

class _Sidebar extends StatefulWidget {
  const _Sidebar({
    required this.location,
    required this.session,
    required this.collapsed,
    required this.onToggle,
  });

  final String location;
  final Session? session;
  final bool collapsed;
  final VoidCallback onToggle;

  @override
  State<_Sidebar> createState() => _SidebarState();
}

class _SidebarState extends State<_Sidebar> {
  static const _collapsedWidth = 76.0;
  static const _expandedWidth = 236.0;

  final Set<String> _open = {};

  bool _isActive(String path) =>
      widget.location == path ||
      (path != '/app/dashboard' && widget.location.startsWith('$path/'));

  bool _groupActive(MenuGroup g) =>
      (g.path != null && _isActive(g.path!)) ||
      g.children.any((l) => _isActive(l.path));

  bool _canOpen(String path) {
    final session = widget.session;
    return session == null || canOpenPage(path, session);
  }

  List<MenuLink> _links(MenuGroup g) {
    final role = widget.session?.role;
    if (role != null &&
        !canSee(g.roles, role) &&
        widget.session?.pages == null) {
      return const [];
    }
    return g.children.where((l) => _canOpen(l.path)).toList();
  }

  bool _visible(MenuGroup g) =>
      g.path != null ? _canOpen(g.path!) : _links(g).isNotEmpty;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      width: widget.collapsed ? _collapsedWidth : _expandedWidth,
      clipBehavior: Clip.hardEdge,
      decoration: BoxDecoration(
        color: c.surface,
        border: Border(right: BorderSide(color: c.divider)),
      ),
      // Bố cục theo bề rộng thực tế: trong lúc thu/mở (animation) không bị tràn chữ.
      child: LayoutBuilder(
        builder: (context, constraints) {
          final collapsed = constraints.maxWidth < 180;
          return Column(
            children: [
              _brand(context, collapsed),
              Expanded(
                child: ListView(
                  padding: EdgeInsets.symmetric(
                    horizontal: collapsed ? 8 : 12,
                    vertical: 8,
                  ),
                  children: [
                    for (final g in kMainMenu.where(_visible))
                      ..._group(context, g, collapsed),
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 8,
                      ),
                      child: Divider(color: c.border, height: 1),
                    ),
                    for (final g in kAdminMenu.where(_visible))
                      ..._group(context, g, collapsed),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _brand(BuildContext context, bool collapsed) {
    final c = context.colors;
    final toggle = IconButton(
      tooltip: widget.collapsed ? 'Mở rộng menu' : 'Thu gọn menu',
      onPressed: widget.onToggle,
      icon: Icon(Icons.menu, color: c.text, size: 20),
    );
    if (collapsed) return SizedBox(height: 78, child: Center(child: toggle));
    return SizedBox(
      height: 78,
      child: Padding(
        padding: const EdgeInsets.only(left: 16, right: 8),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  for (final (text, size, height) in [
                    ('TIGERNET', 17.0, 1.0),
                    ('INVENTORY', 22.0, 1.1),
                  ])
                    Text(
                      text,
                      maxLines: 1,
                      softWrap: false,
                      overflow: TextOverflow.fade,
                      style: TextStyle(
                        color: c.accent,
                        fontSize: size,
                        fontWeight: FontWeight.w800,
                        height: height,
                      ),
                    ),
                ],
              ),
            ),
            toggle,
          ],
        ),
      ),
    );
  }

  void _go(BuildContext context, String path) {
    context.go(path);
    Scaffold.maybeOf(context)?.closeDrawer();
  }

  /// Menu thu gọn: bấm nhóm thì hiện danh sách trang con bên phải để chọn.
  Future<void> _showFlyout(
    BuildContext itemContext,
    MenuGroup g,
    List<MenuLink> links,
  ) async {
    final box = itemContext.findRenderObject() as RenderBox;
    final overlay =
        Overlay.of(itemContext).context.findRenderObject() as RenderBox;
    final topRight = box.localToGlobal(
      Offset(box.size.width + 8, 0),
      ancestor: overlay,
    );
    final path = await showMenu<String>(
      context: itemContext,
      position: RelativeRect.fromLTRB(
        topRight.dx,
        topRight.dy,
        overlay.size.width - topRight.dx,
        0,
      ),
      items: [
        PopupMenuItem<String>(
          enabled: false,
          height: 36,
          child: Text(
            g.label,
            style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13),
          ),
        ),
        for (final l in links)
          PopupMenuItem<String>(
            value: l.path,
            child: Text(
              l.label,
              style: TextStyle(
                fontWeight: widget.location == l.path
                    ? FontWeight.w700
                    : FontWeight.w400,
              ),
            ),
          ),
      ],
    );
    if (path != null && itemContext.mounted) _go(itemContext, path);
  }

  List<Widget> _group(BuildContext context, MenuGroup g, bool collapsed) {
    final active = _groupActive(g);
    final links = _links(g);
    final expanded = !collapsed && (_open.contains(g.label) || active);
    return [
      _NavItem(
        icon: g.icon,
        label: g.label,
        active: active,
        collapsed: collapsed,
        hasChildren: links.isNotEmpty,
        expanded: expanded,
        onTap: (itemContext) {
          if (g.path != null) {
            _go(context, g.path!);
          } else if (collapsed && links.isNotEmpty) {
            _showFlyout(itemContext, g, links);
          } else {
            setState(
              () => _open.contains(g.label)
                  ? _open.remove(g.label)
                  : _open.add(g.label),
            );
          }
        },
      ),
      if (expanded)
        for (final link in links)
          _SubItem(
            label: link.label,
            active: widget.location == link.path,
            onTap: () => _go(context, link.path),
          ),
      const SizedBox(height: 4),
    ];
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.icon,
    required this.label,
    required this.active,
    required this.collapsed,
    required this.hasChildren,
    required this.expanded,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final bool active;
  final bool collapsed;
  final bool hasChildren;
  final bool expanded;
  final void Function(BuildContext itemContext) onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final item = Material(
      color: active ? c.navActive : Colors.transparent,
      borderRadius: BorderRadius.circular(8),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => onTap(context),
        child: SizedBox(
          height: 48,
          child: Stack(
            children: [
              if (active)
                Positioned(
                  left: 0,
                  top: 6,
                  bottom: 6,
                  child: Container(
                    width: 3,
                    decoration: BoxDecoration(
                      color: c.accent,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              Positioned.fill(
                child: Row(
                  children: [
                    if (collapsed)
                      Expanded(
                        child: Icon(
                          icon,
                          size: 23,
                          color: active ? c.navActiveText : c.text,
                        ),
                      )
                    else
                      SizedBox(
                        width: 42,
                        child: Icon(
                          icon,
                          size: 23,
                          color: active ? c.navActiveText : c.text,
                        ),
                      ),
                    if (!collapsed) ...[
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          label,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: active
                                ? FontWeight.w700
                                : FontWeight.w600,
                            color: active ? c.navActiveText : c.text,
                          ),
                        ),
                      ),
                      if (hasChildren)
                        Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: Icon(
                            expanded
                                ? Icons.keyboard_arrow_up
                                : Icons.keyboard_arrow_down,
                            size: 20,
                            color: c.muted,
                          ),
                        ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
    return collapsed ? Tooltip(message: label, child: item) : item;
  }
}

class _SubItem extends StatelessWidget {
  const _SubItem({
    required this.label,
    required this.active,
    required this.onTap,
  });

  final String label;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Padding(
      padding: const EdgeInsets.only(left: 29, top: 2),
      child: Material(
        color: active ? c.navActive : Colors.transparent,
        borderRadius: BorderRadius.circular(6),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(6),
          child: SizedBox(
            height: 35,
            child: Row(
              children: [
                const SizedBox(width: 10),
                Container(
                  width: 3,
                  height: 17,
                  color: active ? c.accent : c.border,
                ),
                const SizedBox(width: 7),
                Expanded(
                  child: Text(
                    label,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: active ? FontWeight.w700 : FontWeight.w400,
                      color: active ? c.navActiveText : c.muted,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Header extends ConsumerWidget {
  const _Header({required this.session, required this.showMenuButton});

  final Session? session;
  final bool showMenuButton;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final wide = MediaQuery.sizeOf(context).width >= 1100;
    final s = session;
    return Container(
      height: 70,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border(bottom: BorderSide(color: c.divider)),
      ),
      child: Row(
        children: [
          if (showMenuButton)
            IconButton(
              icon: const Icon(Icons.menu),
              onPressed: () => Scaffold.of(context).openDrawer(),
            ),
          Expanded(
            child: Align(
              alignment: Alignment.centerLeft,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 580),
                child: TextField(
                  onSubmitted: (q) => context.go(
                    Uri(
                      path: '/app/products',
                      queryParameters: {'q': q},
                    ).toString(),
                  ),
                  decoration: InputDecoration(
                    hintText: wide
                        ? 'Tìm mã phiếu, sản phẩm, kho, người tạo... (Enter)'
                        : 'Tìm kiếm...',
                    prefixIcon: Icon(Icons.search, size: 20, color: c.muted),
                    fillColor: c.surfaceMuted,
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: c.divider),
                    ),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 16),
          if (s != null) _WarehouseSelector(session: s),
          const SizedBox(width: 12),
          _RoundButton(
            tooltip: 'Đổi giao diện sáng/tối',
            filled: true,
            icon: Theme.of(context).brightness == Brightness.dark
                ? Icons.light_mode
                : Icons.dark_mode,
            onTap: () => ref.read(themeModeProvider.notifier).toggle(),
          ),
          const SizedBox(width: 10),
          _RoundButton(
            tooltip: 'Cài đặt',
            icon: Icons.settings_outlined,
            onTap: () => context.go('/app/settings'),
          ),
          const SizedBox(width: 10),
          Badge(
            isLabelVisible: (s?.unreadAlerts ?? 0) > 0,
            label: Text('${s?.unreadAlerts ?? 0}'),
            backgroundColor: c.danger,
            child: _RoundButton(
              tooltip: 'Cảnh báo và thông báo',
              icon: Icons.notifications_none,
              onTap: () => context.go('/app/alerts'),
            ),
          ),
          const SizedBox(width: 16),
          if (s != null) _UserMenu(session: s, showName: wide),
        ],
      ),
    );
  }
}

class _WarehouseSelector extends ConsumerWidget {
  const _WarehouseSelector({required this.session});

  final Session session;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final selected = ref.watch(warehouseScopeProvider);
    final options = <(int?, String)>[
      if (session.canViewAllWarehouses) (null, 'Tất cả kho'),
      for (final w in session.warehouses)
        (w.id, w.name.isNotEmpty ? w.name : w.code),
    ];
    final label = options
        .firstWhere(
          (o) => o.$1 == selected,
          orElse: () => (null, 'Chưa phân kho'),
        )
        .$2;
    return PopupMenuButton<int?>(
      tooltip: 'Chọn kho làm việc',
      onSelected: (id) => ref.read(warehouseScopeProvider.notifier).select(id),
      itemBuilder: (_) => [
        for (final (id, name) in options)
          PopupMenuItem<int?>(value: id, child: Text(name)),
      ],
      child: Container(
        height: 44,
        constraints: const BoxConstraints(minWidth: 140, maxWidth: 220),
        padding: const EdgeInsets.symmetric(horizontal: 12),
        decoration: BoxDecoration(
          border: Border.all(color: c.border),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Flexible(
              child: Text(
                'Kho: $label',
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: c.text,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Icon(Icons.keyboard_arrow_down, size: 18, color: c.text),
          ],
        ),
      ),
    );
  }
}

class _RoundButton extends StatelessWidget {
  const _RoundButton({
    required this.icon,
    required this.onTap,
    required this.tooltip,
    this.filled = false,
  });

  final IconData icon;
  final VoidCallback onTap;
  final String tooltip;
  final bool filled;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Tooltip(
      message: tooltip,
      child: Material(
        color: filled ? c.accent : c.surface,
        shape: CircleBorder(
          side: filled ? BorderSide.none : BorderSide(color: c.border),
        ),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: onTap,
          child: SizedBox(
            width: 32,
            height: 32,
            child: Icon(icon, size: 18, color: filled ? Colors.white : c.text),
          ),
        ),
      ),
    );
  }
}

class _UserMenu extends ConsumerWidget {
  const _UserMenu({required this.session, required this.showName});

  final Session session;
  final bool showName;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    return PopupMenuButton<String>(
      tooltip: 'Tài khoản',
      onSelected: (value) async {
        if (value == 'logout') {
          await ref.read(sessionProvider.notifier).logout();
        } else if (value == 'settings') {
          if (context.mounted) context.go('/app/settings');
        }
      },
      itemBuilder: (_) => const [
        PopupMenuItem(value: 'settings', child: Text('Cài đặt')),
        PopupMenuItem(value: 'logout', child: Text('Đăng xuất')),
      ],
      child: Row(
        children: [
          CircleAvatar(
            radius: 16,
            backgroundColor: const Color(0xFFD7193B),
            child: Text(
              session.initials,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          if (showName) ...[
            const SizedBox(width: 8),
            Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  session.displayName,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: c.text,
                  ),
                ),
                Text(
                  session.role.label,
                  style: TextStyle(fontSize: 11, color: c.muted),
                ),
              ],
            ),
          ],
          const SizedBox(width: 8),
          Icon(Icons.keyboard_arrow_down, size: 20, color: c.text),
        ],
      ),
    );
  }
}
