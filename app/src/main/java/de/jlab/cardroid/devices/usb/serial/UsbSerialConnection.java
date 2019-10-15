package de.jlab.cardroid.devices.usb.serial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.serial.SerialConnectionListener;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.utils.UsageStatistics;

public final class UsbSerialConnection {
    private UsbManager usbManager;

    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serial;
    private int baudRate = 0;

    private boolean isConnected = false;

    private UsageStatistics bandWidthUsage = new UsageStatistics(1000, 60);

    private ArrayList<SerialConnectionListener> listeners = new ArrayList<>();
    private ArrayList<SerialReader> readers = new ArrayList<>();

    private UsbSerialInterface.UsbReadCallback readCallback = bytes -> {
        bandWidthUsage.count(bytes.length);
        for (int i = 0; i < this.readers.size(); i++) {
            this.readers.get(i).onReceiveData(bytes);
        }
    };

    public UsbSerialConnection(@NonNull UsbDevice device, int baudRate, @Nullable UsbManager usbManager) {
        // FIXME: There seems to be a bug in the JNI part of the serial communication lib. It seems keeping an instance of the device can trigger native errors. Not sure tho.
        this.usbManager = usbManager;
        this.device = device;
        this.baudRate = baudRate;
    }

    public UsbDevice getDevice() {
        return this.device;
    }

    /**
     * Connects to an USB-Device
     */
    public boolean connect() {
        if (this.isConnected()) {
            this.disconnect();
        }

        // On emulators UsbManager will always be null
        if (this.usbManager == null) {
            return false;
        }

        connection = this.usbManager.openDevice(this.device);
        if (connection == null) {
            return false;
        }

        this.serial = UsbSerialDevice.createUsbSerialDevice(device, connection);
        // Serial device will be null if no driver is found
        if(this.serial == null) {
            return false;
        }

        // Serial port will not open if there is an I/O error or the wrong driver is used
        if (!this.serial.open()) {
            return false;
        }

        this.serial.setBaudRate(baudRate);
        this.serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
        this.serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
        this.serial.setParity(UsbSerialInterface.PARITY_NONE);
        this.serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        this.serial.read(readCallback);

        this.isConnected = true;

        for (SerialConnectionListener listener : listeners) {
            listener.onConnect();
        }

        return true;
    }

    public void disconnect() {
        if (!this.isConnected) {
            return;
        }

        if (this.serial != null) {
            this.serial.close();
            this.serial = null;
        }

        /*
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
         */

        this.isConnected = false;

        for (SerialConnectionListener listener : listeners) {
            listener.onDisconnect();
        }
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public boolean send(@NonNull byte[] data) {
        if (!this.isConnected()) {
            Log.e(this.getClass().getSimpleName(), "Can not send packet, device " + this.device.getDeviceId() + " is not connected!");
            return false;
        }

        this.serial.write(data);
        return true;
    }

    public void setBaudRate(int baudRate) {
        this.serial.setBaudRate(baudRate);
    }

    public void addBandwidthStatisticsListener(@NonNull UsageStatistics.UsageStatisticsListener listener) {
        this.bandWidthUsage.addListener(listener);
    }

    public void removeBandwidthStatisticsListener(@NonNull UsageStatistics.UsageStatisticsListener listener) {
        this.bandWidthUsage.removeListener(listener);
    }

    public void addUsbSerialReader(@NonNull SerialReader reader) {
        this.readers.add(reader);
    }

    public void removeUsbSerialReader(@NonNull SerialReader reader) {
        this.readers.remove(reader);
    }

    public void addConnectionListener(@NonNull SerialConnectionListener listener) {
        this.listeners.add(listener);
    }

    public void removeConnectionListener(@NonNull SerialConnectionListener listener) {
        this.listeners.remove(listener);
    }

    public int getBaudRate() {
        return this.baudRate;
    }

}
