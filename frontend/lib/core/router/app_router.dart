import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/admin/admin_pages.dart';
import '../../features/alerts/alerts_page.dart';
import '../../features/auth/login_page.dart';
import '../../features/dashboard/dashboard_page.dart';
import '../../features/export_receipts/export_receipt_pages.dart';
import '../../features/import_receipts/import_receipt_pages.dart';
import '../../features/inventory/inventory_pages.dart';
import '../../features/partners/partner_pages.dart';
import '../../features/payments/payment_pages.dart';
import '../../features/payments/reconciliation_page.dart';
import '../../features/products/product_form_page.dart';
import '../../features/products/product_groups_page.dart';
import '../../features/products/product_list_page.dart';
import '../../features/rentals/rental_pages.dart';
import '../../features/reports/report_pages.dart';
import '../../features/warranty/warranty_detail_page.dart';
import '../../features/warranty/warranty_form_page.dart';
import '../../features/warranty/warranty_list_page.dart';
import '../../shared/feedback/state_views.dart';
import '../../shared/layout/app_shell.dart';
import '../auth/session.dart';
import '../auth/session_controller.dart';
import 'menu.dart';

/// Báo cho GoRouter chạy lại redirect khi trạng thái đăng nhập đổi.
class _SessionListenable extends ChangeNotifier {
  _SessionListenable(Ref ref) {
    ref.listen(sessionProvider, (_, _) => notifyListeners());
  }
}

/// Đường dẫn → nhóm vai trò được vào (lấy từ menu), để chặn cả khi gõ URL trực tiếp (PUB-01 AC2).
bool _allowed(String location, Session session) {
  for (final group in [...kMainMenu, ...kAdminMenu]) {
    if (group.path != null &&
        location.startsWith(group.path!) &&
        !canSee(group.roles, session.role)) {
      return false;
    }
    for (final link in group.children) {
      if (location == link.path) {
        return canSee(group.roles, session.role) &&
            canSee(link.roles, session.role);
      }
    }
  }
  return true;
}

final routerProvider = Provider<GoRouter>((ref) {
  final listenable = _SessionListenable(ref);
  ref.onDispose(listenable.dispose);

  GoRoute page(String path, Widget Function(GoRouterState s) build) => GoRoute(
    path: path,
    pageBuilder: (context, state) => NoTransitionPage(
      key: state.pageKey,
      child: PageBody(child: build(state)),
    ),
  );

  return GoRouter(
    initialLocation: '/app/dashboard',
    refreshListenable: listenable,
    redirect: (context, state) {
      final session = ref.read(sessionProvider);
      if (session.isLoading && !session.hasValue) return null;
      final loggedIn = session.value != null;
      final atLogin = state.matchedLocation == '/login';
      if (!loggedIn) return atLogin ? null : '/login';
      if (atLogin || state.matchedLocation == '/') return '/app/dashboard';
      if (!_allowed(state.matchedLocation, session.value!)) return '/app/403';
      return null;
    },
    errorBuilder: (context, state) => const NotFoundPage(),
    routes: [
      GoRoute(path: '/', redirect: (_, _) => '/app/dashboard'),
      GoRoute(path: '/login', builder: (_, _) => const LoginPage()),
      ShellRoute(
        builder: (context, state, child) =>
            AppShell(location: state.matchedLocation, child: child),
        routes: [
          page('/app/dashboard', (_) => const DashboardPage()),
          page('/app/alerts', (_) => const AlertsPage()),
          page(
            '/app/products',
            (s) => ProductListPage(
              key: ValueKey(s.uri.query),
              initialStockStatus: s.uri.queryParameters['stockStatus'],
            ),
          ),
          page('/app/products/new', (_) => const ProductFormPage()),
          page(
            '/app/products/:id/edit',
            (s) => ProductFormPage(
              productId: int.tryParse(s.pathParameters['id']!),
            ),
          ),
          page('/app/product-groups', (_) => const ProductGroupsPage()),
          page('/app/suppliers', (_) => const SupplierListPage()),
          page(
            '/app/suppliers/new',
            (_) => const PartnerForm(kind: PartnerKind.supplier),
          ),
          page('/app/customers', (_) => const CustomerListPage()),
          page('/app/import-receipts', (_) => const ImportReceiptListPage()),
          page(
            '/app/import-receipts/new',
            (_) => const ImportReceiptFormPage(),
          ),
          page('/app/export-receipts', (_) => const ExportReceiptListPage()),
          page(
            '/app/export-receipts/new',
            (_) => const ExportReceiptFormPage(),
          ),
          page('/app/sales-returns', (_) => const SalesReturnListPage()),
          page(
            '/app/rentals',
            (s) => RentalListPage(
              kind: RentalKind.rent,
              initialState: s.uri.queryParameters['state'],
            ),
          ),
          page(
            '/app/rentals/new',
            (_) => const RentalFormPage(kind: RentalKind.rent),
          ),
          page(
            '/app/lends',
            (s) => RentalListPage(
              kind: RentalKind.lend,
              initialState: s.uri.queryParameters['state'],
            ),
          ),
          page(
            '/app/lends/new',
            (_) => const RentalFormPage(kind: RentalKind.lend),
          ),
          page('/app/transfers', (_) => const TransferListPage()),
          page('/app/warehouses', (_) => const WarehouseListPage()),
          page('/app/stocktakes', (_) => const StocktakeListPage()),
          page('/app/reports/stock', (_) => const StockReportPage()),
          page('/app/reports/movements', (_) => const MovementReportPage()),
          page('/app/reports/receivables', (_) => const ReceivablesPage()),
          page(
            '/app/reports/rentals',
            (_) => const RentalListPage(kind: null, asReport: true),
          ),
          // Bảo hành
          page('/app/warranty-claims', (_) => const WarrantyClaimListPage()),
          page(
            '/app/warranty-claims/new',
            (_) => const WarrantyClaimFormPage(),
          ),
          page(
            '/app/warranty-claims/:id',
            (s) => WarrantyClaimDetailPage(
              key: ValueKey(s.pathParameters['id']),
              claimId: int.parse(s.pathParameters['id']!),
            ),
          ),
          // hết Bảo hành
          // Thu chi + đối chiếu công nợ
          page(
            '/app/payments',
            (s) => PaymentListPage(
              initialDirection: s.uri.queryParameters['direction'],
            ),
          ),
          page(
            '/app/payments/new',
            (s) => PaymentFormPage(
              key: ValueKey(s.uri.query),
              direction: s.uri.queryParameters['direction'] == 'OUT'
                  ? 'OUT'
                  : 'IN',
            ),
          ),
          page(
            '/app/reports/reconciliation',
            (_) => const ReconciliationPage(),
          ),
          page('/app/users', (_) => const UserListPage()),
          page('/app/settings', (_) => const SettingsPage()),
          page('/app/403', (_) => const ForbiddenPage()),
        ],
      ),
    ],
  );
});
