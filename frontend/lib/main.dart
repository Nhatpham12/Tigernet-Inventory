import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('vi_VN');
  runApp(
    ProviderScope(
      // lỗi API đã hiện bằng toast/màn lỗi có nút "Thử lại", không tự retry ngầm
      retry: (_, _) => null,
      child: const TigerNetApp(),
    ),
  );
}
