import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../core/auth/session_controller.dart';
import '../../core/network/api_exception.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import '../../shared/form/fields.dart';

/// Bước 2 đăng nhập của Quản trị: nhập mã 6 số từ app Authenticator.
/// Lần đầu: quét QR để cài, nhập mã đầu tiên, rồi lưu 8 mã khôi phục.
class MfaStep extends ConsumerStatefulWidget {
  const MfaStep({super.key, required this.challenge, required this.onBack});

  final MfaChallenge challenge;
  final VoidCallback onBack;

  @override
  ConsumerState<MfaStep> createState() => _MfaStepState();
}

class _MfaStepState extends ConsumerState<MfaStep> {
  final _form = GlobalKey<FormState>();
  final _code = TextEditingController();
  ({String secret, String otpauthUri})? _setup;
  bool _useRecovery = false;
  bool _loading = false;
  String? _error;

  SessionController get _session => ref.read(sessionProvider.notifier);

  @override
  void initState() {
    super.initState();
    if (widget.challenge.setupRequired) _loadSetup();
  }

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _loadSetup() async {
    setState(() => _loading = true);
    try {
      final setup = await _session.mfaSetup(widget.challenge);
      if (mounted) setState(() => _setup = setup);
    } catch (e) {
      if (mounted) setState(() => _error = userMessage(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final value = _code.text.trim();
      final recoveryCodes = await _session.mfaSubmit(
        widget.challenge,
        code: _useRecovery ? null : value,
        recoveryCode: _useRecovery ? value : null,
      );
      if (recoveryCodes.isNotEmpty && mounted) {
        await _showRecoveryCodes(recoveryCodes);
      }
      await _session.finishLogin();
    } catch (e) {
      if (mounted) setState(() => _error = userMessage(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _showRecoveryCodes(List<String> codes) => showDialog<void>(
    context: context,
    barrierDismissible: false,
    builder: (context) => AlertDialog(
      title: const Text('Lưu mã khôi phục'),
      content: SizedBox(
        width: 360,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Mỗi mã dùng được một lần để đăng nhập khi mất điện thoại. '
              'Mã chỉ hiện lần này: hãy chép và cất ở nơi an toàn.',
            ),
            const SizedBox(height: AppSpace.s16),
            SelectableText(
              codes.join('\n'),
              style: const TextStyle(fontFamily: 'monospace', fontSize: 16),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () =>
              Clipboard.setData(ClipboardData(text: codes.join('\n'))),
          child: const Text('Sao chép'),
        ),
        ElevatedButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Tôi đã lưu'),
        ),
      ],
    ),
  );

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final setupMode = widget.challenge.setupRequired;
    return Form(
      key: _form,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Xác thực 2 bước', style: context.text.display),
          const SizedBox(height: AppSpace.s4),
          Text(
            setupMode
                ? 'Tài khoản Quản trị cần mã 6 số từ ứng dụng Google Authenticator '
                      'hoặc Microsoft Authenticator. Quét mã QR rồi nhập mã ứng dụng hiện ra.'
                : 'Nhập mã 6 số đang hiện trong ứng dụng Authenticator.',
            style: context.text.body1.copyWith(color: c.muted),
          ),
          const SizedBox(height: AppSpace.s16),
          if (setupMode && _setup != null) ...[
            Center(
              child: Container(
                color: Colors
                    .white, // QR cần nền sáng để quét được cả ở giao diện tối
                padding: const EdgeInsets.all(AppSpace.s8),
                child: QrImageView(
                  data: _setup!.otpauthUri,
                  size: 180,
                  semanticsLabel: 'Mã QR cài xác thực 2 bước',
                ),
              ),
            ),
            const SizedBox(height: AppSpace.s8),
            Text(
              'Không quét được? Nhập khoá này vào ứng dụng:',
              style: context.text.label,
            ),
            SelectableText(
              _setup!.secret,
              style: const TextStyle(fontFamily: 'monospace'),
            ),
            const SizedBox(height: AppSpace.s16),
          ],
          AppTextField(
            label: _useRecovery ? 'Mã khôi phục' : 'Mã 6 số',
            required: true,
            controller: _code,
            hint: _useRecovery ? 'XXXX-XXXX' : '123456',
            keyboardType: _useRecovery
                ? TextInputType.text
                : TextInputType.number,
            inputFormatters: _useRecovery
                ? [LengthLimitingTextInputFormatter(9)]
                : [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(6),
                  ],
            autofillHints: const [AutofillHints.oneTimeCode],
            onSubmitted: (_) => _submit(),
          ),
          if (!setupMode)
            TextButton(
              onPressed: () => setState(() {
                _useRecovery = !_useRecovery;
                _code.clear();
              }),
              child: Text(
                _useRecovery
                    ? 'Dùng mã 6 số'
                    : 'Mất điện thoại? Dùng mã khôi phục',
              ),
            ),
          if (_error != null) ...[
            const SizedBox(height: AppSpace.s8),
            Text(_error!, style: context.text.body2.copyWith(color: c.danger)),
          ],
          const SizedBox(height: AppSpace.s16),
          Row(
            children: [
              TextButton(
                onPressed: _loading ? null : widget.onBack,
                child: const Text('Quay lại'),
              ),
              const Spacer(),
              ElevatedButton(
                onPressed: _loading || (setupMode && _setup == null)
                    ? null
                    : _submit,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Xác nhận'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
