import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:smartclassroom_ble_plugin/smartclassroom_ble_plugin_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelSmartclassroomBlePlugin platform = MethodChannelSmartclassroomBlePlugin();
  const MethodChannel channel = MethodChannel('smartclassroom_ble_plugin');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
