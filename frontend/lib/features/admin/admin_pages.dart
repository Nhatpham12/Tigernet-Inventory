import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/role.dart';
import '../../core/auth/session_controller.dart';
import '../../core/config/env.dart';
import '../../core/network/api_exception.dart';
import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/theme_mode_provider.dart';
import '../../core/utils/formatters.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/data_table/paged_list_view.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/section_card.dart';
import '../../shared/widgets/status_chip.dart';
import '../common/lookups.dart';

/// Nhân viên (QT-01) — danh sách GET /bff/users; Quản trị thêm/sửa/khóa/xóa qua /bff/api/users.
/// Quản lý chỉ xem danh sách (backend chặn ghi với vai trò khác ADMIN).
class UserListPage extends ConsumerStatefulWidget {
  const UserListPage({super.key});

  @override
  ConsumerState<UserListPage> createState() => _UserListPageState();
}

class _UserListPageState extends ConsumerState<UserListPage> {
  final _controller = PagedListController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _edit([RowData? user]) async {
    final saved = await showDialog<bool>(
      context: context,
      builder: (_) => UserDialog(user: user),
    );
    if (saved == true) _controller.reload();
  }

  Future<void> _setActive(RowData user, bool active) async {
    final name = '${user['username']}';
    if (!active) {
      final ok = await confirmDialog(
        context,
        title: 'Khóa tài khoản $name',
        message:
            'Nhân viên sẽ không đăng nhập được nữa. Lịch sử thao tác vẫn được giữ.',
        confirmLabel: 'Khóa tài khoản',
        danger: true,
      );
      if (!ok) return;
    }
    try {
      await ref
          .read(apiClientProvider)
          .put('/bff/api/users/${user['userId']}', data: {'isActive': active});
      if (!mounted) return;
      Toast.success(
        context,
        active ? 'Đã mở khóa tài khoản $name.' : 'Đã khóa tài khoản $name.',
      );
      _controller.reload();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  Future<void> _delete(RowData user) async {
    final name = '${user['username']}';
    final ok = await confirmDialog(
      context,
      title: 'Xóa nhân viên $name',
      message:
          'Tài khoản chưa dùng sẽ bị xóa hẳn. Tài khoản đã có lịch sử (đăng nhập, lập phiếu...) '
          'chỉ bị khóa để giữ lịch sử giao dịch.',
      confirmLabel: 'Xóa',
      danger: true,
    );
    if (!ok) return;
    try {
      final result = await ref
          .read(apiClientProvider)
          .delete('/bff/api/users/${user['userId']}');
      if (!mounted) return;
      final deleted = result is! Map || result['deleted'] != false;
      Toast.success(
        context,
        deleted
            ? 'Đã xóa nhân viên $name.'
            : 'Nhân viên $name đã có lịch sử nên được khóa tài khoản thay vì xóa.',
      );
      _controller.reload();
    } catch (e) {
      if (mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionProvider).value;
    final isAdmin = session?.role == AppRole.admin;
    return PagedListView(
      title: 'Nhân viên',
      subtitle: isAdmin
          ? 'Tài khoản, vai trò, trạng thái hoạt động và quyền xem trang'
          : 'Tài khoản và vai trò của nhân viên (chỉ Quản trị được chỉnh sửa)',
      crumbs: const [
        Crumb('Trang chủ', '/app/dashboard'),
        Crumb('Quản trị'),
        Crumb('Nhân viên'),
      ],
      controller: _controller,
      actions: [
        if (isAdmin)
          ElevatedButton(
            onPressed: () => _edit(),
            child: const Text('Thêm nhân viên'),
          ),
      ],
      endpoint: '/bff/users',
      useWarehouseScope: false,
      searchHint: 'Tìm tên đăng nhập, họ tên, email...',
      itemLabel: 'nhân viên',
      onRowTap: isAdmin ? _edit : null,
      columns: [
        TableColumnDef.text(
          'Tên đăng nhập',
          (r) => '${r['username']}',
          minWidth: 130,
          sortKey: 'username',
          bold: true,
        ),
        TableColumnDef.text(
          'Họ tên',
          (r) => '${r['fullName'] ?? '—'}',
          flex: 3,
          minWidth: 160,
          sortKey: 'fullName',
        ),
        TableColumnDef.text(
          'Email',
          (r) => '${r['email'] ?? '—'}',
          flex: 3,
          minWidth: 180,
        ),
        TableColumnDef.text(
          'Vai trò',
          (r) => AppRole.fromCode(r['roleName'] as String?).label,
          minWidth: 120,
          sortKey: 'roleName',
        ),
        TableColumnDef.text(
          'Ngày tạo',
          (r) => Fmt.date(r['createdAt']),
          minWidth: 100,
          sortKey: 'createdAt',
        ),
        TableColumnDef(
          label: 'Trạng thái',
          minWidth: 120,
          cell: (r) => r['isActive'] == true
              ? const StatusChip(label: 'Hoạt động', tone: ChipTone.success)
              : const StatusChip(label: 'Đã khóa', tone: ChipTone.danger),
        ),
        if (isAdmin)
          TableColumnDef(
            label: 'Thao tác',
            minWidth: 90,
            flex: 1,
            align: Alignment.center,
            cell: (r) {
              final self = r['userId'] == session?.userId;
              return PopupMenuButton<String>(
                tooltip: 'Thao tác',
                icon: const Icon(Icons.more_horiz),
                onSelected: (v) => switch (v) {
                  'edit' => _edit(r),
                  'pages' => context.go(
                    '/app/permissions?userId=${r['userId']}',
                  ),
                  'audit' => context.go(
                    '/app/audit-logs?userId=${r['userId']}',
                  ),
                  'lock' => _setActive(r, false),
                  'unlock' => _setActive(r, true),
                  _ => _delete(r),
                },
                itemBuilder: (_) => [
                  const PopupMenuItem(
                    value: 'edit',
                    child: Text('Sửa thông tin'),
                  ),
                  if (r['roleName'] != AppRole.admin.code)
                    const PopupMenuItem(
                      value: 'pages',
                      child: Text('Phân quyền trang'),
                    ),
                  const PopupMenuItem(
                    value: 'audit',
                    child: Text('Lịch sử thao tác'),
                  ),
                  if (!self)
                    r['isActive'] == true
                        ? const PopupMenuItem(
                            value: 'lock',
                            child: Text('Khóa tài khoản'),
                          )
                        : const PopupMenuItem(
                            value: 'unlock',
                            child: Text('Mở khóa tài khoản'),
                          ),
                  if (!self)
                    const PopupMenuItem(value: 'delete', child: Text('Xóa')),
                ],
              );
            },
          ),
      ],
    );
  }
}

/// Hộp thoại thêm (user = null) hoặc sửa nhân viên. Sửa: không đổi tên đăng nhập,
/// để trống mật khẩu thì giữ mật khẩu cũ.
class UserDialog extends ConsumerStatefulWidget {
  const UserDialog({super.key, this.user});

  final RowData? user;

  @override
  ConsumerState<UserDialog> createState() => _UserDialogState();
}

class _UserDialogState extends ConsumerState<UserDialog> {
  final _form = GlobalKey<FormState>();
  late final _username = TextEditingController(
    text: '${widget.user?['username'] ?? ''}',
  );
  late final _fullName = TextEditingController(
    text: '${widget.user?['fullName'] ?? ''}',
  );
  late final _email = TextEditingController(
    text: '${widget.user?['email'] ?? ''}',
  );
  final _password = TextEditingController();
  late int? _roleId = (widget.user?['roleId'] as num?)?.toInt();
  late bool _active = widget.user?['isActive'] != false;
  bool _saving = false;

  bool get _editing => widget.user != null;

  @override
  void dispose() {
    for (final c in [_username, _fullName, _email, _password]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _saving = true);
    final api = ref.read(apiClientProvider);
    final email = _email.text.trim();
    try {
      if (_editing) {
        await api.put(
          '/bff/api/users/${widget.user!['userId']}',
          data: {
            'fullName': _fullName.text.trim(),
            'email': email,
            'roleId': _roleId,
            'isActive': _active,
            if (_password.text.isNotEmpty) 'password': _password.text,
          },
        );
      } else {
        await api.post(
          '/bff/api/users',
          data: {
            'username': _username.text.trim(),
            'fullName': _fullName.text.trim(),
            'email': email.isEmpty ? null : email,
            'password': _password.text,
            'roleId': _roleId,
            'isActive': _active,
          },
        );
      }
      if (!mounted) return;
      Toast.success(
        context,
        _editing
            ? 'Đã cập nhật nhân viên ${_username.text.trim()}.'
            : 'Đã tạo tài khoản ${_username.text.trim()}.',
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        Toast.error(context, e);
        setState(() => _saving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final roles = ref.watch(rolesLookupProvider);
    final self =
        _editing &&
        widget.user!['userId'] == ref.watch(sessionProvider).value?.userId;
    return AlertDialog(
      title: Text(_editing ? 'Sửa nhân viên' : 'Thêm nhân viên'),
      content: SizedBox(
        width: 460,
        child: SingleChildScrollView(
          child: Form(
            key: _form,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AppTextField(
                  label: 'Tên đăng nhập',
                  required: true,
                  readOnly: _editing,
                  controller: _username,
                  validator: (v) =>
                      RegExp(
                        r'^[A-Za-z0-9._-]{3,50}$',
                      ).hasMatch(v?.trim() ?? '')
                      ? null
                      : 'Từ 3–50 ký tự: chữ không dấu, số, dấu . _ -',
                ),
                const SizedBox(height: 16),
                AppTextField(
                  label: 'Họ tên',
                  required: true,
                  controller: _fullName,
                ),
                const SizedBox(height: 16),
                AppTextField(
                  label: 'Email',
                  controller: _email,
                  keyboardType: TextInputType.emailAddress,
                  validator: (v) {
                    final t = v?.trim() ?? '';
                    return t.isEmpty ||
                            RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(t)
                        ? null
                        : 'Email không hợp lệ';
                  },
                ),
                const SizedBox(height: 16),
                AppTextField(
                  label: _editing
                      ? 'Mật khẩu mới (để trống nếu không đổi)'
                      : 'Mật khẩu',
                  required: !_editing,
                  obscure: true,
                  controller: _password,
                  validator: (v) {
                    if (_editing && (v == null || v.isEmpty)) return null;
                    return (v == null || v.length < 8)
                        ? 'Mật khẩu tối thiểu 8 ký tự'
                        : null;
                  },
                ),
                const SizedBox(height: 16),
                roles.when(
                  loading: () => const LinearProgressIndicator(),
                  error: (e, _) => Text(userMessage(e)),
                  data: (list) => AppDropdown<int>(
                    label: 'Vai trò',
                    required: true,
                    value: _roleId,
                    items: [
                      for (final r in list)
                        if (!self || r['roleName'] == AppRole.admin.code)
                          (
                            (r['id'] as num).toInt(),
                            AppRole.fromCode(r['roleName'] as String?).label,
                          ),
                    ],
                    onChanged: (v) => setState(() => _roleId = v),
                  ),
                ),
                const SizedBox(height: 4),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Đang hoạt động'),
                  subtitle: self
                      ? const Text('Không thể tự khóa tài khoản của mình')
                      : null,
                  value: _active,
                  onChanged: self ? null : (v) => setState(() => _active = v),
                ),
              ],
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.pop(context, false),
          child: const Text('Hủy'),
        ),
        ElevatedButton(
          onPressed: _saving ? null : _save,
          child: Text(
            _saving
                ? 'Đang lưu...'
                : (_editing ? 'Lưu thay đổi' : 'Tạo tài khoản'),
          ),
        ),
      ],
    );
  }
}

/// Figma 1334:3011 "Cài đặt / Thiết lập hệ thống": giao diện, mức VAT, thông tin phiên.
class SettingsPage extends ConsumerWidget {
  const SettingsPage({super.key});

  Future<void> _addVat(BuildContext context, WidgetRef ref) async {
    final values = await formDialog(
      context,
      title: 'Thêm mức VAT',
      fields: [
        ('vat', 'Nhãn (VD: VAT 8%)', '', true),
        ('rate', 'Thuế suất %', '', false),
      ],
    );
    if (values == null) return;
    try {
      await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/config/vat-options',
            data: {'vat': values['vat'], 'rate': parseNumber(values['rate'])},
          );
      ref.invalidate(vatOptionsLookupProvider);
      if (context.mounted) Toast.success(context, 'Đã thêm mức VAT.');
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.colors;
    final mode = ref.watch(themeModeProvider);
    final session = ref.watch(sessionProvider).value;
    final vats = ref.watch(vatOptionsLookupProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const PageHeader(
          title: 'Thiết lập hệ thống',
          crumbs: [Crumb('Trang chủ', '/app/dashboard'), Crumb('Cài đặt')],
        ),
        SectionCard(
          title: 'Giao diện',
          child: SegmentedButton<ThemeMode>(
            segments: const [
              ButtonSegment(
                value: ThemeMode.light,
                icon: Icon(Icons.light_mode_outlined),
                label: Text('Sáng'),
              ),
              ButtonSegment(
                value: ThemeMode.dark,
                icon: Icon(Icons.dark_mode_outlined),
                label: Text('Tối'),
              ),
              ButtonSegment(
                value: ThemeMode.system,
                icon: Icon(Icons.brightness_auto),
                label: Text('Theo hệ thống'),
              ),
            ],
            selected: {mode},
            onSelectionChanged: (s) =>
                ref.read(themeModeProvider.notifier).set(s.first),
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Mức thuế VAT',
          trailing: OutlinedButton(
            onPressed: () => _addVat(context, ref),
            child: const Text('Thêm mức VAT'),
          ),
          child: vats.when(
            loading: () => const LoadingView(),
            error: (e, _) => StateView.error(
              e,
              onRetry: () => ref.invalidate(vatOptionsLookupProvider),
            ),
            data: (rows) => rows.isEmpty
                ? const StateView.empty()
                : AppDataTable(
                    rows: rows,
                    columns: [
                      TableColumnDef.text(
                        'Nhãn',
                        (r) => '${r['vat']}',
                        flex: 3,
                        bold: true,
                      ),
                      TableColumnDef.text(
                        'Thuế suất',
                        (r) => r['rate'] == null
                            ? 'Không chịu thuế'
                            : '${r['rate']}%',
                        align: Alignment.centerRight,
                      ),
                    ],
                  ),
          ),
        ),
        const SizedBox(height: 16),
        SectionCard(
          title: 'Tài khoản & kết nối',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final (label, value) in [
                ('Người dùng', session?.displayName ?? '—'),
                ('Vai trò', session?.role.label ?? '—'),
                (
                  'Kho được phân công',
                  session == null
                      ? '—'
                      : session.warehouses.map((w) => w.name).join(', '),
                ),
                ('Máy chủ BFF', Env.bffBaseUrl),
              ])
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    children: [
                      SizedBox(
                        width: 200,
                        child: Text(label, style: TextStyle(color: c.muted)),
                      ),
                      Expanded(
                        child: Text(
                          value,
                          style: TextStyle(
                            color: c.text,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ],
    );
  }
}
