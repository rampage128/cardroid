package de.jlab.cardroid.devices.usb.serial;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.devices.usb.UsbDeviceHandler;

public abstract class UsbSerialDeviceHandler<ReaderType extends SerialReader> extends UsbDeviceHandler {

    private ReaderType reader;
    private UsbSerialConnection connection;
    private DeviceUid uid;

    public UsbSerialDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull Application app) {
        super(device);

        UsbManager usbManager = (UsbManager)app.getSystemService(Context.USB_SERVICE);
        this.connection = new UsbSerialConnection(device, defaultBaudrate, usbManager);
        this.uid = DeviceUid.fromUsbDevice(device);
    }

    @Override
    public boolean isConnected() {
        return this.connection.isConnected();
    }

    @Override
    public boolean connectDevice() {
        boolean isConnected = this.connection.connect();
        if (isConnected) {
            Log.e(this.getClass().getSimpleName(), "Device connected " + this.getConnectionId() + " (" + this.connection.getBaudRate() + ")");
            this.notifyStart();
            this.reader = this.onConnect();
            this.connection.addUsbSerialReader(this.reader);
        } else {
            this.onConnectFailed();
        }
        return isConnected;
    }

    @Override
    public void disconnectDevice() {
        Log.e(this.getClass().getSimpleName(), "Device disconnecting... " + this.getConnectionId());
        this.connection.removeUsbSerialReader(this.reader);
        if (this.connection.isConnected()) {
            Log.e(this.getClass().getSimpleName(), "Device disconnected " + this.getConnectionId());
            this.connection.disconnect();
        }
        this.onDisconnect(this.reader);
        this.notifyEnd();
    }

    public void setBaudRate(int baudRate) {
        this.connection.setBaudRate(baudRate);
    }

    protected void send(byte[] data) {
        this.connection.send(data);
    }

    protected abstract ReaderType onConnect();
    protected abstract void onConnectFailed();
    protected abstract void onDisconnect(ReaderType reader);

}
