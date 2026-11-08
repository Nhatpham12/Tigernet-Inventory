import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/feedback/toast.dart';
import '../../shared/form/fields.dart';
import '../../shared/widgets/page_header.dart';
import '../../shared/widgets/section_card.dart';
import '../common/lookups.dart';

/// Figma 1366:1796 "Sản phẩm / Thêm sản phẩm". [productId] != null thì sửa.
/// [inDialog]: mở trong hộp thoại (vd. từ phiếu nhập), lưu xong trả sản phẩm vừa tạo qua Navigator.pop.
class ProductFormPage extends ConsumerStatefulWidget {
  const ProductFormPage({super.key, this.productId, this.inDialog = false});

  final int? productId;
  final bool inDialog;

  @override
  ConsumerState<ProductFormPage> createState() => _ProductFormPageState();
}

class _ProductFormPageState extends ConsumerState<ProductFormPage> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _unit = TextEditingController(text: 'Cái');
  final _priceIn = TextEditingController();
  final _priceOut = TextEditingController();
  final _minStock = TextEditingController(text: '0');
  int? _groupId;
  bool _serialized = false;
  bool _saving = false;
  late Future<void> _loading;

  bool get _isEdit => widget.productId != null;

  @override
  void initState() {
    super.initState();
    _loading = _isEdit ? _load() : Future.value();
    for (final c in [_priceIn, _priceOut]) {
      c.addListener(() => setState(() {}));
    }
  }

  Future<void> _load() async {
    final p =
        await ref
                .read(apiClientProvider)
                .get('/bff/api/products/${widget.productId}')
            as Map;
    _name.text = '${p['productName'] ?? ''}';
    _unit.text = '${p['unit'] ?? ''}';
    _priceIn.text = '${(p['defaultPriceIn'] as num?)?.toInt() ?? ''}';
    _priceOut.text = '${(p['defaultPriceOut'] as num?)?.toInt() ?? ''}';
    _minStock.text = '${p['minStockLevel'] ?? 0}';
    _groupId = (p['groupId'] as num?)?.toInt();
    _serialized = p['isSerialized'] == true;
  }

  @override
  void dispose() {
    for (final c in [_name, _unit, _priceIn, _priceOut, _minStock]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _saving = true);
    final body = {
      'productName': _name.text.trim(),
      'groupId': _groupId,
      'unit': _unit.text.trim(),
      'defaultPriceIn': parseNumber(_priceIn.text) ?? 0,
      'defaultPriceOut': parseNumber(_priceOut.text) ?? 0,
      'isSerialized': _serialized,
      'minStockLevel': parseNumber(_minStock.text)?.toInt() ?? 0,
    };
    try {
      final api = ref.read(apiClientProvider);
      final saved = _isEdit
          ? await api.put('/bff/api/products/${widget.productId}', data: body)
          : await api.post('/bff/api/products', data: body);
      if (!mounted) return;
      Toast.success(
        context,
        _isEdit ? 'Đã cập nhật sản phẩm.' : 'Đã thêm sản phẩm.',
      );
      ref.invalidate(productsLookupProvider);
      _close(saved is Map ? Map<String, dynamic>.from(saved) : null);
    } catch (e) {
      if (mounted) Toast.error(context, e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  /// Trong hộp thoại: đóng và trả [saved] (null = hủy). Ngoài trang: về danh sách sản phẩm.
  void _close([Map<String, dynamic>? saved]) => widget.inDialog
      ? Navigator.pop(context, saved)
      : context.go('/app/products');

  @override
  Widget build(BuildContext context) {
    final c = context.colors;
    final groups = ref.watch(productGroupsLookupProvider);
    final priceIn = parseNumber(_priceIn.text);
    final priceOut = parseNumber(_priceOut.text);
    final margin = priceIn != null && priceOut != null && priceOut > 0
        ? ((priceOut - priceIn) / priceOut * 100).toStringAsFixed(1)
        : null;
    return FutureBuilder<void>(
      future: _loading,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const LoadingView();
        }
        if (snapshot.hasError) return StateView.error(snapshot.error!);
        return Form(
          key: _form,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (!widget.inDialog)
                PageHeader(
                  title: _isEdit ? 'Sửa sản phẩm' : 'Thêm sản phẩm',
                  crumbs: [
                    const Crumb('Trang chủ', '/app/dashboard'),
                    const Crumb('Sản phẩm', '/app/products'),
                    Crumb(_isEdit ? 'Sửa sản phẩm' : 'Thêm sản phẩm'),
                  ],
                ),
              SectionCard(
                title: 'Thông tin cơ bản',
                child: FormGrid(
                  children: [
                    AppTextField(
                      label: 'Mã SP / SKU',
                      readOnly: true,
                      controller: TextEditingController(
                        text: _isEdit
                            ? Fmt.productCode(widget.productId)
                            : 'Tự sinh khi lưu',
                      ),
                    ),
                    AppTextField(
                      label: 'Tên sản phẩm',
                      required: true,
                      controller: _name,
                      hint: 'Máy khoan Bosch GSB 13 RE',
                    ),
                    groups.when(
                      loading: () => const LinearProgressIndicator(),
                      error: (e, _) => Text('$e'),
                      data: (list) => AppDropdown<int>(
                        label: 'Danh mục',
                        required: true,
                        value: _groupId,
                        items: [
                          for (final g in list)
                            ((g['id'] as num).toInt(), '${g['groupName']}'),
                        ],
                        onChanged: (v) => setState(() => _groupId = v),
                      ),
                    ),
                    AppTextField(
                      label: 'Đơn vị tính',
                      required: true,
                      controller: _unit,
                    ),
                    AppDropdown<bool>(
                      label: 'Theo dõi',
                      value: _serialized,
                      items: const [
                        (false, 'Số lượng'),
                        (true, 'Serial & bảo hành'),
                      ],
                      onChanged: (v) =>
                          setState(() => _serialized = v ?? false),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              SectionCard(
                title: 'Giá & theo dõi hàng hóa',
                trailing: margin == null
                    ? null
                    : Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: c.successBg,
                          border: Border.all(
                            color: c.success.withValues(alpha: 0.4),
                          ),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          'Biên lợi nhuận: $margin%  ·  Lãi gộp: ${Fmt.money(priceOut! - priceIn!)}/SP',
                          style: TextStyle(fontSize: 12, color: c.success),
                        ),
                      ),
                child: FormGrid(
                  children: [
                    AppTextField(
                      label: 'Giá nhập (₫)',
                      required: true,
                      controller: _priceIn,
                      keyboardType: TextInputType.number,
                      inputFormatters: digitsOnly,
                    ),
                    AppTextField(
                      label: 'Giá bán (₫)',
                      required: true,
                      controller: _priceOut,
                      keyboardType: TextInputType.number,
                      inputFormatters: digitsOnly,
                    ),
                    AppTextField(
                      label: 'Ngưỡng tồn tối thiểu',
                      controller: _minStock,
                      keyboardType: TextInputType.number,
                      inputFormatters: digitsOnly,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton(
                    onPressed: () => _close(),
                    child: const Text('Hủy'),
                  ),
                  const SizedBox(width: 12),
                  SizedBox(
                    width: 140,
                    child: ElevatedButton(
                      onPressed: _saving ? null : _save,
                      child: Text(_saving ? 'Đang lưu...' : 'Lưu sản phẩm'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}
