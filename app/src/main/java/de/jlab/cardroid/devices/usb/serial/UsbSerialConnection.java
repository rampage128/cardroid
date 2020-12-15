package de.jlab.cardroid.devices.usb.serial;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import de.jlab.cardroid.devices.serial.SerialConnectionListener;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.utils.UsageStatistics;

public final class UsbSerialConnection {
    private UsbManager usbManager;

    private UsbDevice device;
    private UsbSerialPort serialPort = null;
    private SerialInputOutputManager usbIoManager = null;
    private int baudRate = 0;

    private boolean isConnected = false;

    private UsageStatistics bandWidthUsage = new UsageStatistics(1000, 60);

    private ArrayList<SerialConnectionListener> listeners = new ArrayList<>();
    private ArrayList<SerialReader> readers = new ArrayList<>();

    private SerialInputOutputManager.Listener readCallback = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            bandWidthUsage.count(data.length);
            for (int i = 0; i < readers.size(); i++) {
                readers.get(i).onReceiveData(data);
            }
        }

        @Override
        public void onRunError(Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error in read callback of device " + device.getDeviceName(), e);
        }
    };

    public UsbSerialConnection(@NonNull UsbDevice device, int baudRate, @Nullable UsbManager usbManager) {
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

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(this.device);
        if(driver == null) {
            Log.w(this.getClass().getSimpleName(), "No driver found for " + this.device.getDeviceName());
            return false;
        }

        UsbDeviceConnection connection = this.usbManager.openDevice(this.device);
        if (connection == null) {
            return false;
        }

        // Retrieve first port from serial device
        this.serialPort = driver.getPorts().get(0);
        try {
            this.serialPort.open(connection);
            this.serialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Opening port on device " + this.device.getDeviceName() + " failed!", e);
            return false;
        }

        this.usbIoManager = new SerialInputOutputManager(this.serialPort, this.readCallback);
        Executors.newSingleThreadExecutor().submit(this.usbIoManager);

        this.isConnected = true;

        for (SerialConnectionListener listener : this.listeners) {
            listener.onConnect();
        }

        return true;
    }

    public void disconnect() {
        if (!this.isConnected) {
            return;
        }

        if(this.usbIoManager != null) {
            this.usbIoManager.stop();
        }
        this.usbIoManager = null;

        if (this.serialPort != null) {
            try {
                this.serialPort.close();
            } catch (Exception e) {
                Log.d(this.getClass().getSimpleName(), "Can not close port on device " + this.device.getDeviceName(), e);
            }
            this.serialPort = null;
        }

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
            return false;
        }

        try {
            this.serialPort.write(data, 2000);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Could not write to port on " + this.device.getDeviceName());
        }
        return true;
    }

    public void setBaudRate(int baudRate) {
        try {
            this.serialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Could not set new baud rate on " + this.device.getDeviceName(), e);
        }
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
