import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/role.dart';
import '../../core/auth/session_controller.dart';
import '../../core/config/env.dart';
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

/// Người dùng (QT-01) — GET /bff/users, tạo qua /bff/api/users.
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

  Future<void> _add() async {
    final created = await showDialog<bool>(
      context: context,
      builder: (_) => const _UserDialog(),
    );
    if (created == true) _controller.reload();
  }

  @override
  Widget build(BuildContext context) => PagedListView(
    title: 'Người dùng',
    subtitle: 'Tài khoản, vai trò và trạng thái hoạt động',
    crumbs: const [Crumb('Trang chủ', '/app/dashboard'), Crumb('Người dùng')],
    controller: _controller,
    actions: [
      ElevatedButton(onPressed: _add, child: const Text('Thêm người dùng')),
    ],
    endpoint: '/bff/users',
    useWarehouseScope: false,
    searchHint: 'Tìm tên đăng nhập, họ tên, email...',
    itemLabel: 'người dùng',
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
    ],
  );
}

class _UserDialog extends ConsumerStatefulWidget {
  const _UserDialog();

  @override
  ConsumerState<_UserDialog> createState() => _UserDialogState();
}

class _UserDialogState extends ConsumerState<_UserDialog> {
  final _form = GlobalKey<FormState>();
  final _username = TextEditingController();
  final _fullName = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  int? _roleId;
  bool _saving = false;

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
    try {
      await ref
          .read(apiClientProvider)
          .post(
            '/bff/api/users',
            data: {
              'username': _username.text.trim(),
              'fullName': _fullName.text.trim(),
              'email': _email.text.trim().isEmpty ? null : _email.text.trim(),
              'password': _password.text,
              'roleId': _roleId,
              'isActive': true,
            },
          );
      if (!mounted) return;
      Toast.success(context, 'Đã tạo tài khoản ${_username.text.trim()}.');
      Navigator.pop(context, true);
    } catch (e) {
      if (mounted) Toast.error(context, e);
      setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final roles = ref.watch(rolesLookupProvider);
    return AlertDialog(
      title: const Text('Thêm người dùng'),
      content: SizedBox(
        width: 460,
        child: Form(
          key: _form,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppTextField(
                label: 'Tên đăng nhập',
                required: true,
                controller: _username,
              ),
              const SizedBox(height: 12),
              AppTextField(
                label: 'Họ tên',
                required: true,
                controller: _fullName,
              ),
              const SizedBox(height: 12),
              AppTextField(
                label: 'Email',
                controller: _email,
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 12),
              AppTextField(
                label: 'Mật khẩu',
                required: true,
                obscure: true,
                controller: _password,
                validator: (v) => (v == null || v.length < 8)
                    ? 'Mật khẩu tối thiểu 8 ký tự'
                    : null,
              ),
              const SizedBox(height: 12),
              roles.when(
                loading: () => const LinearProgressIndicator(),
                error: (e, _) => Text('$e'),
                data: (list) => AppDropdown<int>(
                  label: 'Vai trò',
                  required: true,
                  value: _roleId,
                  items: [
                    for (final r in list)
                      (
                        (r['id'] as num).toInt(),
                        AppRole.fromCode(r['roleName'] as String?).label,
                      ),
                  ],
                  onChanged: (v) => setState(() => _roleId = v),
                ),
              ),
            ],
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
          child: Text(_saving ? 'Đang lưu...' : 'Tạo tài khoản'),
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
