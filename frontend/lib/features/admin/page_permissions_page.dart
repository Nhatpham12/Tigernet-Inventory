import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/role.dart';
import '../../core/providers.dart';
import '../../core/router/menu.dart';
import '../../core/theme/app_colors.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

final usersLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/users'),
);

/// Quản trị chọn trang (màn hình) mỗi nhân viên được xem.
/// Chưa phân quyền riêng thì nhân viên dùng quyền mặc định theo vai trò.
/// GET/PUT/DELETE /bff/api/users/{id}/page-permissions.
class PagePermissionsPage extends ConsumerStatefulWidget {
  const PagePermissionsPage({super.key, this.initialUserId});

  final int? initialUserId;

  @override
  ConsumerState<PagePermissionsPage> createState() =>
      _PagePermissionsPageState();
}

class _PagePermissionsPageState extends ConsumerState<PagePermissionsPage> {
  late int? _userId = widget.initialUserId;
  Set<String> _selected = {};
  bool _custom = false;
  bool _loading = false;
  bool _saving = false;
  bool _dirty = false;
  Object? _error;

  @override
  void initState() {
    super.initState();
    if (_userId != null) _load(_userId!);
  }

  Json? _user(List<Json> users) =>
      users.where((u) => u['userId'] == _userId).firstOrNull;

  AppRole _role(List<Json> users) =>
      AppRole.fromCode(_user(users)?['roleName'] as String?);

  Set<String> _roleDefaults(AppRole role) => {
    for (final p in kAssignablePages)
      if (roleDefaultAllows(p.path, role)) p.path,
  };

  Future<void> _load(int userId) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await ref
          .read(apiClientProvider)
          .get('/bff/api/users/$userId/page-permissions');
      final users = await ref.read(usersLookupProvider.future);
      if (!mounted || userId != _userId) return;
      final custom = result is Map && result['custom'] == true;
      setState(() {
        _custom = custom;
        _selected = custom
            ? {for (final p in (result['pages'] as List? ?? const [])) '$p'}
            : _roleDefaults(_role(users));
        _dirty = false;
        _loading = false;
      });
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e;
          _loading = false;
        });
      }
    }
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      await ref
          .read(apiClientProvider)
          .put(
            '/bff/api/users/$_userId/page-permissions',
            data: {'pages': _selected.toList()..sort()},
          );
      if (!mounted) return;
      setState(() {
        _custom = true;
        _dirty = false;
      });
      Toast.success(
        context,
        'Đã lưu phân quyền. Nhân viên thấy thay đổi ở lần đăng nhập/tải lại trang tiếp theo.',
      );
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _reset(AppRole role) async {
    final ok = await confirmDialog(
      context,
      title: 'Dùng quyền mặc định',
      message:
          'Xóa phân quyền riêng, nhân viên sẽ xem các trang theo vai trò ${role.label}.',
      confirmLabel: 'Dùng mặc định',
    );
    if (!ok) return;
    setState(() => _saving = true);
    try {
      await ref
          .read(apiClientProvider)
          .delete('/bff/api/users/$_userId/page-permissions');
      if (!mounted) return;
      setState(() {
        _custom = false;
        _selected = _roleDefaults(role);
        _dirty = false;
      });
      Toast.success(context, 'Đã chuyển về quyền mặc định theo vai trò.');
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _toggle(Iterable<String> paths, bool on) => setState(() {
    on ? _selected.addAll(paths) : _selected.removeAll(paths);
    _dirty = true;
  });

  @override
  Widget build(BuildContext context) {
    final users = ref.watch(usersLookupProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const PageHeader(
          title: 'Phân quyền trang',
          subtitle:
              'Chọn các trang mỗi nhân viên được xem. Quản trị luôn xem được mọi trang.',
          crumbs: [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Quản trị'),
            Crumb('Phân quyền trang'),
          ],
        ),
        users.when(
          loading: () => const LoadingView(),
          error: (e, _) => StateView.error(
            e,
            onRetry: () => ref.invalidate(usersLookupProvider),
          ),
          data: (list) => _content(context, list),
        ),
      ],
    );
  }

  Widget _content(BuildContext context, List<Json> users) {
    final c = context.colors;
    final staff =
        users.where((u) => u['roleName'] != AppRole.admin.code).toList()..sort(
          (a, b) => '${a['username']}'.toLowerCase().compareTo(
            '${b['username']}'.toLowerCase(),
          ),
        );
    final user = _user(users);
    final role = _role(users);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SectionCard(
          child: Wrap(
            spacing: 16,
            runSpacing: 12,
            crossAxisAlignment: WrapCrossAlignment.end,
            children: [
              SizedBox(
                width: 420,
                child: AppDropdown<int>(
                  label: 'Nhân viên',
                  hint: 'Chọn nhân viên cần phân quyền',
                  value: staff.any((u) => u['userId'] == _userId)
                      ? _userId
                      : null,
                  items: [
                    for (final u in staff)
                      (
                        (u['userId'] as num).toInt(),
                        '${u['username']} – ${u['fullName'] ?? ''} '
                            '(${AppRole.fromCode(u['roleName'] as String?).label})',
                      ),
                  ],
                  onChanged: (id) {
                    if (id == null) return;
                    setState(() => _userId = id);
                    _load(id);
                  },
                ),
              ),
              if (user != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _custom
                      ? const StatusChip(
                          label: 'Đang dùng phân quyền riêng',
                          tone: ChipTone.purple,
                        )
                      : StatusChip(
                          label: 'Đang dùng mặc định của ${role.label}',
                          tone: ChipTone.info,
                        ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        if (_userId == null || user == null)
          SectionCard(
            child: Text(
              staff.isEmpty
                  ? 'Chưa có nhân viên nào ngoài Quản trị.'
                  : 'Chọn một nhân viên để xem và chỉnh các trang được phép xem.',
              style: TextStyle(color: c.muted),
            ),
          )
        else if (_loading)
          const LoadingView()
        else if (_error != null)
          StateView.error(_error!, onRetry: () => _load(_userId!))
        else
          _pagesCard(context, role),
      ],
    );
  }

  Widget _pagesCard(BuildContext context, AppRole role) {
    final c = context.colors;
    final groups = <String, List<PageOption>>{};
    for (final p in kAssignablePages) {
      groups.putIfAbsent(p.group, () => []).add(p);
    }
    return SectionCard(
      title: 'Trang được xem',
      trailing: Wrap(
        spacing: 8,
        runSpacing: 8,
        children: [
          OutlinedButton(
            onPressed: _saving
                ? null
                : () => setState(() {
                    _selected = _roleDefaults(role);
                    _dirty = true;
                  }),
            child: const Text('Chọn theo vai trò'),
          ),
          if (_custom)
            OutlinedButton(
              onPressed: _saving ? null : () => _reset(role),
              child: const Text('Dùng mặc định'),
            ),
          ElevatedButton(
            onPressed: _saving || (!_dirty && _custom) ? null : _save,
            child: Text(_saving ? 'Đang lưu...' : 'Lưu phân quyền'),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Trang Tổng quan, Cảnh báo và Cài đặt luôn hiển thị. '
            'Đây là quyền xem màn hình; thao tác ghi dữ liệu vẫn theo vai trò.',
            style: TextStyle(fontSize: 13, color: c.muted),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              for (final MapEntry(key: group, value: pages) in groups.entries)
                _groupBox(context, group, pages, role),
            ],
          ),
        ],
      ),
    );
  }

  Widget _groupBox(
    BuildContext context,
    String group,
    List<PageOption> pages,
    AppRole role,
  ) {
    final c = context.colors;
    final paths = pages.map((p) => p.path);
    final count = paths.where(_selected.contains).length;
    final groupValue = count == 0
        ? false
        : (count == pages.length ? true : null);
    return Container(
      width: 300,
      decoration: BoxDecoration(
        border: Border.all(color: c.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CheckboxListTile(
            tristate: true,
            value: groupValue,
            onChanged: (_) => _toggle(paths, groupValue != true),
            controlAffinity: ListTileControlAffinity.leading,
            title: Text(
              group,
              style: TextStyle(fontWeight: FontWeight.w700, color: c.text),
            ),
            dense: true,
          ),
          Divider(height: 1, color: c.divider),
          for (final p in pages)
            CheckboxListTile(
              value: _selected.contains(p.path),
              onChanged: (v) => _toggle([p.path], v == true),
              controlAffinity: ListTileControlAffinity.leading,
              dense: true,
              contentPadding: const EdgeInsets.only(left: 32, right: 12),
              title: Text(p.label, style: TextStyle(color: c.text)),
              subtitle: roleDefaultAllows(p.path, role)
                  ? null
                  : Text(
                      'Ngoài quyền mặc định của ${role.label}',
                      style: TextStyle(fontSize: 11, color: c.muted),
                    ),
            ),
        ],
      ),
    );
  }
}
