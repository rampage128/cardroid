package de.jlab.cardroid.devices.detection;

import android.app.Application;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.DeviceConnectionRequest;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialPacket;
import de.jlab.cardroid.devices.serial.carduino.CarduinoUidGenerator;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDevice;

public final class CarduinoSerialMatcher implements UsbSerialDeviceDetector.SerialMatcher {

    private static final String PATTERN = new StringBuilder("[\\S\\s]*")
            .append("\\").append((char)CarduinoSerialPacket.HEADER)
            .append((char)CarduinoPacketType.META.getType())
            .append("[^\\}]*").append("\\").append((char)CarduinoSerialPacket.FOOTER)
            .append("[\\S\\s]*").toString();

    private String received = "";

    @Nullable
    @Override
    public DeviceConnectionRequest detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app) {
        this.received += new String(data);
        Log.e(this.getClass().getSimpleName(), "<- " + this.received + "(" + PATTERN + " = " + this.received.matches(PATTERN) + ")");
        if (this.received.matches(PATTERN)) {
            return new DeviceConnectionRequest(
                    new CarduinoUsbDevice(device, baudRate, app),
                    CarduinoUidGenerator.getUid(this.received)
            );
        }
        return null;
    }

    @Override
    public void clear() {
        this.received = "";
    }
}
