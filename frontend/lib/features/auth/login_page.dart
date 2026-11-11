import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/session_controller.dart';
import '../../core/config/env.dart';
import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/brand_logo.dart';

/// Figma 962:1454 "Xác thực / Đăng nhập": khối thương hiệu vàng bên trái, form bên phải.
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _form = GlobalKey<FormState>();
  // bản demo điền sẵn tài khoản mẫu
  final _username = TextEditingController(text: Env.demoMode ? 'admin' : '');
  final _password = TextEditingController(
    text: Env.demoMode ? 'Admin@123' : '',
  );
  bool _obscure = true;
  bool _remember = true;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await ref
          .read(sessionProvider.notifier)
          .login(_username.text, _password.text, remember: _remember);
    } catch (e) {
      setState(() => _error = userMessage(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final wide = MediaQuery.sizeOf(context).width >= 1000;
    return Scaffold(
      backgroundColor: c.page,
      body: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (wide) const _BrandPanel(),
          Expanded(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(AppSpace.s24),
                child: Container(
                  constraints: const BoxConstraints(maxWidth: 544),
                  padding: const EdgeInsets.all(AppSpace.s32),
                  decoration: BoxDecoration(
                    color: c.surface,
                    border: Border.all(color: c.border),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Form(
                    key: _form,
                    child: AutofillGroup(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (!wide) ...[
                            // Màu theo token brandInk nên đọc được cả ở giao diện tối.
                            const BrandLogo(),
                            const SizedBox(height: AppSpace.s24),
                          ],
                          Text('Đăng nhập', style: context.text.display),
                          const SizedBox(height: AppSpace.s4),
                          Text(
                            'Nhập tài khoản để tiếp tục quản lý kho.',
                            style: context.text.body1.copyWith(color: c.muted),
                          ),
                          const SizedBox(height: AppSpace.s24),
                          AppTextField(
                            label: 'Tên đăng nhập',
                            required: true,
                            controller: _username,
                            hint: 'tendangnhap',
                            autofillHints: const [AutofillHints.username],
                          ),
                          const SizedBox(height: AppSpace.s16),
                          AppTextField(
                            label: 'Mật khẩu',
                            required: true,
                            controller: _password,
                            hint: 'Nhập mật khẩu',
                            obscure: _obscure,
                            autofillHints: const [AutofillHints.password],
                            onSubmitted: (_) => _submit(),
                            suffix: IconButton(
                              tooltip: _obscure
                                  ? 'Hiện mật khẩu'
                                  : 'Ẩn mật khẩu',
                              icon: Icon(
                                _obscure
                                    ? Icons.visibility_outlined
                                    : Icons.visibility_off_outlined,
                                color: c.muted,
                              ),
                              onPressed: () =>
                                  setState(() => _obscure = !_obscure),
                            ),
                          ),
                          const SizedBox(height: AppSpace.s8),
                          Row(
                            children: [
                              Expanded(
                                child: CheckboxListTile(
                                  value: _remember,
                                  onChanged: (v) =>
                                      setState(() => _remember = v ?? false),
                                  controlAffinity:
                                      ListTileControlAffinity.leading,
                                  contentPadding: EdgeInsets.zero,
                                  dense: true,
                                  title: Text(
                                    'Ghi nhớ đăng nhập',
                                    style: context.text.label,
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed: () => Toast.info(
                                  context,
                                  'Quên mật khẩu: liên hệ quản trị viên hoặc hotro@tigernet.vn để đặt lại.',
                                ),
                                child: const Text('Quên mật khẩu?'),
                              ),
                            ],
                          ),
                          if (Env.demoMode) ...[
                            const SizedBox(height: AppSpace.s8),
                            _Notice(
                              color: c.text,
                              background: c.warningBg,
                              text:
                                  'Bản demo dùng dữ liệu mẫu. Bấm Đăng nhập để vào; thêm/sửa/xóa sẽ không được lưu.',
                            ),
                          ],
                          if (_error != null) ...[
                            const SizedBox(height: AppSpace.s8),
                            _Notice(
                              color: c.danger,
                              background: c.dangerBg,
                              text: _error!,
                              icon: Icons.error_outline,
                            ),
                          ],
                          const SizedBox(height: AppSpace.s16),
                          SizedBox(
                            width: double.infinity,
                            height: AppSpace.touch,
                            child: ElevatedButton(
                              onPressed: _loading ? null : _submit,
                              child: _loading
                                  ? const SizedBox(
                                      width: 20,
                                      height: 20,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                      ),
                                    )
                                  : const Text('Đăng nhập'),
                            ),
                          ),
                          const SizedBox(height: AppSpace.s16),
                          // Chỉ là ghi chú, không phải link: dùng màu phụ.
                          Center(
                            child: Text(
                              'Chưa có tài khoản? Liên hệ quản trị viên để được cấp.',
                              textAlign: TextAlign.center,
                              style: context.text.body2.copyWith(
                                color: c.muted,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Notice extends StatelessWidget {
  const _Notice({
    required this.text,
    required this.color,
    required this.background,
    this.icon,
  });

  final String text;
  final Color color;
  final Color background;
  final IconData? icon;

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(AppSpace.s16),
    decoration: BoxDecoration(
      color: background,
      borderRadius: BorderRadius.circular(8),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (icon != null) ...[
          Icon(icon, size: 20, color: color),
          const SizedBox(width: AppSpace.s8),
        ],
        Expanded(
          child: Text(text, style: context.text.body1.copyWith(color: color)),
        ),
      ],
    ),
  );
}

/// Khối thương hiệu vàng bên trái, cao hết màn, sát mép (không bo góc).
class _BrandPanel extends StatelessWidget {
  const _BrandPanel();

  @override
  Widget build(BuildContext context) {
    // Nền vàng cố định ở cả hai giao diện nên chữ luôn lấy token của bản Light (9:1).
    const brand = AppColors.light;
    final onBrand = brand.onBrand;
    return Container(
      width: 560,
      padding: const EdgeInsets.all(AppSpace.s32),
      color: brand.brand,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          BrandLogo(size: BrandLogoSize.large, color: brand.brandInk),
          const SizedBox(height: AppSpace.s32),
          Text(
            'Quản lý kho rõ ràng.\nVận hành chủ động.',
            style: context.text.display.copyWith(color: onBrand),
          ),
          const SizedBox(height: AppSpace.s16),
          Text(
            'Theo dõi hàng hóa, nhập xuất, cho thuê, công nợ và thông báo trong một nơi.',
            style: context.text.bodyLarge.copyWith(color: onBrand),
          ),
          const SizedBox(height: AppSpace.s24),
          Text(
            'Hỗ trợ: hotro@tigernet.vn',
            style: context.text.body1.copyWith(color: onBrand),
          ),
        ],
      ),
    );
  }
}
