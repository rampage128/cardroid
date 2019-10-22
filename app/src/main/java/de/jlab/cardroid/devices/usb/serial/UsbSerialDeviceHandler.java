package de.jlab.cardroid.devices.usb.serial;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.SerialReader;
import de.jlab.cardroid.devices.usb.UsbDeviceHandler;

public abstract class UsbSerialDeviceHandler<ReaderType extends SerialReader> extends UsbDeviceHandler {

    private ReaderType reader;
    private UsbSerialConnection serialPort;
    private DeviceUid uid;

    public UsbSerialDeviceHandler(@NonNull UsbDevice device, int defaultBaudrate, @NonNull Application app) {
        super(device, app);

        UsbManager usbManager = (UsbManager)app.getSystemService(Context.USB_SERVICE);
        this.serialPort = new UsbSerialConnection(device, defaultBaudrate, usbManager);
        this.uid = DeviceUid.fromUsbDevice(device);
    }

    @Override
    public final void open() {
        State newState = this.serialPort.connect() ? State.OPEN : State.INVALID;
        if (newState == State.OPEN) {
            this.reader = this.onOpenSuccess();
            this.serialPort.addUsbSerialReader(this.reader);
        } else {
            this.onOpenFailed();
        }
        this.setState(newState);
    }

    @Override
    public final void close() {
        this.serialPort.removeUsbSerialReader(this.reader);
        if (this.serialPort.isConnected()) {
            this.serialPort.disconnect();
        }
        this.onClose(this.reader);
        this.setState(State.INVALID);
    }

    /**
     * @deprecated TODO setBaudRate and send should probably be part of an interactable? Yes, no?
     */
    @Deprecated
    public void setBaudRate(int baudRate) {
        this.serialPort.setBaudRate(baudRate);
    }

    /**
     * @deprecated TODO setBaudRate and send should probably be part of an interactable? Yes, no?
     */
    @Deprecated
    protected void send(byte[] data) {
        this.serialPort.send(data);
    }

    @NonNull
    protected final ReaderType getReader() {
        if (this.reader == null) {
            throw new IllegalStateException("Reader has not been initialized yet! Did you call getReader() before open()?");
        }
        return this.reader;
    }

    protected abstract ReaderType onOpenSuccess();
    protected abstract void onOpenFailed();
    protected abstract void onClose(ReaderType reader);

}
