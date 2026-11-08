/// Lỗi trả về từ BFF/backend, đã đổi sang câu tiếng Việt để hiển thị toast.
class ApiException implements Exception {
  ApiException({
    required this.statusCode,
    required this.message,
    this.code,
    this.fieldErrors = const {},
  });

  final int statusCode;
  final int? code;
  final String message;
  final Map<String, String> fieldErrors;

  bool get isUnauthorized => statusCode == 401;
  bool get isForbidden => statusCode == 403;

  @override
  String toString() => message;
}

/// Mã lỗi backend (ErrorCode.java) → thông báo tiếng Việt.
const Map<int, String> kErrorMessages = {
  1001: 'Không tìm thấy người dùng.',
  1002: 'Tên đăng nhập đã tồn tại.',
  1003: 'Sai tên đăng nhập hoặc mật khẩu.',
  1004: 'Tài khoản đã bị khóa.',
  1005: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
  1006: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.',
  1201: 'Không tìm thấy kho.',
  1202: 'Mã kho đã tồn tại.',
  1203: 'Tên kho đã tồn tại.',
  1204: 'Kho đã ngừng hoạt động.',
  1205: 'Kho đang được sử dụng, không thể xóa.',
  1301: 'Không tìm thấy danh mục sản phẩm.',
  1302: 'Tên danh mục đã tồn tại.',
  1303: 'Danh mục vẫn còn sản phẩm.',
  1304: 'Không tìm thấy sản phẩm.',
  1305: 'Tên sản phẩm đã tồn tại trong danh mục.',
  1307: 'Không tìm thấy mức VAT.',
  1308: 'Mức VAT đã tồn tại.',
  1309: 'Mức VAT đang được dùng, hãy thêm mức mới.',
  1401: 'Không tìm thấy đối tác.',
  1402: 'Mã số thuế đã tồn tại.',
  1403: 'Cần chọn đối tác có sẵn hoặc nhập thông tin mới.',
  1404: 'Không tìm thấy nhà cung cấp.',
  1405: 'Đối tác này đã là nhà cung cấp.',
  1408: 'Không tìm thấy khách hàng.',
  1409: 'Đối tác này đã là khách hàng.',
  1501: 'Không tìm thấy hàng trong kho.',
  1504: 'Số serial đã tồn tại trong kho.',
  2001: 'Phiếu không thể sửa ở trạng thái hiện tại.',
  2004: 'Số tiền thanh toán vượt quá số còn nợ.',
  2006: 'Hạn trả phải sau ngày xuất.',
  2016: 'Một sản phẩm xuất hiện nhiều lần.',
  2019: 'Kho đi và kho đến phải khác nhau.',
  2020: 'Dòng có serial phải có số lượng 1.',
  2021: 'Số serial bị trùng.',
  2027: 'Phiếu gốc hoặc đối tượng không hợp lệ cho phiếu thu/chi.',
  2028: 'Phiếu đã hủy trước đó.',
  2029: 'Không chuyển được sang trạng thái này.',
  9503: 'Không kết nối được máy chủ. Vui lòng thử lại.',
};
