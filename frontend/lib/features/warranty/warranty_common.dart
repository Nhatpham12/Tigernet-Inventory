import 'package:flutter/material.dart';

import '../../shared/widgets/print_button.dart';
import '../../shared/widgets/status_chip.dart';

/// Trạng thái phiếu bảo hành: RECEIVED → SENT → RETURNED → CLOSED, hoặc RECEIVED → REJECTED → CLOSED.
const kWarrantyStatuses = <(String?, String)>[
  (null, 'Tất cả'),
  ('RECEIVED', 'Đã nhận'),
  ('SENT', 'Đã gửi đi'),
  ('RETURNED', 'Đã về'),
  ('CLOSED', 'Đã trả khách'),
  ('REJECTED', 'Từ chối'),
];

/// Chuyển trạng thái hợp lệ (khớp backend).
const kWarrantyTransitions = <String, List<String>>{
  'RECEIVED': ['SENT', 'REJECTED'],
  'SENT': ['RETURNED'],
  'RETURNED': ['CLOSED'],
  'REJECTED': ['CLOSED'],
};

const kWarrantyResults = <(String, String)>[
  ('REPAIRED', 'Đã sửa'),
  ('REPLACED', 'Đổi mới'),
  ('REJECTED', 'Từ chối bảo hành'),
];

String warrantyStatusLabel(String? s) => kWarrantyStatuses
    .firstWhere((e) => e.$1 == s, orElse: () => (s, s ?? '—'))
    .$2;

String warrantyResultLabel(String? s) => kWarrantyResults
    .firstWhere((e) => e.$1 == s, orElse: () => (s ?? '', s ?? '—'))
    .$2;

/// Nhãn nút / tiêu đề khi chuyển sang [target].
String warrantyActionLabel(String target) => switch (target) {
  'SENT' => 'Gửi đi bảo hành',
  'RETURNED' => 'Nhận hàng về',
  'CLOSED' => 'Trả khách',
  'REJECTED' => 'Từ chối bảo hành',
  _ => target,
};

StatusChip warrantyStatusChip(String? s) => switch (s) {
  'RECEIVED' => const StatusChip(label: 'Đã nhận', tone: ChipTone.info),
  'SENT' => const StatusChip(label: 'Đã gửi đi', tone: ChipTone.warning),
  'RETURNED' => const StatusChip(label: 'Đã về', tone: ChipTone.purple),
  'CLOSED' => const StatusChip(
    label: 'Đã trả khách',
    tone: ChipTone.success,
    icon: Icons.check,
  ),
  'REJECTED' => const StatusChip(label: 'Từ chối', tone: ChipTone.danger),
  _ => StatusChip(label: s ?? '—', tone: ChipTone.neutral),
};

/// Biên bản tiếp nhận bảo hành của một hoặc nhiều phiếu (in chung).
PrintDoc warrantyPrintDoc(Iterable<Object?> ids) => ids.length == 1
    ? PrintDoc(
        'Biên bản tiếp nhận bảo hành',
        '/bff/api/warranty-claims/${ids.first}/print',
      )
    : PrintDoc(
        'Biên bản tiếp nhận bảo hành (in chung)',
        '/bff/api/warranty-claims/print',
        query: {'ids': ids.join(',')},
      );
