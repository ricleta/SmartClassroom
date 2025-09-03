import 'dart:async';

import 'smartclassroom_ble_plugin_platform_interface.dart';

class SmartclassroomBlePlugin {
  Future<String?> getPlatformVersion() {
    return SmartclassroomBlePluginPlatform.instance.getPlatformVersion();
  }

  Future<void> startAdvertising(String studentId) async {
    return SmartclassroomBlePluginPlatform.instance.startAdvertising(studentId);
  }

  Future<void> stopAdvertising() async {
    return SmartclassroomBlePluginPlatform.instance.stopAdvertising();
  }

  Future<void> startListening() async {
    return SmartclassroomBlePluginPlatform.instance.startListening();
  }

  Future<void> stopListening() async {
    return SmartclassroomBlePluginPlatform.instance.stopListening();
  }

  Stream<String> get messageStream =>
      SmartclassroomBlePluginPlatform.instance.onStudentIdReceived;
}