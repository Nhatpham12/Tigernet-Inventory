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

final rolesLookupProvider = FutureProvider.autoDispose<List<Json>>(
  (ref) => ref.watch(apiClientProvider).getList('/bff/api/roles'),
);

String partyName(Json? row) =>
    (row?['party'] as Map?)?['fullName'] as String? ?? '';
