import 'package:flutter/material.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_tokens.dart';
import '../../core/utils/formatters.dart';
import 'fields.dart';
import 'unsaved_changes.dart';

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
  });

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
    // Mọi thay đổi dòng hàng cũng tính là "đang nhập dở" (TextField ở đây không phải FormField).
    void changed() {
      UnsavedChanges.mark();
      onChanged();
    }

    final header = context.text.label.copyWith(fontWeight: FontWeight.w700);
    final cellText = context.text.body2;

    Widget deleteButton(ReceiptLine line) => IconButton(
      tooltip: 'Xóa dòng',
      icon: Icon(Icons.delete_outline, color: c.danger, size: 20),
      style: IconButton.styleFrom(
        backgroundColor: c.dangerBg,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
      onPressed: () {
        lines.remove(line);
        line.dispose();
        changed();
      },
    );

    Widget serialField(ReceiptLine line) => TextField(
      controller: line.serial,
      style: cellText,
      onChanged: (_) => UnsavedChanges.mark(),
      decoration: const InputDecoration(hintText: 'SN-...'),
    );

    Widget cell(
      Widget child, {
      int flex = 2,
      Alignment align = Alignment.centerLeft,
    }) => Expanded(
      flex: flex,
      child: Padding(
        padding: const EdgeInsets.all(AppSpace.s4),
        child: Align(alignment: align, child: child),
      ),
    );

    final table = Column(
      children: [
        Container(
          color: c.surfaceMuted,
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpace.s4,
            vertical: AppSpace.s8,
          ),
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
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpace.s4,
              vertical: AppSpace.s2,
            ),
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: c.divider)),
            ),
            child: Row(
              children: [
                cell(
                  Text(
                    '${Fmt.productCode(line.productId)} · ${line.product['productName']}',
                    style: cellText.copyWith(fontWeight: FontWeight.w600),
                  ),
                  flex: 5,
                ),
                cell(Text('${line.product['unit'] ?? ''}', style: cellText), flex: 1),
                cell(
                  _NumberBox(controller: line.qty, onChanged: changed),
                  flex: 2,
                  align: Alignment.centerRight,
                ),
                if (showPrice)
                  cell(
                    _NumberBox(
                      controller: line.price,
                      onChanged: changed,
                      suffix: '₫',
                    ),
                    flex: 3,
                    align: Alignment.centerRight,
                  ),
                if (showPrice)
                  cell(
                    Text(
                      Fmt.money(line.lineTotal),
                      style: context.text.label.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    flex: 3,
                    align: Alignment.centerRight,
                  ),
                if (showSerial) cell(serialField(line), flex: 3),
                if (showWarranty)
                  cell(
                    _NumberBox(controller: line.warranty, onChanged: changed),
                    flex: 2,
                  ),
                cell(deleteButton(line), flex: 1, align: Alignment.center),
              ],
            ),
          ),
      ],
    );

    // Điện thoại: mỗi dòng hàng là một thẻ, nhập liệu không phải cuộn ngang.
    Widget lineCard(ReceiptLine line) => Container(
      margin: const EdgeInsets.only(bottom: AppSpace.s8),
      padding: const EdgeInsets.all(AppSpace.s16),
      decoration: BoxDecoration(
        border: Border.all(color: c.divider),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${Fmt.productCode(line.productId)} · ${line.product['productName']}',
                      style: context.text.label,
                    ),
                    Text(
                      'ĐVT: ${line.product['unit'] ?? '—'}',
                      style: context.text.caption.copyWith(color: c.muted),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: AppSpace.s8),
              deleteButton(line),
            ],
          ),
          const SizedBox(height: AppSpace.s8),
          Row(
            children: [
              Expanded(
                child: _MiniField(
                  label: 'Số lượng',
                  child: _NumberBox(controller: line.qty, onChanged: changed),
                ),
              ),
              if (showPrice) ...[
                const SizedBox(width: AppSpace.s8),
                Expanded(
                  flex: 2,
                  child: _MiniField(
                    label: priceLabel,
                    child: _NumberBox(
                      controller: line.price,
                      onChanged: changed,
                      suffix: '₫',
                    ),
                  ),
                ),
              ],
            ],
          ),
          if (showSerial || showWarranty) ...[
            const SizedBox(height: AppSpace.s8),
            Row(
              children: [
                if (showSerial)
                  Expanded(
                    flex: 2,
                    child: _MiniField(
                      label: 'Serial',
                      child: serialField(line),
                    ),
                  ),
                if (showSerial && showWarranty)
                  const SizedBox(width: AppSpace.s8),
                if (showWarranty)
                  Expanded(
                    child: _MiniField(
                      label: 'BH (tháng)',
                      child: _NumberBox(
                        controller: line.warranty,
                        onChanged: changed,
                      ),
                    ),
                  ),
              ],
            ),
          ],
          if (showPrice) ...[
            const SizedBox(height: AppSpace.s8),
            Row(
              children: [
                Expanded(
                  child: Text(
                    'Thành tiền',
                    style: cellText.copyWith(color: c.muted),
                  ),
                ),
                Text(
                  Fmt.money(line.lineTotal),
                  style: context.text.label.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );

    return Column(
      children: [
        LayoutBuilder(
          builder: (context, constraints) {
            if (constraints.maxWidth < 600) {
              return Column(children: [for (final l in lines) lineCard(l)]);
            }
            return constraints.maxWidth < 900
                ? SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: SizedBox(width: 900, child: table),
                  )
                : table;
          },
        ),
        const SizedBox(height: AppSpace.s16),
        _ProductSearch(
          products: products,
          onSelected: (p) {
            final existing = lines.where(
              (l) =>
                  l.productId == (p['id'] as num).toInt() &&
                  l.serialValue == null,
            );
            if (existing.isNotEmpty && p['isSerialized'] != true) {
              final line = existing.first;
              line.qty.text = '${line.quantity + 1}';
            } else {
              lines.add(ReceiptLine(product: p, price: defaultPrice(p)));
            }
            changed();
          },
        ),
      ],
    );
  }
}

/// Nhãn nhỏ + ô nhập, dùng trong thẻ dòng hàng trên điện thoại.
class _MiniField extends StatelessWidget {
  const _MiniField({required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    mainAxisSize: MainAxisSize.min,
    children: [
      Text(
        label,
        style: context.text.caption.copyWith(color: context.colors.muted),
      ),
      const SizedBox(height: AppSpace.s4),
      child,
    ],
  );
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
    style: context.text.body2,
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
            padding: const EdgeInsets.symmetric(vertical: AppSpace.s8),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    label,
                    style: context.text.body1.copyWith(color: c.muted),
                  ),
                ),
                Text(
                  value,
                  // Tổng cộng: Heading 2 đậm, màu chữ cam đủ tương phản.
                  style: emphasize
                      ? context.text.h2.copyWith(
                          fontWeight: FontWeight.w800,
                          color: c.accentText,
                        )
                      : context.text.label,
                ),
              ],
            ),
          ),
      ],
    );
  }
}
