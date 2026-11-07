import '../network/api_client.dart';
import 'file_saver_io.dart' if (dart.library.js_interop) 'file_saver_web.dart'
    as impl;

/// Lưu file tải từ API (PDF giấy tờ). Web: trình duyệt tải về (hoặc mở tab xem);
/// Windows/Android: ghi vào thư mục Tải về/tạm và trả lại đường dẫn.
class FileSaver {
  const FileSaver._();

  /// Trả về đường dẫn đã lưu (chỉ có trên bản cài đặt), null trên web.
  static Future<String?> save(DownloadedFile file) => impl.saveFile(file);

  /// Mở file để xem/in (web: tab mới; bản cài đặt: lưu rồi trả đường dẫn).
  static Future<String?> open(DownloadedFile file) => impl.openFile(file);
}
