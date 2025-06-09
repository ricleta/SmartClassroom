package smartclassroom_ble_plugin

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodChannel

/** SmartclassroomBlePlugin */
class SmartclassroomBlePlugin: FlutterPlugin, ActivityAware {
    private lateinit var channel : MethodChannel
    private var bleAdvertiser: BleAdvertiser? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "smartclassroom_ble_plugin")
        bleAdvertiser = BleAdvertiser(flutterPluginBinding.applicationContext, channel)
        channel.setMethodCallHandler(bleAdvertiser)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        bleAdvertiser?.stopAdvertising()
        bleAdvertiser = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        bleAdvertiser?.setActivity(binding.activity)
        binding.addRequestPermissionsResultListener(bleAdvertiser!!)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        bleAdvertiser?.setActivity(null)
    }
}