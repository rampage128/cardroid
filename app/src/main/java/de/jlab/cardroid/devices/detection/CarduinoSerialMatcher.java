package de.jlab.cardroid.devices.detection;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialPacket;
import de.jlab.cardroid.devices.detection.UsbSerialDeviceDetector;
import de.jlab.cardroid.devices.serial.carduino.CarduinoUidGenerator;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDevice;

public final class CarduinoSerialMatcher implements UsbSerialDeviceDetector.SerialMatcher {

    private static final String PATTERN = new StringBuilder(".*")
            .append("\\").append((char)CarduinoSerialPacket.HEADER)
            .append((char)CarduinoPacketType.META.getType())
            .append("[^\\}]*").append("\\").append((char)CarduinoSerialPacket.FOOTER)
            .append(".*").toString();

    private String received = "";
    private String deviceIdFrames = "";

    @Nullable
    @Override
    public Device detect(@NonNull byte[] data, @NonNull UsbDevice device, int baudRate, @NonNull Application app) {
        this.received += new String(data);
        if (this.received.matches(PATTERN)) {
            this.deviceIdFrames = this.received;
            return new CarduinoUsbDevice(device, baudRate, app);
        }
        return null;
    }

    @Nullable
    @Override
    public DeviceUid predictDeviceUid(@NonNull UsbDevice device) {
        return CarduinoUidGenerator.getUid(this.deviceIdFrames);
    }

    @Override
    public void clear() {
        this.received = "";
    }
}
