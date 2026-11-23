/// Cấu hình môi trường. Đổi địa chỉ BFF khi build:
/// `flutter run -d chrome --dart-define=BFF_BASE_URL=http://localhost:8081`
/// Để rỗng (`--dart-define=BFF_BASE_URL=`, bản Docker) thì gọi BFF cùng tên miền với trang web.
class Env {
  const Env._();

  static const String _bffBaseUrl = String.fromEnvironment(
    'BFF_BASE_URL',
    defaultValue: 'http://localhost:8081',
  );

  static String get bffBaseUrl =>
      _bffBaseUrl.isEmpty ? Uri.base.origin : _bffBaseUrl;

  /// `--dart-define=DEMO_MODE=true`: chạy bằng dữ liệu mẫu, không cần BFF/backend.
  static const bool demoMode = bool.fromEnvironment('DEMO_MODE');
}
