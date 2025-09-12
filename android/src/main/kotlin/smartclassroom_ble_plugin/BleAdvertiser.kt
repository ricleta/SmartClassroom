package smartclassroom_ble_plugin

import android.annotation.SuppressLint
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
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanSettings as RxScanSettings
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry
import io.reactivex.disposables.Disposable
import java.util.UUID

class BleAdvertiser(private val context: Context, private val channel: MethodChannel) :
    MethodCallHandler, PluginRegistry.RequestPermissionsResultListener {

    private val TAG = "BleAdvertiser"
    private var activity: Activity? = null

    // RxAndroidBle for scanning
    private lateinit var rxBleClient: RxBleClient
    private var scanSubscription: Disposable? = null

    // Native Android BLE for advertising
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: android.bluetooth.le.BluetoothLeAdvertiser? = null

    private var isAdvertising: Boolean = false
    private var isScanning: Boolean = false
    private var currentStudentId: String? = null // Store studentId for permission callback

    private val ADVERTISE_REQUEST_CODE = 1001
    private val YOUR_APP_SERVICE_UUID = UUID.fromString("02110526-1234-5678-1234-56789ABCDEF0")

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
        }
    }

    init {
        // Initialize RxBleClient for scanning
        rxBleClient = RxBleClient.create(context)

        // Initialize Bluetooth components for advertising
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
            "startListening" -> {
                startListening(result)
            }
            "stopListening" -> {
                stopListening()
                result.success(null)
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
                currentStudentId?.let {
                    startAdvertising(it, null, true)
                } ?: Log.e(TAG, "No student ID to retry advertising after permissions.")
            } else {
                Log.e(TAG, "BLE advertising permissions denied.")
                channel.invokeMethod("onAdvertisingStateChanged", false)
            }
            currentStudentId = null
            return true
        }
        return false
    }

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
        if (isAdvertising && !isRetry) {
            result?.success(null)
            return
        }

        if (!checkPermissionsAndStartAdvertising(studentId, result)) {
            return
        }

        startBleAdvertising(studentId, result)
    }

    @SuppressLint("CheckResult")
    private fun startListening(result: MethodChannel.Result?) {
        Log.d(TAG, "startListening called")

        val scanSettings = RxScanSettings.Builder()
            .setScanMode(RxScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanSubscription = rxBleClient.scanBleDevices(scanSettings)
            .subscribe(
                { scanResult ->
                    Log.d(TAG, "Device found: ${scanResult.bleDevice.macAddress}, Name: ${scanResult.bleDevice.name}")
                    channel.invokeMethod("onStudentIdReceived", scanResult.bleDevice.name)
                },
                { throwable ->
                    Log.e(TAG, "BLE scan failed: $throwable")
                }
            )
        isScanning = true
        result?.success(null)
    }

    private fun stopListening() {
        Log.d(TAG, "stopListening called")
        scanSubscription?.dispose()
        isScanning = false
        Log.d(TAG, "BLE scanning stopped")
    }

    private fun checkPermissionsAndStartAdvertising(studentId: String, result: MethodChannel.Result?): Boolean {
        if (activity == null) {
            result?.error("NO_ACTIVITY", "Activity is not attached. Cannot request permissions.", null)
            return false
        }

        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            @Suppress("Deprecation")
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH_ADMIN)
            }
            @Suppress("Deprecation")
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.BLUETOOTH)
            }
        }

        if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            activity?.requestPermissions(permissionsNeeded.toTypedArray(), ADVERTISE_REQUEST_CODE)
            result?.error("PERMISSIONS_REQUIRED", "BLE advertising permissions are required.", null)
            return false
        }
        return true
    }

    private fun startBleAdvertising(studentId: String, result: MethodChannel.Result?) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val parcelUuid = ParcelUuid(YOUR_APP_SERVICE_UUID)
        val studentIdBytes = studentId.toByteArray(Charsets.UTF_8)

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(parcelUuid, studentIdBytes)
            .build()

        if (isAdvertising) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        }

        bluetoothLeAdvertiser?.startAdvertising(settings, advertiseData, advertiseCallback)
        result?.success(null)
    }

    fun stopAdvertising() {
        if (bluetoothLeAdvertiser != null && isAdvertising) {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            Log.d(TAG, "BLE advertising stopped")
            isAdvertising = false
            channel.invokeMethod("onAdvertisingStateChanged", false)
        } else {
            Log.d(TAG, "BLE advertising not active or advertiser unavailable.")
        }
    }

    fun isAdvertising(): Boolean {
        Log.d(TAG, "isAdvertising called: $isAdvertising")
        return isAdvertising
    }
}
