/// Cấu hình môi trường. Đổi địa chỉ BFF khi build:
/// `flutter run -d chrome --dart-define=BFF_BASE_URL=http://localhost:8081`
class Env {
  const Env._();

  static const String bffBaseUrl = String.fromEnvironment(
    'BFF_BASE_URL',
    defaultValue: 'http://localhost:8081',
  );
}
