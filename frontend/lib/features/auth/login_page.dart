import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/session_controller.dart';
import '../../core/config/env.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_theme.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';

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
  bool _remember = false;
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
          .login(_username.text, _password.text);
    } catch (e) {
      setState(() => _error = '$e');
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
        children: [
          if (wide) const _BrandPanel(),
          Expanded(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Container(
                  width: 544,
                  padding: const EdgeInsets.all(32),
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
                            Text(
                              'TIGERNET INVENTORY',
                              style: const TextStyle(
                                fontFamily: AppTheme.brandFontFamily,
                                fontSize: 24,
                                color: Color(0xFF1F2E45),
                              ),
                            ),
                            const SizedBox(height: 16),
                          ],
                          Text(
                            'Đăng nhập',
                            style: TextStyle(
                              fontSize: 32,
                              fontWeight: FontWeight.w600,
                              height: 1.5,
                              color: c.text,
                            ),
                          ),
                          Text(
                            'Nhập tài khoản để tiếp tục quản lý kho.',
                            style: TextStyle(fontSize: 14, color: c.muted),
                          ),
                          const SizedBox(height: 16),
                          AppTextField(
                            label: 'Tên đăng nhập',
                            required: true,
                            controller: _username,
                            hint: 'tendangnhap',
                            autofillHints: const [AutofillHints.username],
                          ),
                          const SizedBox(height: 16),
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
                          const SizedBox(height: 16),
                          // Wrap: màn hẹp thì "Quên mật khẩu?" xuống dòng thay vì tràn ngang
                          SizedBox(
                            width: double.infinity,
                            child: Wrap(
                              alignment: WrapAlignment.spaceBetween,
                              crossAxisAlignment: WrapCrossAlignment.center,
                              spacing: 8,
                              children: [
                                InkWell(
                                  onTap: () =>
                                      setState(() => _remember = !_remember),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Checkbox(
                                        value: _remember,
                                        onChanged: (v) =>
                                            setState(() => _remember = v!),
                                      ),
                                      Text(
                                        'Ghi nhớ đăng nhập',
                                        style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.w600,
                                          color: c.text,
                                        ),
                                      ),
                                    ],
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
                          ),
                          if (Env.demoMode) ...[
                            const SizedBox(height: 8),
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: c.warningBg,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                'Bản demo dùng dữ liệu mẫu. Bấm Đăng nhập để vào; thêm/sửa/xóa sẽ không được lưu.',
                                style: TextStyle(color: c.text),
                              ),
                            ),
                          ],
                          if (_error != null) ...[
                            const SizedBox(height: 8),
                            Container(
                              width: double.infinity,
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: c.dangerBg,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                _error!,
                                style: TextStyle(color: c.danger),
                              ),
                            ),
                          ],
                          const SizedBox(height: 16),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: _loading ? null : _submit,
                              child: _loading
                                  ? const SizedBox(
                                      width: 18,
                                      height: 18,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                      ),
                                    )
                                  : const Text('Đăng nhập'),
                            ),
                          ),
                          const SizedBox(height: 8),
                          Center(
                            child: Text(
                              'Chưa có tài khoản? Liên hệ quản trị viên để được cấp.',
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: c.link,
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

class _BrandPanel extends StatelessWidget {
  const _BrandPanel();

  @override
  Widget build(BuildContext context) {
    const onBrand = Color(0xFF1F2937);
    return Container(
      width: 570,
      padding: const EdgeInsets.all(48),
      decoration: BoxDecoration(
        color: const Color(0xFFFFC107),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          IntrinsicWidth(
            child: Column(
              children: [
                Text(
                  'TIGERNET',
                  style: const TextStyle(
                    fontFamily: AppTheme.brandFontFamily,
                    fontSize: 32,
                    color: Color(0xFF1F2E45),
                  ),
                ),
                Text(
                  'INVENTORY',
                  style: const TextStyle(
                    fontFamily: AppTheme.brandFontFamily,
                    fontSize: 48,
                    color: Color(0xFF1F2E45),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Quản lý kho rõ ràng.\nVận hành chủ động.',
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.w600,
              height: 1.5,
              color: onBrand,
            ),
          ),
          const SizedBox(height: 16),
          const SizedBox(
            width: 474,
            child: Text(
              'Theo dõi hàng hóa, nhập xuất, cho thuê, công nợ và thông báo trong một nơi.',
              style: TextStyle(fontSize: 18, height: 1.5, color: onBrand),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Hỗ trợ: hotro@tigernet.vn',
            style: TextStyle(fontSize: 14, color: onBrand),
          ),
        ],
      ),
    );
  }
}
