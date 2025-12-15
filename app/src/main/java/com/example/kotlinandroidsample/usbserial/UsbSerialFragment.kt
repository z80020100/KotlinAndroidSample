package com.example.kotlinandroidsample.usbserial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.Editable
import android.text.TextWatcher
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kotlinandroidsample.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UsbSerialFragment : Fragment() {

    private lateinit var btnScanDevices: MaterialButton
    private lateinit var btnRequestDeviceAccess: MaterialButton
    private lateinit var btnConnect: MaterialButton
    private lateinit var btnDisconnect: MaterialButton
    private lateinit var btnApplyConfig: MaterialButton
    private lateinit var btnSendData: MaterialButton
    private lateinit var btnClearLog: MaterialButton
    private lateinit var switchAutoscroll: SwitchMaterial
    private lateinit var switchSkipPermissionCheck: SwitchMaterial
    private lateinit var spinnerDevices: AutoCompleteTextView
    private lateinit var tilDevices: TextInputLayout
    private lateinit var spinnerBaudRate: AutoCompleteTextView
    private lateinit var spinnerDataBits: AutoCompleteTextView
    private lateinit var spinnerStopBits: AutoCompleteTextView
    private lateinit var spinnerParity: AutoCompleteTextView
    private lateinit var etSendData: TextInputEditText
    private lateinit var cbAppendNewline: MaterialCheckBox
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvDeviceInfo: TextView
    private lateinit var tvDeviceSectionHeader: TextView
    private lateinit var tvLogDisplay: TextView
    private lateinit var scrollLog: ScrollView

    private lateinit var usbManager: UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private var serialInputOutputManager: SerialInputOutputManager? = null

    private val deviceList = mutableListOf<UsbDeviceInfo>()
    private var selectedDevice: UsbDevice? = null
    private var isConnected = false
    private var hasPermission = false
    private var autoScroll = true
    private var skipPermissionCheck = false
    private val logBuffer = StringBuilder()
    private val receiveBuffer = StringBuilder()
    private val timestampFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var textWatcher: TextWatcher? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    selectedDevice?.let { device ->
                        // Intent result may be incorrect on some devices
                        val intentResult = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        val actualPermission = usbManager.hasPermission(device)

                        appendLog(LogType.DEBUG, "Permission result - Intent: $intentResult, Actual: $actualPermission")

                        hasPermission = actualPermission

                        if (hasPermission) {
                            appendLog(LogType.INFO, "Device access permission granted")
                        } else {
                            appendLog(LogType.WARN, "Device access permission denied")
                        }
                        updateUIState()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_usb_serial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager

        // RECEIVER_EXPORTED required for system USB permission broadcasts
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(requireContext(), usbPermissionReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        initializeViews(view)
        setupClickListeners()
        setupSpinners()
        updateUIState()
        appendLog(LogType.INFO, "USB Serial Demo initialized")
    }

    private fun initializeViews(view: View) {
        btnScanDevices = view.findViewById(R.id.btn_scan_devices)
        btnRequestDeviceAccess = view.findViewById(R.id.btn_request_device_access)
        btnConnect = view.findViewById(R.id.btn_connect)
        btnDisconnect = view.findViewById(R.id.btn_disconnect)
        btnApplyConfig = view.findViewById(R.id.btn_apply_config)
        btnSendData = view.findViewById(R.id.btn_send_data)
        btnClearLog = view.findViewById(R.id.btn_clear_log)
        switchAutoscroll = view.findViewById(R.id.switch_autoscroll)
        switchSkipPermissionCheck = view.findViewById(R.id.switch_skip_permission_check)
        spinnerDevices = view.findViewById(R.id.spinner_devices)
        tilDevices = view.findViewById(R.id.til_devices)
        spinnerBaudRate = view.findViewById(R.id.spinner_baud_rate)
        spinnerDataBits = view.findViewById(R.id.spinner_data_bits)
        spinnerStopBits = view.findViewById(R.id.spinner_stop_bits)
        spinnerParity = view.findViewById(R.id.spinner_parity)
        etSendData = view.findViewById(R.id.et_send_data)
        cbAppendNewline = view.findViewById(R.id.cb_append_newline)
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status)
        tvDeviceInfo = view.findViewById(R.id.tv_device_info)
        tvDeviceSectionHeader = view.findViewById(R.id.tv_device_section_header)
        tvLogDisplay = view.findViewById(R.id.tv_log_display)
        scrollLog = view.findViewById(R.id.scroll_log)
    }

    private fun setupClickListeners() {
        btnScanDevices.setOnClickListener { scanForUsbDevices() }
        btnRequestDeviceAccess.setOnClickListener { requestDeviceAccessPermission() }
        btnConnect.setOnClickListener { connectToDevice() }
        btnDisconnect.setOnClickListener { disconnectFromDevice() }
        btnApplyConfig.setOnClickListener { applySerialConfiguration() }
        btnSendData.setOnClickListener { sendData() }
        btnClearLog.setOnClickListener { clearLog() }
        switchAutoscroll.setOnCheckedChangeListener { _, isChecked ->
            autoScroll = isChecked
        }
        switchSkipPermissionCheck.setOnCheckedChangeListener { _, isChecked ->
            skipPermissionCheck = isChecked
            updateUIState()
        }

        spinnerDevices.setOnItemClickListener { _, _, position, _ ->
            if (position < deviceList.size) {
                selectedDevice = deviceList[position].device
                tvDeviceInfo.text = "Selected: ${deviceList[position].getDetailedInfo()}"
                hasPermission = selectedDevice?.let { usbManager.hasPermission(it) } ?: false
                updateUIState()
            }
        }

        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSendButtonState()
            }
        }
        etSendData.addTextChangedListener(textWatcher)
    }

    private fun setupSpinners() {
        val baudRateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, BAUD_RATES)
        spinnerBaudRate.setAdapter(baudRateAdapter)
        spinnerBaudRate.setText(DEFAULT_BAUD_RATE, false)

        val dataBitsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, DATA_BITS)
        spinnerDataBits.setAdapter(dataBitsAdapter)
        spinnerDataBits.setText(DEFAULT_DATA_BITS, false)

        val stopBitsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, STOP_BITS)
        spinnerStopBits.setAdapter(stopBitsAdapter)
        spinnerStopBits.setText(DEFAULT_STOP_BITS, false)

        val parityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, PARITY_OPTIONS)
        spinnerParity.setAdapter(parityAdapter)
        spinnerParity.setText(DEFAULT_PARITY, false)
    }

    private fun scanForUsbDevices() {
        if (isConnected) {
            disconnectFromDevice()
        }

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        deviceList.clear()
        deviceList.addAll(availableDrivers.map { UsbDeviceInfo.from(it.device) })

        spinnerDevices.setText("", false)
        selectedDevice = null
        hasPermission = false

        if (deviceList.isEmpty()) {
            appendLog(LogType.INFO, "No USB serial devices found")
            tvDeviceInfo.text = "No serial devices found"
            tvDeviceSectionHeader.text = "USB Serial Device Selection"
        } else {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deviceList)
            spinnerDevices.setAdapter(adapter)

            val deviceCount = deviceList.size
            val countText = if (deviceCount == 1) "1 device" else "$deviceCount devices"
            tvDeviceSectionHeader.text = "USB Serial Device Selection ($countText)"
            tvDeviceInfo.text = "Select a device from the list"
            appendLog(LogType.INFO, "Found $deviceCount USB serial device(s)")
        }

        updateUIState()
    }

    private fun requestDeviceAccessPermission() {
        selectedDevice?.let { device ->
            // Skip permission check: always request permission (even if already granted)
            // Normal mode: only request if not already granted
            if (!skipPermissionCheck && usbManager.hasPermission(device)) {
                hasPermission = true
                appendLog(LogType.INFO, "Device access already granted")
                updateUIState()
            } else {
                val permissionIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    0,
                    Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(device, permissionIntent)
                val currentPermission = usbManager.hasPermission(device)
                appendLog(LogType.DEBUG, "Requesting device access (current: $currentPermission)")
            }
        } ?: appendLog(LogType.WARN, "No device selected")
    }

    private fun connectToDevice() {
        try {
            selectedDevice?.let { device ->
                val connection = usbManager.openDevice(device)
                    ?: throw IOException("Cannot open USB device")

                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                val driver = availableDrivers.find { it.device == device }
                    ?: throw IOException("No compatible driver found for this device")

                usbSerialPort = driver.ports[0]
                usbSerialPort?.open(connection)
                applySerialConfiguration()
                startContinuousReading()

                isConnected = true
                appendLog(LogType.INFO, "Connected to serial device")
                updateUIState()
            } ?: appendLog(LogType.WARN, "No device selected")
        } catch (e: Exception) {
            appendLog(LogType.ERROR, "Connection failed: ${e.message}")
            try { usbSerialPort?.close() } catch (_: Exception) {}
            usbSerialPort = null
            isConnected = false
            updateUIState()
        }
    }

    private fun disconnectFromDevice() {
        stopContinuousReading()
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            appendLog(LogType.ERROR, "Disconnect error: ${e.message}")
        } finally {
            usbSerialPort = null
            isConnected = false
            appendLog(LogType.INFO, "Disconnected from serial device")
            updateUIState()
        }
    }

    private fun applySerialConfiguration() {
        try {
            val baudRate = spinnerBaudRate.text.toString().toIntOrNull() ?: 115200
            val dataBits = spinnerDataBits.text.toString().toIntOrNull() ?: 8
            val stopBitsStr = spinnerStopBits.text.toString()
            val parityStr = spinnerParity.text.toString()

            val stopBits = when (stopBitsStr) {
                "1" -> UsbSerialPort.STOPBITS_1
                "1.5" -> UsbSerialPort.STOPBITS_1_5
                "2" -> UsbSerialPort.STOPBITS_2
                else -> UsbSerialPort.STOPBITS_1
            }

            val parity = when (parityStr) {
                "None" -> UsbSerialPort.PARITY_NONE
                "Odd" -> UsbSerialPort.PARITY_ODD
                "Even" -> UsbSerialPort.PARITY_EVEN
                "Mark" -> UsbSerialPort.PARITY_MARK
                "Space" -> UsbSerialPort.PARITY_SPACE
                else -> UsbSerialPort.PARITY_NONE
            }

            usbSerialPort?.setParameters(baudRate, dataBits, stopBits, parity)
            appendLog(LogType.INFO, "Config: ${baudRate}/${dataBits}/${stopBitsStr}/${parityStr}")
        } catch (e: Exception) {
            appendLog(LogType.ERROR, "Configuration failed: ${e.message}")
        }
    }

    private fun sendData() {
        val port = usbSerialPort ?: return

        try {
            var text = etSendData.text.toString()
            if (cbAppendNewline.isChecked) {
                text += "\r\n"
            }
            port.write(text.toByteArray(Charsets.UTF_8), WRITE_TIMEOUT_MS)
            appendLog(LogType.TX, etSendData.text.toString())
        } catch (e: IOException) {
            appendLog(LogType.ERROR, "Send failed: ${e.message}")
        }
    }

    private fun startContinuousReading() {
        val listener = object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                if (view == null) return
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    receiveBuffer.append(data.toString(Charsets.UTF_8))

                    var newlineIndex = receiveBuffer.indexOf('\n')
                    while (newlineIndex >= 0) {
                        val line = receiveBuffer.substring(0, newlineIndex).trimEnd('\r')
                        if (line.isNotEmpty()) {
                            appendLog(LogType.RX, line)
                        }
                        receiveBuffer.delete(0, newlineIndex + 1)
                        newlineIndex = receiveBuffer.indexOf('\n')
                    }
                }
            }

            override fun onRunError(e: Exception) {
                if (view == null) return
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    if (isConnected) {
                        appendLog(LogType.ERROR, "Read error: ${e.message}")
                    }
                }
            }
        }

        serialInputOutputManager = SerialInputOutputManager(usbSerialPort, listener)
        serialInputOutputManager?.start()
        appendLog(LogType.DEBUG, "Continuous reading started")
    }

    private fun stopContinuousReading() {
        serialInputOutputManager?.let {
            it.stop()
            serialInputOutputManager = null
            receiveBuffer.clear()
            appendLog(LogType.DEBUG, "Continuous reading stopped")
        }
    }

    private fun appendLog(type: LogType, message: String) {
        val timestamp = timestampFormat.format(Date())
        val typeStr = when (type) {
            LogType.RX -> "RX "
            LogType.TX -> "TX "
            LogType.DEBUG -> "DBG"
            LogType.INFO -> "INF"
            LogType.WARN -> "WRN"
            LogType.ERROR -> "ERR"
        }

        val entry = "[$timestamp] [$typeStr] $message\n"
        logBuffer.append(entry)
        if (logBuffer.length > MAX_LOG_BUFFER_SIZE) {
            logBuffer.delete(0, logBuffer.length - (MAX_LOG_BUFFER_SIZE * 4 / 5))
        }

        tvLogDisplay.text = logBuffer.toString()

        if (autoScroll) {
            scrollLog.post {
                scrollLog.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun clearLog() {
        logBuffer.clear()
        appendLog(LogType.INFO, "Log cleared")
    }

    private fun updateUIState() {
        spinnerDevices.isEnabled = deviceList.isNotEmpty()
        tilDevices.isEnabled = deviceList.isNotEmpty()

        // Skip permission check: allow requesting permission even if already granted
        btnRequestDeviceAccess.isEnabled = if (skipPermissionCheck) {
            selectedDevice != null
        } else {
            selectedDevice != null && !hasPermission
        }

        // Skip permission check: allow connecting even without permission
        btnConnect.isEnabled = if (skipPermissionCheck) {
            selectedDevice != null && !isConnected
        } else {
            hasPermission && !isConnected
        }

        btnDisconnect.isEnabled = isConnected
        btnApplyConfig.isEnabled = isConnected
        updateSendButtonState()
        updateConnectionStatus()
    }

    private fun updateSendButtonState() {
        btnSendData.isEnabled = isConnected && etSendData.text.toString().isNotEmpty()
    }

    private fun updateConnectionStatus() {
        when {
            isConnected -> {
                tvConnectionStatus.text = "Connected"
                tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_connected)
                )
            }
            selectedDevice == null -> {
                tvConnectionStatus.text = "No device selected"
                tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_no_device)
                )
            }
            !hasPermission -> {
                tvConnectionStatus.text = "Device access permission required"
                tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_no_permission)
                )
            }
            else -> {
                tvConnectionStatus.text = "Ready to connect"
                tvConnectionStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.status_ready)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            textWatcher?.let { etSendData.removeTextChangedListener(it) }
            textWatcher = null
            stopContinuousReading()
            usbSerialPort?.close()
            usbSerialPort = null
            requireContext().unregisterReceiver(usbPermissionReceiver)
            deviceList.clear()
            selectedDevice = null
            logBuffer.clear()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.kotlinandroidsample.USB_PERMISSION"
        private const val MAX_LOG_BUFFER_SIZE = 50000
        private const val WRITE_TIMEOUT_MS = 1000
        private val BAUD_RATES = arrayOf("9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600")
        private val DATA_BITS = arrayOf("5", "6", "7", "8")
        private val STOP_BITS = arrayOf("1", "1.5", "2")
        private val PARITY_OPTIONS = arrayOf("None", "Odd", "Even", "Mark", "Space")
        private const val DEFAULT_BAUD_RATE = "115200"
        private const val DEFAULT_DATA_BITS = "8"
        private const val DEFAULT_STOP_BITS = "1"
        private const val DEFAULT_PARITY = "None"
    }
}
