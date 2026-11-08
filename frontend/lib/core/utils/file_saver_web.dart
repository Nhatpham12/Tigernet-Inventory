import 'dart:js_interop';

import 'package:web/web.dart' as web;

import '../network/api_client.dart';

String _blobUrl(DownloadedFile file) {
  final blob = web.Blob(
    [file.bytes.toJS].toJS,
    web.BlobPropertyBag(type: file.contentType),
  );
  return web.URL.createObjectURL(blob);
}

Future<String?> saveFile(DownloadedFile file) async {
  final url = _blobUrl(file);
  final a = web.HTMLAnchorElement()
    ..href = url
    ..download = file.name;
  web.document.body?.append(a);
  a.click();
  a.remove();
  Future<void>.delayed(
    const Duration(seconds: 30),
    () => web.URL.revokeObjectURL(url),
  );
  return null;
}

Future<String?> openFile(DownloadedFile file) async {
  final url = _blobUrl(file);
  web.window.open(url, '_blank');
  Future<void>.delayed(
    const Duration(minutes: 5),
    () => web.URL.revokeObjectURL(url),
  );
  return null;
}
