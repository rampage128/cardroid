package de.jlab.cardroid.devices.usb.serial;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.usb.UsbDeviceHandler;
import de.jlab.cardroid.devices.serial.SerialReader;

public abstract class UsbSerialDeviceHandler<ReaderType extends SerialReader> extends UsbDeviceHandler {

    private ReaderType reader;
    private UsbSerialConnection connection;

    public UsbSerialDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull DeviceService service) {
        super(device, service);

        UsbManager usbManager = (UsbManager)service.getSystemService(Context.USB_SERVICE);
        this.connection = new UsbSerialConnection(device, defaultBaudrate, usbManager);
    }

    @Override
    public boolean isConnected() {
        return this.connection.isConnected();
    }

    @Override
    public boolean connectDevice() {
        boolean isConnected = this.connection.connect();
        if (isConnected) {
            Log.e(this.getClass().getSimpleName(), "Device connected " + device.getDeviceId());
            this.reader = this.onConnect();
            this.connection.addUsbSerialReader(this.reader);
        } else {
            this.onConnectFailed();
        }
        return isConnected;
    }

    @Override
    public void disconnectDevice() {
        Log.e(this.getClass().getSimpleName(), "Device disconnected " + device.getDeviceId());
        this.connection.removeUsbSerialReader(this.reader);
        if (this.connection.isConnected()) {
            this.connection.disconnect();
        }
        this.onDisconnect(this.reader);
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
