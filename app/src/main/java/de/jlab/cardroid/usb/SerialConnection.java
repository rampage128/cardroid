package de.jlab.cardroid.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;

public class SerialConnection {
    private Context context;

    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serial;
    private int baudRate = 0;

    private boolean isConnected = false;

    private UsageStatistics bandWidthUsage = new UsageStatistics(1000, 60);

    private ArrayList<SerialConnectionListener> listeners = new ArrayList<>();

    private UsbSerialInterface.UsbReadCallback readCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            bandWidthUsage.count(bytes.length);
            for (SerialConnectionListener listener : listeners) {
                listener.onReceiveData(bytes);
            }
        }
    };

    public SerialConnection(Context context) {
        this.context = context;
    }

    /**
     * Connects to an USB-Device
     */
    public void connect(UsbDevice device, int baudRate) {
        if (this.isConnected()) {
            this.disconnect();
        }

        this.device = device;
        this.baudRate = baudRate;

        if (this.device == null) {
            return;
        }

        UsbManager usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);

        if (usbManager == null) {
            return;
        }

        connection = usbManager.openDevice(this.device);
        if (connection == null) {
            return;
        }

        this.serial = UsbSerialDevice.createUsbSerialDevice(device, connection);
        // SERIALPORT WILL BE NULL IF NO DRIVER IS FOUND
        if(this.serial != null) {
            // SERIALPORT WILL NOT OPEN IF THERE IS AN I/O ERROR OR THE WRONG DRIVER WAS CHOSEN
            if (this.serial.open()) {
                this.serial.setBaudRate(baudRate);
                this.serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                this.serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                this.serial.setParity(UsbSerialInterface.PARITY_NONE);
                this.serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                this.serial.read(readCallback);
            }
        }

        this.isConnected = true;

        for (SerialConnectionListener listener : listeners) {
            listener.onConnect();
        }
    }

    public void reconnect() {
        UsbDevice device = this.device;
        int baudRate = this.baudRate;

        this.disconnect();
        this.connect(device, baudRate);
    }

    public void disconnect() {
        if (!this.isConnected) {
            return;
        }

        if (this.serial != null) {
            this.serial.close();
            this.serial = null;
        }
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
        this.device = null;

        this.isConnected = false;

        for (SerialConnectionListener listener : listeners) {
            listener.onDisconnect();
        }
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public boolean isConnected(UsbDevice device) {
        if (this.device == null) {
            return false;
        }

        return this.device.getDeviceId() == device.getDeviceId();
    }

    public boolean send(byte[] data) {
        if (!this.isConnected()) {
            return false;
        }
        this.serial.write(data);
        return true;
    }

    public void setBaudRate(int baudRate) {
        this.serial.setBaudRate(baudRate);
    }

    public void addBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
        this.bandWidthUsage.addListener(listener);
    }

    public void removeBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
        this.bandWidthUsage.removeListener(listener);
    }

    public void addConnectionListener(SerialConnectionListener listener) {
        this.listeners.add(listener);
    }

    public void removeConnectionListener(SerialConnectionListener listener) {
        this.listeners.remove(listener);
    }

    public int getBaudRate() {
        return this.baudRate;
    }

    public interface SerialConnectionListener {
        void onReceiveData(byte[] data);
        void onConnect();
        void onDisconnect();
    }
}
