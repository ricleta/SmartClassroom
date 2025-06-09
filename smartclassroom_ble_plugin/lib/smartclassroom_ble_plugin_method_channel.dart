import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'smartclassroom_ble_plugin_platform_interface.dart';

/// An implementation of [SmartclassroomBlePluginPlatform] that uses method channels.
class MethodChannelSmartclassroomBlePlugin extends SmartclassroomBlePluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('smartclassroom_ble_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<void> startAdvertising(String studentId) async {
    await methodChannel.invokeMethod('startAdvertising', {'studentId': studentId});
  }

  @override
  Future<void> stopAdvertising() async {
    await methodChannel.invokeMethod('stopAdvertising');
  }
}
