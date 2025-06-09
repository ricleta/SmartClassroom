package smartclassroom_ble_plugin

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry
import java.util.UUID

class BleAdvertiser(private val context: Context, private val channel: MethodChannel) :
    MethodCallHandler, PluginRegistry.RequestPermissionsResultListener {

    private val TAG = "BleAdvertiser"
    private var activity: Activity? = null
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: android.bluetooth.le.BluetoothLeAdvertiser? = null
    private var isAdvertising: Boolean = false
    private var currentStudentId: String? = null // Store studentId for permission callback

    private val ADVERTISE_REQUEST_CODE = 1001

    // Callback for advertising status
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "BLE advertising started successfully")
            isAdvertising = true
            channel.invokeMethod("onAdvertisingStateChanged", true) // Notify Flutter
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "BLE advertising failed: $errorCode")
            isAdvertising = false
            channel.invokeMethod("onAdvertisingStateChanged", false) // Notify Flutter
            // TODO Specific error handling based on errorCode can be added here
        }
    }

    init {
        // Initialize Bluetooth components
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    }

    // Method to set the activity, called from SmartclassroomBlePlugin
    fun setActivity(activity: Activity?) {
        this.activity = activity
    }

    // Handles method calls from Flutter
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startAdvertising" -> {
                val studentId = call.argument<String>("studentId")
                if (studentId != null) {
                    currentStudentId = studentId // Store the ID temporarily
                    startAdvertising(studentId, result)
                } else {
                    result.error("INVALID_ARGUMENT", "Student ID cannot be null", null)
                }
            }
            "stopAdvertising" -> {
                stopAdvertising()
                result.success(null)
            }
            "isAdvertising" -> {
                result.success(isAdvertising())
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    // Handles permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == ADVERTISE_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "Permissions granted. Retrying advertising.")
                // If permissions are granted, try to start advertising with the stored studentId
                // Pass null for the result object as this is a retry, not an original method call
                currentStudentId?.let {
                    startAdvertising(it, null, true) // Pass null here for result
                } ?: Log.e(TAG, "No student ID to retry advertising after permissions.")
            } else {
                Log.e(TAG, "BLE advertising permissions denied.")
                channel.invokeMethod("onAdvertisingStateChanged", false) // Notify Flutter about failure
            }
            currentStudentId = null // Clear the stored ID
            return true
        }
        return false
    }

    // Main method to initiate advertising, includes permission checks
    private fun startAdvertising(studentId: String, result: MethodChannel.Result?, isRetry: Boolean = false) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            result?.error("BLUETOOTH_NOT_AVAILABLE", "Bluetooth is not available or not enabled.", null)
            return
        }
        if (bluetoothLeAdvertiser == null) {
            result?.error("BLE_ADVERTISER_UNAVAILABLE", "BLE Advertiser is not available on this device.", null)
            return
        }
        if (!bluetoothAdapter!!.isMultipleAdvertisementSupported) {
             result?.error("FEATURE_UNSUPPORTED", "BLE Advertising is not supported on this device.", null)
             return
        }
        if (isAdvertising && !isRetry) { // Don't return if it's a retry, we want to re-initiate
            result?.success(null) // Already advertising
            return
        }

        // Request permissions if not already granted
        if (!checkPermissionsAndStartAdvertising(studentId, result)) {
            // Permissions are being requested or denied, so advertising won't start immediately
            return
        }

        // If permissions are already granted, proceed with advertising
        startBleAdvertising(studentId, result)
    }

    // Helper to check and request permissions
    private fun checkPermissionsAndStartAdvertising(studentId: String, result: MethodChannel.Result?): Boolean {
        if (activity == null) {
            result?.error("NO_ACTIVITY", "Activity is not attached. Cannot request permissions.", null)
            return false
        }

        val permissionsNeeded = mutableListOf<String>()

        // Check for Android 12 (API 31) and above specific permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // For Android 11 (API 30) and below, use BLUETOOTH_ADMIN and BLUETOOTH
            @Suppress("Deprecation")
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_ADMIN)
            }
            @Suppress("Deprecation")
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH)
            }
        }

        // ACCESS_FINE_LOCATION is generally required for BLE operations
        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            activity?.requestPermissions(permissionsNeeded.toTypedArray(), ADVERTISE_REQUEST_CODE)
            result?.error("PERMISSIONS_REQUIRED", "BLE advertising permissions are required.", null)
            return false // Permissions requested, not yet granted
        }
        return true // All necessary permissions are already granted
    }

    // Actual BLE advertising start logic
    private fun startBleAdvertising(studentId: String, result: MethodChannel.Result?) {
        // Build AdvertiseSettings
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false) // Set to true if you want other devices to connect
            .setTimeout(0) // 0 means no timeout
            .build()

        // Build AdvertiseData with a custom ID (studentId)
        val YOUR_APP_SERVICE_UUID = UUID.fromString("02110526-1234-5678-1234-56789ABCDEF0")
        val parcelUuid = ParcelUuid(YOUR_APP_SERVICE_UUID)
        val studentIdBytes = studentId.toByteArray(Charsets.UTF_8) // Convert studentId string to bytes

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Whether to include the device's local name in the advertisement
            .addServiceData(parcelUuid, studentIdBytes) // Add the studentId as data associated with your service
            .build()

        // Stop any existing advertising before starting a new one
        if (isAdvertising) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, advertiseData, advertiseCallback)
        // If result is not null, it means this was an initial call from Flutter, so acknowledge it.
        // Actual advertising success/failure is reported via advertiseCallback.
        result?.success(null)
    }

    // Stops BLE advertising
    fun stopAdvertising() {
        if (bluetoothLeAdvertiser != null && isAdvertising) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            Log.d(TAG, "BLE advertising stopped")
            isAdvertising = false
            channel.invokeMethod("onAdvertisingStateChanged", false) // Notify Flutter
        } else {
            Log.d(TAG, "BLE advertising not active or advertiser unavailable.")
        }
    }

    // Checks if advertising is currently active
    fun isAdvertising(): Boolean {
        Log.d(TAG, "isAdvertising called: $isAdvertising")
        return isAdvertising
    }
}