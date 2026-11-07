import 'dart:io';

import '../network/api_client.dart';

Directory _targetDir() {
  final home =
      Platform.environment['USERPROFILE'] ?? Platform.environment['HOME'];
  if (home != null) {
    final downloads = Directory('$home${Platform.pathSeparator}Downloads');
    if (downloads.existsSync()) return downloads;
  }
  return Directory.systemTemp;
}

Future<String?> saveFile(DownloadedFile file) async {
  final out = File('${_targetDir().path}${Platform.pathSeparator}${file.name}');
  await out.writeAsBytes(file.bytes, flush: true);
  return out.path;
}

Future<String?> openFile(DownloadedFile file) => saveFile(file);
