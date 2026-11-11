import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/session.dart';
import '../../core/auth/session_controller.dart';
import '../../core/router/menu.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import '../../core/theme/theme_mode_provider.dart';
import '../form/unsaved_changes.dart';
import '../widgets/brand_logo.dart';

/// Breakpoint dùng chung của khung app.
class ShellBreakpoints {
  const ShellBreakpoints._();

  /// Dưới mức này: header thu gọn (tìm kiếm thành icon, chọn kho chuyển vào drawer).
  static const compact = 600.0;

  /// Dưới mức này: sidebar thành drawer.
  static const drawer = 900.0;

  /// Dưới mức này: sidebar tự thu thành cột icon.
  static const railAuto = 1200.0;

  /// Từ mức này: hiện tên người dùng cạnh avatar.
  static const wide = 1100.0;
}

/// Chiều cao chung của header và khối logo sidebar để hai đường viền dưới thẳng hàng.
const _barHeight = 64.0;

/// Điều hướng từ menu/header: hỏi trước nếu form đang nhập dở, rồi mới đóng drawer.
Future<void> _navigate(BuildContext context, String path) async {
  final router = GoRouter.of(context);
  final scaffold = Scaffold.maybeOf(context);
  if (!await UnsavedChanges.confirmLeave(context)) return;
  if (scaffold?.isDrawerOpen ?? false) scaffold!.closeDrawer();
  router.go(path);
}

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
  /// null = tự theo bề rộng màn hình (thu gọn khi < [ShellBreakpoints.railAuto]);
  /// khác null = người dùng đã bấm nút menu.
  bool? _userCollapsed;

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionProvider).value;
    final c = context.colors;
    final width = MediaQuery.sizeOf(context).width;
    final mobile = width < ShellBreakpoints.drawer;
    final compact = width < ShellBreakpoints.compact;
    final collapsed =
        !mobile && (_userCollapsed ?? width < ShellBreakpoints.railAuto);
    final sidebar = _Sidebar(
      location: widget.location,
      session: session,
      collapsed: collapsed,
      // Trên điện thoại bộ chọn kho nằm đầu drawer thay vì chiếm chỗ trên header.
      showWarehouseSelector: compact,
      inDrawer: mobile,
      onToggle: () => setState(() => _userCollapsed = !collapsed),
    );
    return Scaffold(
      backgroundColor: c.page,
      drawer: mobile ? Drawer(width: 280, child: sidebar) : null,
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (!mobile) sidebar,
          Expanded(
            child: Column(
              children: [
                _Header(
                  session: session,
                  showMenuButton: mobile,
                  location: widget.location,
                ),
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
/// Trang form (FormPageLayout) tự cuộn để thanh Lưu/Hủy dính đáy nên [scroll] = false.
class PageBody extends StatelessWidget {
  const PageBody({super.key, required this.child, this.scroll = true});

  final Widget child;
  final bool scroll;

  @override
  Widget build(BuildContext context) {
    if (!scroll) return child;
    final mobile = MediaQuery.sizeOf(context).width < ShellBreakpoints.drawer;
    return SingleChildScrollView(
      padding: EdgeInsets.symmetric(
        horizontal: mobile ? AppSpace.s16 : AppSpace.s24,
        vertical: AppSpace.s16,
      ),
      child: child,
    );
  }
}

class _Sidebar extends StatefulWidget {
  const _Sidebar({
    required this.location,
    required this.session,
    required this.collapsed,
    required this.showWarehouseSelector,
    required this.inDrawer,
    required this.onToggle,
  });

  final String location;
  final Session? session;
  final bool collapsed;
  final bool showWarehouseSelector;
  final bool inDrawer;
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
          final pad = collapsed ? AppSpace.s8 : AppSpace.s16;
          return Column(
            children: [
              _brand(context, collapsed),
              if (widget.showWarehouseSelector && widget.session != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(
                    AppSpace.s16,
                    AppSpace.s16,
                    AppSpace.s16,
                    0,
                  ),
                  child: _WarehouseSelector(
                    session: widget.session!,
                    expand: true,
                  ),
                ),
              Expanded(
                child: ListView(
                  padding: EdgeInsets.symmetric(
                    horizontal: pad,
                    vertical: AppSpace.s8,
                  ),
                  children: [
                    for (final g in kMainMenu.where(_visible))
                      ..._group(context, g, collapsed),
                  ],
                ),
              ),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: pad),
                child: Divider(color: c.border, height: 1),
              ),
              Padding(
                padding: EdgeInsets.fromLTRB(
                  pad,
                  AppSpace.s8,
                  pad,
                  AppSpace.s24,
                ),
                child: Column(
                  children: [
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
    return Container(
      height: _barHeight,
      padding: EdgeInsets.symmetric(horizontal: collapsed ? 0 : AppSpace.s16),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: c.divider)),
      ),
      child: Row(
        mainAxisAlignment: collapsed
            ? MainAxisAlignment.center
            : MainAxisAlignment.start,
        children: [
          if (!collapsed) const Expanded(child: BrandLogo()),
          widget.inDrawer
              ? _IconBtn(
                  icon: Icons.close,
                  tooltip: 'Đóng menu',
                  onTap: () => Scaffold.of(context).closeDrawer(),
                )
              : _IconBtn(
                  icon: Icons.menu,
                  tooltip: widget.collapsed ? 'Mở rộng menu' : 'Thu gọn menu',
                  onTap: widget.onToggle,
                ),
        ],
      ),
    );
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
      Offset(box.size.width + AppSpace.s8, 0),
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
            style: itemContext.text.label.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        for (final l in links)
          PopupMenuItem<String>(
            value: l.path,
            child: Text(
              l.label,
              style: itemContext.text.body2.copyWith(
                fontWeight: widget.location == l.path
                    ? FontWeight.w700
                    : FontWeight.w400,
              ),
            ),
          ),
      ],
    );
    if (path != null && itemContext.mounted) _navigate(itemContext, path);
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
            _navigate(context, g.path!);
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
            onTap: () => _navigate(context, link.path),
          ),
      const SizedBox(height: AppSpace.s4),
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
                  top: AppSpace.s8,
                  bottom: AppSpace.s8,
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
                          size: 24,
                          color: active ? c.navActiveText : c.text,
                        ),
                      )
                    else
                      SizedBox(
                        width: AppSpace.touch,
                        child: Icon(
                          icon,
                          size: 24,
                          color: active ? c.navActiveText : c.text,
                        ),
                      ),
                    if (!collapsed) ...[
                      const SizedBox(width: AppSpace.s8),
                      Expanded(
                        child: Text(
                          label,
                          overflow: TextOverflow.ellipsis,
                          style: context.text.h3.copyWith(
                            fontWeight: active
                                ? FontWeight.w700
                                : FontWeight.w600,
                            color: active ? c.navActiveText : c.text,
                          ),
                        ),
                      ),
                      if (hasChildren)
                        Padding(
                          padding: const EdgeInsets.only(right: AppSpace.s8),
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
      padding: const EdgeInsets.only(left: AppSpace.s24, top: AppSpace.s2),
      child: Material(
        color: active ? c.navActive : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(8),
          child: SizedBox(
            height: AppSpace.touch,
            child: Row(
              children: [
                const SizedBox(width: AppSpace.s8),
                Container(
                  width: 3,
                  height: AppSpace.s16,
                  color: active ? c.accent : c.border,
                ),
                const SizedBox(width: AppSpace.s8),
                Expanded(
                  child: Text(
                    label,
                    overflow: TextOverflow.ellipsis,
                    style: context.text.body2.copyWith(
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
  const _Header({
    required this.session,
    required this.showMenuButton,
    required this.location,
  });

  final Session? session;
  final bool showMenuButton;

  /// Đường dẫn đang mở, để tô nút Thông báo khi đang ở trang đó.
  final String location;

  /// Ô tìm kiếm chỉ tìm sản phẩm, nên gợi ý nói đúng điều đó.
  static void _search(BuildContext context, String q) {
    final query = q.trim();
    guardedGo(
      context,
      Uri(
        path: '/app/products',
        queryParameters: query.isEmpty ? null : {'q': query},
      ).toString(),
    );
  }

  /// Điện thoại: chạm icon tìm → overlay tìm kiếm (Open Overlay · Dissolve 200ms).
  static Future<void> _openSearchOverlay(BuildContext context) async {
    final q = await showGeneralDialog<String>(
      context: context,
      barrierDismissible: true,
      barrierLabel: 'Đóng tìm kiếm',
      barrierColor: Colors.black54,
      transitionDuration: const Duration(milliseconds: 200),
      transitionBuilder: (context, animation, _, child) =>
          FadeTransition(opacity: animation, child: child),
      pageBuilder: (dialogContext, _, _) {
        final c = dialogContext.colors;
        return SafeArea(
          child: Align(
            alignment: Alignment.topCenter,
            child: Material(
              color: c.surface,
              child: Padding(
                padding: const EdgeInsets.all(AppSpace.s8),
                child: Row(
                  children: [
                    Expanded(
                      child: TextField(
                        autofocus: true,
                        textInputAction: TextInputAction.search,
                        onSubmitted: (v) => Navigator.pop(dialogContext, v),
                        decoration: InputDecoration(
                          hintText: 'Tìm sản phẩm theo tên hoặc mã SKU',
                          prefixIcon: Icon(
                            Icons.search,
                            size: 20,
                            color: c.muted,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: AppSpace.s4),
                    _IconBtn(
                      icon: Icons.close,
                      tooltip: 'Đóng',
                      onTap: () => Navigator.pop(dialogContext),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
    if (q != null && context.mounted) _search(context, q);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final width = MediaQuery.sizeOf(context).width;
    final compact = width < ShellBreakpoints.compact;
    final wide = width >= ShellBreakpoints.wide;
    final s = session;
    final selectedWarehouse = s == null
        ? null
        : _warehouseLabel(s, ref.watch(warehouseScopeProvider));
    return Container(
      height: _barHeight,
      padding: EdgeInsets.symmetric(
        horizontal: compact ? AppSpace.s8 : AppSpace.s16,
      ),
      decoration: BoxDecoration(
        color: c.surface,
        border: Border(bottom: BorderSide(color: c.divider)),
      ),
      child: Row(
        children: [
          if (showMenuButton)
            _IconBtn(
              icon: Icons.menu,
              tooltip: 'Mở menu',
              onTap: () => Scaffold.of(context).openDrawer(),
            ),
          if (compact) ...[
            const SizedBox(width: AppSpace.s8),
            // Logo + kho đang làm việc, để luôn biết mình đang ở kho nào.
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const BrandLogo(size: BrandLogoSize.compact),
                  if (selectedWarehouse != null)
                    Text(
                      selectedWarehouse,
                      overflow: TextOverflow.ellipsis,
                      style: context.text.caption.copyWith(color: c.muted),
                    ),
                ],
              ),
            ),
            _IconBtn(
              icon: Icons.search,
              tooltip: 'Tìm kiếm sản phẩm',
              onTap: () => _openSearchOverlay(context),
            ),
          ] else ...[
            if (showMenuButton) const SizedBox(width: AppSpace.s8),
            Expanded(
              child: Align(
                alignment: Alignment.centerLeft,
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 560),
                  child: TextField(
                    textInputAction: TextInputAction.search,
                    onSubmitted: (q) => _search(context, q),
                    style: context.text.body1,
                    decoration: InputDecoration(
                      hintText: wide
                          ? 'Tìm sản phẩm theo tên hoặc mã SKU (Enter)'
                          : 'Tìm sản phẩm...',
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
            const SizedBox(width: AppSpace.s16),
            if (s != null) _WarehouseSelector(session: s),
            const SizedBox(width: AppSpace.s8),
            _IconBtn(
              tooltip: 'Đổi giao diện sáng/tối',
              filled: true,
              icon: Theme.of(context).brightness == Brightness.dark
                  ? Icons.light_mode
                  : Icons.dark_mode,
              onTap: () => ref.read(themeModeProvider.notifier).toggle(),
            ),
          ],
          SizedBox(width: compact ? 0 : AppSpace.s8),
          Badge(
            isLabelVisible: (s?.unreadAlerts ?? 0) > 0,
            label: Text('${s?.unreadAlerts ?? 0}'),
            backgroundColor: c.danger,
            offset: const Offset(-AppSpace.s4, AppSpace.s4),
            child: _IconBtn(
              tooltip: 'Cảnh báo và thông báo',
              icon: Icons.notifications_none,
              selectedIcon: Icons.notifications,
              selected: location.startsWith('/app/alerts'),
              onTap: () => guardedGo(context, '/app/alerts'),
            ),
          ),
          SizedBox(width: compact ? 0 : AppSpace.s8),
          if (s != null)
            _UserMenu(session: s, showName: wide, showThemeToggle: compact),
        ],
      ),
    );
  }
}

String _warehouseLabel(Session session, int? selected) {
  if (selected == null) {
    return session.canViewAllWarehouses ? 'Tất cả kho' : 'Chưa phân kho';
  }
  final w = session.warehouses.where((opt) => opt.id == selected).firstOrNull;
  if (w == null) return 'Chưa phân kho';
  return w.name.isNotEmpty ? w.name : w.code;
}

class _WarehouseSelector extends ConsumerWidget {
  const _WarehouseSelector({required this.session, this.expand = false});

  final Session session;

  /// true = chiếm hết bề ngang (đặt trong drawer trên điện thoại).
  final bool expand;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final selected = ref.watch(warehouseScopeProvider);
    final options = <(int?, String)>[
      if (session.canViewAllWarehouses) (null, 'Tất cả kho'),
      for (final w in session.warehouses)
        (w.id, w.name.isNotEmpty ? w.name : w.code),
    ];
    final label = _warehouseLabel(session, selected);
    return PopupMenuButton<int?>(
      tooltip: 'Chọn kho làm việc',
      onSelected: (id) => ref.read(warehouseScopeProvider.notifier).select(id),
      itemBuilder: (_) => [
        for (final (id, name) in options)
          PopupMenuItem<int?>(value: id, child: Text(name)),
      ],
      child: Container(
        height: AppSpace.touch,
        width: expand ? double.infinity : null,
        constraints: expand
            ? null
            : const BoxConstraints(minWidth: 140, maxWidth: 220),
        padding: const EdgeInsets.symmetric(horizontal: AppSpace.s16),
        decoration: BoxDecoration(
          border: Border.all(color: c.border),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          mainAxisSize: expand ? MainAxisSize.max : MainAxisSize.min,
          children: [
            Flexible(
              fit: expand ? FlexFit.tight : FlexFit.loose,
              child: Text(
                'Kho: $label',
                overflow: TextOverflow.ellipsis,
                style: context.text.label,
              ),
            ),
            const SizedBox(width: AppSpace.s8),
            Icon(Icons.keyboard_arrow_down, size: 20, color: c.text),
          ],
        ),
      ),
    );
  }
}

/// Nút icon tròn: hình 32px nhưng vùng bấm 44×44px.
/// [selected]: đang ở trang của nút → tô màu nhấn (giống nút sáng/tối).
class _IconBtn extends StatelessWidget {
  const _IconBtn({
    required this.icon,
    required this.onTap,
    required this.tooltip,
    this.filled = false,
    this.selected = false,
    this.selectedIcon,
  });

  final IconData icon;
  final IconData? selectedIcon;
  final VoidCallback onTap;
  final String tooltip;
  final bool filled;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final on = filled || selected;
    return Tooltip(
      message: tooltip,
      child: Semantics(
        button: true,
        selected: selected,
        child: SizedBox.square(
          dimension: AppSpace.touch,
          child: Material(
            type: MaterialType.transparency,
            child: InkWell(
              customBorder: const CircleBorder(),
              onTap: onTap,
              child: Center(
                child: Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: on ? c.accent : Colors.transparent,
                  ),
                  child: Icon(
                    selected ? (selectedIcon ?? icon) : icon,
                    size: 20,
                    color: on ? Colors.white : c.text,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _UserMenu extends ConsumerWidget {
  const _UserMenu({
    required this.session,
    required this.showName,
    required this.showThemeToggle,
  });

  final Session session;
  final bool showName;

  /// Trên điện thoại nút sáng/tối chuyển vào menu này để header gọn.
  final bool showThemeToggle;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final dark = Theme.of(context).brightness == Brightness.dark;
    return PopupMenuButton<String>(
      tooltip: 'Tài khoản',
      onSelected: (value) async {
        switch (value) {
          case 'theme':
            ref.read(themeModeProvider.notifier).toggle();
          case 'settings':
            guardedGo(context, '/app/settings');
          case 'logout':
            if (await UnsavedChanges.confirmLeave(context)) {
              await ref.read(sessionProvider.notifier).logout();
            }
        }
      },
      itemBuilder: (_) => [
        if (showThemeToggle)
          PopupMenuItem(
            value: 'theme',
            child: Text(dark ? 'Giao diện sáng' : 'Giao diện tối'),
          ),
        const PopupMenuItem(value: 'settings', child: Text('Cài đặt')),
        const PopupMenuItem(value: 'logout', child: Text('Đăng xuất')),
      ],
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: AppSpace.touch),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(width: AppSpace.s4),
            CircleAvatar(
              radius: 16,
              backgroundColor: c.brand,
              child: Text(
                session.initials,
                style: context.text.caption.copyWith(
                  color: c.onBrand,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            if (showName) ...[
              const SizedBox(width: AppSpace.s8),
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(session.displayName, style: context.text.label),
                  Text(
                    session.role.label,
                    style: context.text.caption.copyWith(color: c.muted),
                  ),
                ],
              ),
            ],
            const SizedBox(width: AppSpace.s4),
            Icon(Icons.keyboard_arrow_down, size: 20, color: c.text),
          ],
        ),
      ),
    );
  }
}
