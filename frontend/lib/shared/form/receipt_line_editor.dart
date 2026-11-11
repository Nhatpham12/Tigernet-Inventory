import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import 'fields.dart';

/// Một dòng hàng trên phiếu (nhập / xuất / thuê / mượn / chuyển kho).
class ReceiptLine {
  ReceiptLine({required this.product, num? price, int quantity = 1})
    : qty = TextEditingController(text: '$quantity'),
      price = TextEditingController(
        text: price == null ? '' : '${price.toInt()}',
      ),
      serial = TextEditingController(),
      warranty = TextEditingController();

  final Map<String, dynamic> product;
  final TextEditingController qty;
  final TextEditingController price;
  final TextEditingController serial;
  final TextEditingController warranty;

  int get productId => (product['id'] as num).toInt();
  int get quantity => parseNumber(qty.text)?.toInt() ?? 0;
  num get unitPrice => parseNumber(price.text) ?? 0;
  num get lineTotal => quantity * unitPrice;
  String? get serialValue =>
      serial.text.trim().isEmpty ? null : serial.text.trim();
  int? get warrantyMonths => parseNumber(warranty.text)?.toInt();

  void dispose() {
    qty.dispose();
    price.dispose();
    serial.dispose();
    warranty.dispose();
  }
}

/// Bảng "Danh sách hàng hóa" (Figma 978:1453): đổi SL, thêm/xóa dòng, ô tìm SKU để thêm hàng.
class ReceiptLineEditor extends StatelessWidget {
  const ReceiptLineEditor({
    super.key,
    required this.lines,
    required this.products,
    required this.onChanged,
    required this.defaultPrice,
    this.priceLabel = 'Đơn giá',
    this.showPrice = true,
    this.showSerial = true,
    this.showWarranty = false,
    this.onCreateProduct,
  });

  /// Có giá trị thì hiện nút "Thêm sản phẩm mới" cạnh ô tìm: tạo xong (trả về sản phẩm) thì thêm luôn vào phiếu.
  final Future<Map<String, dynamic>?> Function()? onCreateProduct;

  final List<ReceiptLine> lines;
  final List<Map<String, dynamic>> products;
  final VoidCallback onChanged;
  final num? Function(Map<String, dynamic> product) defaultPrice;
  final String priceLabel;
  final bool showPrice;
  final bool showSerial;
  final bool showWarranty;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final header = TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w700,
      color: c.text,
    );
    Widget cell(
      Widget child, {
      int flex = 2,
      Alignment align = Alignment.centerLeft,
    }) => Expanded(
      flex: flex,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
        child: Align(alignment: align, child: child),
      ),
    );
    final table = Column(
      children: [
        Container(
          color: c.surfaceMuted,
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
          child: Row(
            children: [
              cell(Text('SKU / Sản phẩm', style: header), flex: 5),
              cell(Text('ĐVT', style: header), flex: 1),
              cell(
                Text('SL', style: header),
                flex: 2,
                align: Alignment.centerRight,
              ),
              if (showPrice)
                cell(
                  Text(priceLabel, style: header),
                  flex: 3,
                  align: Alignment.centerRight,
                ),
              if (showPrice)
                cell(
                  Text('Thành tiền', style: header),
                  flex: 3,
                  align: Alignment.centerRight,
                ),
              if (showSerial) cell(Text('Serial', style: header), flex: 3),
              if (showWarranty)
                cell(Text('BH (tháng)', style: header), flex: 2),
              cell(
                Text('Thao tác', style: header),
                flex: 1,
                align: Alignment.center,
              ),
            ],
          ),
        ),
        for (final line in lines)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: c.divider)),
            ),
            child: Row(
              children: [
                cell(
                  Text(
                    '${Fmt.productCode(line.productId)} · ${line.product['productName']}',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: c.text,
                    ),
                  ),
                  flex: 5,
                ),
                cell(
                  Text(
                    '${line.product['unit'] ?? ''}',
                    style: TextStyle(fontSize: 13, color: c.text),
                  ),
                  flex: 1,
                ),
                cell(
                  _NumberBox(controller: line.qty, onChanged: onChanged),
                  flex: 2,
                  align: Alignment.centerRight,
                ),
                if (showPrice)
                  cell(
                    _NumberBox(
                      controller: line.price,
                      onChanged: onChanged,
                      suffix: '₫',
                    ),
                    flex: 3,
                    align: Alignment.centerRight,
                  ),
                if (showPrice)
                  cell(
                    Text(
                      Fmt.money(line.lineTotal),
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: c.text,
                      ),
                    ),
                    flex: 3,
                    align: Alignment.centerRight,
                  ),
                if (showSerial)
                  cell(
                    TextField(
                      controller: line.serial,
                      style: const TextStyle(fontSize: 13),
                      decoration: const InputDecoration(hintText: 'SN-...'),
                    ),
                    flex: 3,
                  ),
                if (showWarranty)
                  cell(
                    _NumberBox(controller: line.warranty, onChanged: onChanged),
                    flex: 2,
                  ),
                cell(
                  IconButton(
                    tooltip: 'Xóa dòng',
                    icon: Icon(Icons.delete_outline, color: c.danger, size: 20),
                    style: IconButton.styleFrom(
                      backgroundColor: c.dangerBg,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(6),
                      ),
                    ),
                    onPressed: () {
                      lines.remove(line);
                      line.dispose();
                      onChanged();
                    },
                  ),
                  flex: 1,
                  align: Alignment.center,
                ),
              ],
            ),
          ),
      ],
    );
    return Column(
      children: [
        LayoutBuilder(
          builder: (context, constraints) => constraints.maxWidth < 900
              ? SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: SizedBox(width: 900, child: table),
                )
              : table,
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _ProductSearch(products: products, onSelected: _add),
            ),
            if (onCreateProduct != null) ...[
              const SizedBox(width: 12),
              OutlinedButton.icon(
                icon: const Icon(Icons.add_box_outlined, size: 18),
                label: const Text('Thêm sản phẩm mới'),
                onPressed: () async {
                  final created = await onCreateProduct!();
                  if (created != null && created['id'] != null) _add(created);
                },
              ),
            ],
          ],
        ),
      ],
    );
  }

  /// Thêm sản phẩm vào phiếu: hàng theo số lượng đã có dòng thì cộng 1, còn lại thêm dòng mới.
  void _add(Map<String, dynamic> p) {
    final existing = lines.where(
      (l) => l.productId == (p['id'] as num).toInt() && l.serialValue == null,
    );
    if (existing.isNotEmpty && p['isSerialized'] != true) {
      final line = existing.first;
      line.qty.text = '${line.quantity + 1}';
    } else {
      lines.add(ReceiptLine(product: p, price: defaultPrice(p)));
    }
    onChanged();
  }
}

class _NumberBox extends StatelessWidget {
  const _NumberBox({
    required this.controller,
    required this.onChanged,
    this.suffix,
  });

  final TextEditingController controller;
  final VoidCallback onChanged;
  final String? suffix;

  @override
  Widget build(BuildContext context) => TextField(
    controller: controller,
    textAlign: TextAlign.right,
    keyboardType: TextInputType.number,
    inputFormatters: digitsOnly,
    style: const TextStyle(fontSize: 13),
    onChanged: (_) => onChanged(),
    decoration: InputDecoration(suffixText: suffix),
  );
}

class _ProductSearch extends StatelessWidget {
  const _ProductSearch({required this.products, required this.onSelected});

  final List<Map<String, dynamic>> products;
  final ValueChanged<Map<String, dynamic>> onSelected;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Autocomplete<Map<String, dynamic>>(
      displayStringForOption: (p) =>
          '${Fmt.productCode(p['id'])} · ${p['productName']}',
      optionsBuilder: (value) {
        final q = value.text.trim().toLowerCase();
        if (q.isEmpty) return products.take(20);
        return products
            .where(
              (p) => '${Fmt.productCode(p['id'])} ${p['productName']}'
                  .toLowerCase()
                  .contains(q),
            )
            .take(20);
      },
      onSelected: onSelected,
      fieldViewBuilder: (context, controller, focusNode, onSubmit) {
        return TextField(
          controller: controller,
          focusNode: focusNode,
          onSubmitted: (_) => onSubmit(),
          onTap: () => controller.clear(),
          decoration: InputDecoration(
            hintText: 'Tìm mã SKU hoặc tên sản phẩm để thêm hàng ngay tại đây',
            prefixIcon: Icon(Icons.add, color: c.info),
            fillColor: c.infoBg.withValues(alpha: 0.4),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: BorderSide(color: c.info.withValues(alpha: 0.5)),
            ),
          ),
        );
      },
    );
  }
}

/// Khối tổng tiền bên phải phiếu: Tạm tính, VAT, Tổng cộng, Đã thanh toán, Còn lại.
class TotalsPanel extends StatelessWidget {
  const TotalsPanel({super.key, required this.rows});

  final List<(String label, String value, bool emphasize)> rows;

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    return Column(
      children: [
        for (final (label, value, emphasize) in rows)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    label,
                    style: TextStyle(fontSize: 14, color: c.muted),
                  ),
                ),
                Text(
                  value,
                  style: TextStyle(
                    fontSize: emphasize ? 20 : 14,
                    fontWeight: emphasize ? FontWeight.w800 : FontWeight.w600,
                    color: emphasize ? c.accent : c.text,
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
