import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../core/utils/file_saver.dart';
import '../feedback/toast.dart';

/// Một loại giấy tờ in được từ phiếu, VD "Phiếu xuất kho 02-VT".
class PrintDoc {
  const PrintDoc(this.label, this.path, {this.query = const {}});

  final String label;

  /// Đường dẫn qua BFF, VD `/bff/api/export-receipts/12/print`.
  final String path;
  final Map<String, dynamic> query;
}

/// Nút "In giấy tờ": chọn loại giấy tờ rồi Xem/In (mở PDF) hoặc Tải PDF.
/// Tên file do backend đặt: tên nghiệp vụ + thời gian thực hiện.
class PrintButton extends ConsumerWidget {
  const PrintButton({super.key, required this.docs, this.compact = false});

  final List<PrintDoc> docs;

  /// true: chỉ hiện icon (dùng trong cột Thao tác của bảng).
  final bool compact;

  static Future<void> run(
    BuildContext context,
    WidgetRef ref,
    PrintDoc doc, {
    required bool download,
  }) async {
    final api = ref.read(apiClientProvider);
    try {
      final file = await api.getFile(
        doc.path,
        query: {...doc.query, if (download) 'download': 'true'},
      );
      final saved = download
          ? await FileSaver.save(file)
          : await FileSaver.open(file);
      if (!context.mounted) return;
      if (download || saved != null) {
        Toast.success(
          context,
          saved == null ? 'Đã tải ${file.name}' : 'Đã lưu $saved',
        );
      }
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final entries = <PopupMenuEntry<(PrintDoc, bool)>>[];
    for (final d in docs) {
      if (docs.length > 1) {
        entries.add(
          PopupMenuItem(
            enabled: false,
            height: 32,
            child: Text(
              d.label,
              style: Theme.of(context).textTheme.labelMedium,
            ),
          ),
        );
      }
      entries
        ..add(
          PopupMenuItem(
            value: (d, false),
            child: ListTile(
              dense: true,
              leading: const Icon(Icons.print_outlined),
              title: Text(docs.length > 1 ? 'Xem / In' : 'Xem / In ${d.label}'),
            ),
          ),
        )
        ..add(
          PopupMenuItem(
            value: (d, true),
            child: ListTile(
              dense: true,
              leading: const Icon(Icons.download_outlined),
              title: Text(docs.length > 1 ? 'Tải PDF' : 'Tải PDF ${d.label}'),
            ),
          ),
        );
    }
    return PopupMenuButton<(PrintDoc, bool)>(
      tooltip: 'In giấy tờ',
      onSelected: (v) => run(context, ref, v.$1, download: v.$2),
      itemBuilder: (_) => entries,
      child: compact
          ? const Padding(
              padding: EdgeInsets.all(8),
              child: Icon(Icons.print_outlined),
            )
          : IgnorePointer(
              child: OutlinedButton.icon(
                onPressed: () {},
                icon: const Icon(Icons.print_outlined),
                label: const Text('In giấy tờ'),
              ),
            ),
    );
  }
}
