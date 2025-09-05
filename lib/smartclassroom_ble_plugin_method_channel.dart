import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'smartclassroom_ble_plugin_platform_interface.dart';

/// An implementation of [SmartclassroomBlePluginPlatform] that uses method channels.
class MethodChannelSmartclassroomBlePlugin extends SmartclassroomBlePluginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('smartclassroom_ble_plugin');

  final StreamController<String> _studentIdStreamController = StreamController.broadcast();

  MethodChannelSmartclassroomBlePlugin() {
    methodChannel.setMethodCallHandler(_handleMethodCall);
  }

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

  @override
  Future<void> startListening() async {
    await methodChannel.invokeMethod('startListening');
  }

  @override
  Future<void> stopListening() async {
    await methodChannel.invokeMethod('stopListening');
  }

  @override
  Stream<String> get onStudentIdReceived => _studentIdStreamController.stream;

  Future<void> _handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onStudentIdReceived':
        final studentId = call.arguments as String;
        // ignore: avoid_print
        print('####### Received student ID: $studentId');
        _studentIdStreamController.add(studentId);
        break;
      default:
        throw MissingPluginException();
    }
  }
}