import 'package:flutter_test/flutter_test.dart';
import 'package:smartclassroom_ble_plugin/smartclassroom_ble_plugin.dart';
import 'package:smartclassroom_ble_plugin/smartclassroom_ble_plugin_platform_interface.dart';
import 'package:smartclassroom_ble_plugin/smartclassroom_ble_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSmartclassroomBlePluginPlatform
    with MockPlatformInterfaceMixin
    implements SmartclassroomBlePluginPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<void> startAdvertising(String studentId) async {
    // Simulate advertising logic here, if needed.
  }

  @override
  Future<void> stopAdvertising() async {
    // Simulate stopping advertising logic here, if needed.
  }
}

void main() {
  final SmartclassroomBlePluginPlatform initialPlatform = SmartclassroomBlePluginPlatform.instance;

  test('$MethodChannelSmartclassroomBlePlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSmartclassroomBlePlugin>());
  });

  test('getPlatformVersion', () async {
    SmartclassroomBlePlugin smartclassroomBlePlugin = SmartclassroomBlePlugin();
    MockSmartclassroomBlePluginPlatform fakePlatform = MockSmartclassroomBlePluginPlatform();
    SmartclassroomBlePluginPlatform.instance = fakePlatform;

    expect(await smartclassroomBlePlugin.getPlatformVersion(), '42');
  });

  test('startAdvertising', () async {
    SmartclassroomBlePlugin smartclassroomBlePlugin = SmartclassroomBlePlugin();
    MockSmartclassroomBlePluginPlatform fakePlatform = MockSmartclassroomBlePluginPlatform();
    SmartclassroomBlePluginPlatform.instance = fakePlatform;

    await smartclassroomBlePlugin.startAdvertising("student123");
    // Assuming startAdvertising does not return anything, we just check if it completes without error.
  });
}
