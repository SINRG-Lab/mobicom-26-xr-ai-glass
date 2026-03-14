package com.sdk.glassessdksample.ui
import com.sdk.glassessdksample.DeviceName

object DeviceManager {
    private var deviceName: DeviceName? = DeviceName()

    fun getDeviceName(): DeviceName? = deviceName
}