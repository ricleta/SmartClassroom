import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'smartclassroom_ble_plugin_method_channel.dart';

abstract class SmartclassroomBlePluginPlatform extends PlatformInterface {
  /// Constructs a SmartclassroomBlePluginPlatform.
  SmartclassroomBlePluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static SmartclassroomBlePluginPlatform _instance = MethodChannelSmartclassroomBlePlugin();

  /// The default instance of [SmartclassroomBlePluginPlatform] to use.
  ///
  /// Defaults to [MethodChannelSmartclassroomBlePlugin].
  static SmartclassroomBlePluginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SmartclassroomBlePluginPlatform] when
  /// they register themselves.
  static set instance(SmartclassroomBlePluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<void> startAdvertising(String studentId) {
    throw UnimplementedError('startAdvertising() has not been implemented.');
  }

  Future<void> stopAdvertising() {
    throw UnimplementedError('stopAdvertising() has not been implemented.');
  }
}
