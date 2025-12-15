package com.example.kotlinandroidsample.usbserial

import android.hardware.usb.UsbDevice

data class UsbDeviceInfo(
    val device: UsbDevice,
    val displayName: String,
    val vid: String,
    val pid: String
) {
    override fun toString(): String = displayName

    fun getDetailedInfo(): String {
        return "$displayName (VID:$vid PID:$pid)"
    }

    companion object {
        fun from(device: UsbDevice): UsbDeviceInfo {
            val vid = String.format("0x%04X", device.vendorId)
            val pid = String.format("0x%04X", device.productId)

            val manufacturer = device.manufacturerName
            val product = device.productName

            val deviceName = when {
                !product.isNullOrEmpty() && !manufacturer.isNullOrEmpty() ->
                    "$manufacturer $product"
                !product.isNullOrEmpty() ->
                    product
                !manufacturer.isNullOrEmpty() ->
                    manufacturer
                else ->
                    null
            }

            // Display name for spinner (without VID/PID)
            val displayName = deviceName ?: "USB Device $vid:$pid"

            return UsbDeviceInfo(device, displayName, vid, pid)
        }
    }
}
