/// Khớp PageResponse của BFF (page bắt đầu từ 1).
class PageData<T> {
  const PageData({
    required this.items,
    required this.page,
    required this.size,
    required this.totalItems,
    required this.totalPages,
  });

  final List<T> items;
  final int page;
  final int size;
  final int totalItems;
  final int totalPages;

  factory PageData.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromItem,
  ) {
    final raw = (json['items'] as List?) ?? const [];
    return PageData(
      items: raw
          .map((e) => fromItem(Map<String, dynamic>.from(e as Map)))
          .toList(),
      page: (json['page'] as num?)?.toInt() ?? 1,
      size: (json['size'] as num?)?.toInt() ?? raw.length,
      totalItems: (json['totalItems'] as num?)?.toInt() ?? raw.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 1,
    );
  }
}
