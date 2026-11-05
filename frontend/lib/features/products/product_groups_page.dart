import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../../shared/data_table/app_data_table.dart';
import '../../shared/feedback/dialogs.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/widgets/page_header.dart';
import '../common/lookups.dart';

/// Figma 968:321 "Sản phẩm / Danh mục": CRUD nhóm sản phẩm qua /bff/api/product-groups.
class ProductGroupsPage extends ConsumerWidget {
  const ProductGroupsPage({super.key});

  Future<void> _edit(
    BuildContext context,
    WidgetRef ref, [
    Map<String, dynamic>? group,
  ]) async {
    final result = await formDialog(
      context,
      title: group == null ? 'Thêm danh mục' : 'Sửa danh mục',
      fields: [
        ('groupName', 'Tên danh mục', '${group?['groupName'] ?? ''}', true),
      ],
    );
    if (result == null) return;
    try {
      final api = ref.read(apiClientProvider);
      if (group == null) {
        await api.post('/bff/api/product-groups', data: result);
      } else {
        await api.put('/bff/api/product-groups/${group['id']}', data: result);
      }
      ref.invalidate(productGroupsLookupProvider);
      if (context.mounted) Toast.success(context, 'Đã lưu danh mục.');
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  Future<void> _delete(
    BuildContext context,
    WidgetRef ref,
    Map<String, dynamic> group,
  ) async {
    final ok = await confirmDialog(
      context,
      title: 'Xóa danh mục',
      message:
          'Xóa "${group['groupName']}"? Chỉ xóa được khi danh mục không còn sản phẩm.',
      confirmLabel: 'Xóa',
      danger: true,
    );
    if (!ok) return;
    try {
      await ref
          .read(apiClientProvider)
          .delete('/bff/api/product-groups/${group['id']}');
      ref.invalidate(productGroupsLookupProvider);
      if (context.mounted) Toast.success(context, 'Đã xóa danh mục.');
    } catch (e) {
      if (context.mounted) Toast.error(context, e);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final groups = ref.watch(productGroupsLookupProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PageHeader(
          title: 'Danh mục sản phẩm',
          subtitle: 'Nhóm sản phẩm dùng để lọc và báo cáo',
          crumbs: const [
            Crumb('Trang chủ', '/app/dashboard'),
            Crumb('Sản phẩm', '/app/products'),
            Crumb('Danh mục'),
          ],
          actions: [
            ElevatedButton(
              onPressed: () => _edit(context, ref),
              child: const Text('Thêm danh mục'),
            ),
          ],
        ),
        groups.when(
          loading: () => const LoadingView(),
          error: (e, _) => StateView.error(
            e,
            onRetry: () => ref.invalidate(productGroupsLookupProvider),
          ),
          data: (rows) => rows.isEmpty
              ? const StateView.empty(
                  message:
                      'Chưa có danh mục. Thêm danh mục đầu tiên để phân loại sản phẩm.',
                )
              : AppDataTable(
                  rows: rows,
                  columns: [
                    TableColumnDef.text(
                      'Mã',
                      (r) => 'DM-${'${r['id']}'.padLeft(3, '0')}',
                      flex: 1,
                    ),
                    TableColumnDef.text(
                      'Tên danh mục',
                      (r) => '${r['groupName']}',
                      flex: 4,
                      bold: true,
                    ),
                    TableColumnDef(
                      label: 'Thao tác',
                      flex: 1,
                      align: Alignment.center,
                      cell: (r) => Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            tooltip: 'Sửa',
                            icon: const Icon(Icons.edit_outlined, size: 18),
                            onPressed: () => _edit(context, ref, r),
                          ),
                          IconButton(
                            tooltip: 'Xóa',
                            icon: const Icon(Icons.delete_outline, size: 18),
                            onPressed: () => _delete(context, ref, r),
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
