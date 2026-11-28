import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';

typedef Json = Map<String, dynamic>;

/// Danh mục dùng cho dropdown trên form (lấy qua proxy /bff/api/**).
final warehousesLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref
      .watch(apiClientProvider)
      .getList('/bff/api/warehouses', query: {'isActive': true}),
);

final suppliersLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/suppliers'),
);

final customersLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/customers'),
);

final productsLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/products'),
);

final productGroupsLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/product-groups'),
);

final vatOptionsLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/config/vat-options'),
);

/// Lựa chọn VAT trên phiếu nhập/xuất: một mục "Không chịu thuế" (để trống). Mục trong danh mục
/// có thuế suất rỗng (nhãn "NO") cùng nghĩa nên ẩn đi cho khỏi trùng — trừ khi phiếu đang chọn
/// đúng mục đó (phiếu cũ), để ô chọn vẫn hiện được giá trị hiện tại.
List<(int?, String)> vatDropdownItems(List<Json> options, int? selectedId) => [
  (null, 'Không chịu thuế'),
  for (final v in options)
    if (v['rate'] != null || (v['id'] as num).toInt() == selectedId)
      ((v['id'] as num).toInt(), '${v['vat']}'),
];

final rolesLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/roles'),
);

String partyName(Json? row) =>
    (row?['party'] as Map?)?['fullName'] as String? ?? '';
