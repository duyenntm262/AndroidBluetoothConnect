package com.example.bluetoothexample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bluetoothexample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val discoveredDevices = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private val pairedDevicesList = mutableListOf<String>()
    private lateinit var pairedDevicesAdapter: ArrayAdapter<String>
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isReceiverRegistered = false

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            startDiscovery()
        } else {
            // Permission denied, handle as appropriate for your application
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        val deviceName = it.name ?: "Unknown Device"
                        val deviceHardwareAddress = it.address // MAC address
                        val deviceEntry = "$deviceName\n$deviceHardwareAddress"
                        if (!discoveredDevices.contains(deviceEntry)) {
                            discoveredDevices.add(deviceEntry)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListView()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        } else {
            if (!bluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBtIntent)
            }
        }

        binding.scanButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermissions()
            } else {
                startDiscovery()
            }
        }

        // Khởi tạo adapter cho danh sách thiết bị đã ghép nối và gán nó cho ListView
        pairedDevicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDevicesList)
        binding.btPairLst.adapter = pairedDevicesAdapter

        // Xử lý sự kiện nhấn nút "Pair for Devices"
        binding.pairButton.setOnClickListener {
            updatePairedDevicesList()
        }
    }

    private fun setupListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDevices)
        binding.btLst.adapter = adapter
    }

    private fun updatePairedDevicesList() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            pairedDevicesList.clear()
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val deviceName = device.name ?: "Unknown Device"
                val deviceAddress = device.address // MAC address
                pairedDevicesList.add("$deviceName\n$deviceAddress")
            }
            pairedDevicesAdapter.notifyDataSetChanged()
        } else {
            // Yêu cầu quyền BLUETOOTH_CONNECT nếu chưa được cấp
        }
    }

    private fun requestBluetoothPermissions() {
        bluetoothPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    private fun startDiscovery() {
        bluetoothAdapter?.let { adapter ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(receiver, filter)
            isReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(receiver)
            isReceiverRegistered = false
        }
    }
}
