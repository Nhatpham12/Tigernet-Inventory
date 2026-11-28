import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tigernet_app/core/theme/app_theme.dart';
import 'package:tigernet_app/features/common/lookups.dart';
import 'package:tigernet_app/shared/data_table/app_data_table.dart';

const _topBar = ValueKey('app-data-table-top-scrollbar');

Widget _table({required double width, int columns = 12}) => ProviderScope(
  child: MaterialApp(
    theme: AppTheme.light(),
    home: Scaffold(
      body: Align(
        alignment: Alignment.topLeft,
        child: SizedBox(
          width: width,
          child: AppDataTable(
            columns: [
              for (var i = 0; i < columns; i++)
                TableColumnDef.text(
                  'Cột $i',
                  (r) => '${r['v']}',
                  minWidth: 150,
                ),
            ],
            rows: const [
              {'v': 'giá trị'},
            ],
          ),
        ),
      ),
    ),
  ),
);

/// Bộ điều khiển cuộn ngang của một Scrollbar (thanh đầu hoặc đáy bảng).
ScrollController _controllerOf(WidgetTester tester, Finder scrollbar) =>
    tester.widget<Scrollbar>(scrollbar).controller!;

void main() {
  group('vatDropdownItems', () {
    const options = [
      {'id': 1, 'vat': 'NO', 'rate': null},
      {'id': 2, 'vat': '0%', 'rate': 0},
      {'id': 5, 'vat': '10%', 'rate': 10},
    ];

    test('chỉ một mục "Không chịu thuế", ẩn mục NO trùng nghĩa', () {
      final items = vatDropdownItems(options, null);
      expect(items.first, (null, 'Không chịu thuế'));
      expect(items.map((e) => e.$2), ['Không chịu thuế', '0%', '10%']);
    });

    test(
      'phiếu cũ đang chọn mục NO thì vẫn giữ mục đó để hiện đúng giá trị',
      () {
        final items = vatDropdownItems(options, 1);
        expect(items.map((e) => e.$1), [null, 1, 2, 5]);
      },
    );
  });

  group('AppDataTable thanh cuộn ngang ở đầu bảng', () {
    testWidgets(
      'bảng rộng hơn khung: có thanh đầu bảng, kéo thanh đầu thì bảng cuộn theo',
      (tester) async {
        await tester.pumpWidget(_table(width: 600));
        await tester.pumpAndSettle();

        final top = find.byKey(_topBar);
        expect(top, findsOneWidget);

        final topController = _controllerOf(tester, top);
        final bottomController = _controllerOf(
          tester,
          find.byWidgetPredicate((w) => w is Scrollbar && w.key != _topBar),
        );
        expect(bottomController.position.maxScrollExtent, greaterThan(0));
        expect(
          topController.position.maxScrollExtent,
          closeTo(bottomController.position.maxScrollExtent, 1),
        );

        topController.jumpTo(200);
        await tester.pump();
        expect(bottomController.offset, 200);

        bottomController.jumpTo(50);
        await tester.pump();
        expect(topController.offset, 50);
      },
    );

    testWidgets('bảng vừa khung: không hiện thanh đầu bảng', (tester) async {
      await tester.pumpWidget(_table(width: 1200, columns: 3));
      await tester.pumpAndSettle();
      expect(find.byKey(_topBar), findsNothing);
    });
  });
}
