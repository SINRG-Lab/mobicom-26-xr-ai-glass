package com.sdk.glassessdksample.ui
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.communication.CommandHandle
import com.oudmon.ble.base.communication.req.SimpleKeyReq
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import com.sdk.glassessdksample.MainActivity
import com.sdk.glassessdksample.R
import com.sdk.glassessdksample.databinding.ActivityDeviceBindBinding
import com.xiasuhuei321.loadingdialog.view.LoadingDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import androidx.core.content.edit

class DeviceBindActivity : BaseActivity() {
    private lateinit var binding: ActivityDeviceBindBinding
    private lateinit var  adapter: DeviceListAdapter
    private var scanSize:Int=0
    private val runnable=MyRunnable()

    private lateinit var loadingDialog: LoadingDialog
    private val myHandler : Handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    val deviceList = mutableListOf<SmartWatch>()
    val bleScanCallback: BleCallback = BleCallback()

    lateinit var deviceName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDeviceBindBinding.inflate(layoutInflater)
        EventBus.getDefault().register(this)
        setContentView(binding.root)

        requestPermissionsAndStart()

        /*
        if (XXPermissions.isGranted(
                this,
                com.hjq.permissions.Permission.BLUETOOTH_SCAN,
                com.hjq.permissions.Permission.BLUETOOTH_CONNECT
            )
        ) {
            binding.startScan.performClick()
        } else {
            // Request again if missing
            XXPermissions.with(this)
                .permission(
                    com.hjq.permissions.Permission.BLUETOOTH_SCAN,
                    com.hjq.permissions.Permission.BLUETOOTH_CONNECT
                )
                .request { permissions, all -> if (all) binding.startScan.performClick() }
        }
        */
    }

    override fun onResume() {
        super.onResume()
        // requestLocationPermission(this, PermissionCallback())
        /*
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Pre-Android 12 needs location for BLE
            if (!XXPermissions.isGranted(this, com.hjq.permissions.Permission.ACCESS_FINE_LOCATION)) {
                requestLocationPermission(this, PermissionCallback())
            } else {
                binding.startScan.performClick()
            }
        } else {
            // Android 12+ (already granted BLUETOOTH_SCAN in MainActivity)
            binding.startScan.performClick()
        }
        */
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(this.packageName)
        }
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(messageEvent: BluetoothEvent) {
        Log.i(TAG, "onMessageEvent: "+messageEvent.connect)
        if(messageEvent.connect){
            loadingDialog.close()
            finish()
        }
    }

    override fun setupViews() {
        super.setupViews()
        adapter = DeviceListAdapter(this, deviceList)
        binding.run {
            deviceRcv.layoutManager = LinearLayoutManager(this@DeviceBindActivity)
            deviceRcv.adapter = adapter
            titleBar.tvTitle.text="Scan"
            titleBar.ivNavigateBefore.setOnClickListener {
                finish()
            }
        }

        adapter.notifyDataSetChanged()

        adapter.run {
            setOnItemClickListener{ _, _, position->
                myHandler.removeCallbacks(runnable)
                val smartWatch:SmartWatch= deviceList[position]
                BleOperateManager.getInstance().connectDirectly(smartWatch.deviceAddress)
                deviceName = smartWatch.deviceName
                Log.d(TAG, "Connected device name: $deviceName")

                DeviceManager.getDeviceName()?.deviceName = deviceName

                Log.d(TAG, "Device name: ${DeviceManager.getDeviceName()?.deviceName}")

                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit {
                    putString("last_device_name", deviceName)
                }

                Log.d(TAG, "Saved device name to prefs: $deviceName")

                finish()

                /*
                val resultIntent = Intent().apply {
                    putExtra("device_name", smartWatch.deviceName)
                    putExtra("device_address", smartWatch.deviceAddress)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()

                loadingDialog =LoadingDialog(this@DeviceBindActivity)
                loadingDialog.setLoadingText(getString(R.string.text_22)).show()
                 */
            }
        }

        setOnClickListener(binding.startScan) {
            deviceList.clear()
            adapter.notifyDataSetChanged()
            BleScannerHelper.getInstance().reSetCallback()
            if(!BluetoothUtils.isEnabledBluetooth(this@DeviceBindActivity)){
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                @Suppress("MissingPermission")
                activity!!.startActivityForResult(intent, 300)
            } else {
                scanSize = 0
                BleScannerHelper.getInstance()
                    .scanDevice(this@DeviceBindActivity, null, bleScanCallback)
                myHandler.removeCallbacks(runnable)
                myHandler.postDelayed(runnable, 15 * 1000)
            }
        }
    }

    inner class MyRunnable:Runnable{
        override fun run() {
            BleScannerHelper.getInstance().stopScan(this@DeviceBindActivity)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    inner class PermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if(never){
                XXPermissions.startPermissionActivity(this@DeviceBindActivity, permissions);
            }
        }

    }


    inner class BleCallback : ScanWrapperCallback {
        override fun onStart() {
        }

        override fun onStop() {

        }

        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            @Suppress("MissingPermission")
            if (device != null && (!device.name.isNullOrEmpty()) && device.name.startsWith("G")) {
//                if (device.name.startsWith("O_")||device.name.startsWith("Q_")) {
//
//                }

                val smartWatch = SmartWatch(device.name, device.address, rssi)
                Log.i("1111",device.name+"---"+ device.address)

                if (!deviceList.contains(smartWatch)) {
                    scanSize++
                    deviceList.add(0, smartWatch)
                    deviceList.sortByDescending { it -> it.rssi }
                    adapter.notifyDataSetChanged()
                    if (scanSize > 30) {
                        BleScannerHelper.getInstance().stopScan(this@DeviceBindActivity)
                    }
                }

                /*
                if (device.name == "G300_1C2B") {
                    myHandler.removeCallbacks(runnable) // Stop scan timeout
                    BleScannerHelper.getInstance().stopScan(this@DeviceBindActivity) // Stop scanning
                    connectToDevice(smartWatch)
                }
                */
            }
        }

        override fun onScanFailed(errorCode: Int) {

        }

        override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {

        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {

        }

    }

    private fun requestPermissionsAndStart() {
        val perms = listOf(
            com.hjq.permissions.Permission.BLUETOOTH_SCAN,
            com.hjq.permissions.Permission.BLUETOOTH_CONNECT,
            com.hjq.permissions.Permission.ACCESS_FINE_LOCATION
        )
        XXPermissions.with(this)
            .permission(perms)
            .request { permissions, all ->
                if (all && BluetoothUtils.isEnabledBluetooth(this)) {
                    binding.startScan.performClick()
                } else {
                    Log.w("BLE", "Missing permissions or Bluetooth disabled")
                }
            }
    }

    private fun connectToDevice(smartWatch: SmartWatch) {
        BleOperateManager.getInstance().connectDirectly(smartWatch.deviceAddress)
    }
}