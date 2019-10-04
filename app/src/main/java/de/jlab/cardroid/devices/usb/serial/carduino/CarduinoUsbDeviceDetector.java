package de.jlab.cardroid.devices.usb.serial.carduino;

import android.hardware.usb.UsbDevice;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketParser;
import de.jlab.cardroid.devices.serial.carduino.CarduinoPacketType;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialPacket;
import de.jlab.cardroid.devices.serial.carduino.CarduinoSerialReader;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceDetector;

public final class CarduinoUsbDeviceDetector extends UsbSerialDeviceDetector {

    private CarduinoDetectionParser parser;
    private CarduinoSerialReader reader;
    private CarduinoDummyDeviceHandler dummyDevice;

    private UsbDevice device;
    private DeviceService service;

    @Override
    protected boolean startSerialIdentification(@NonNull UsbDevice device, @NonNull DeviceService service) {
        this.device = device;
        this.service = service;

        this.parser = new CarduinoUsbDeviceDetector.CarduinoDetectionParser();
        this.reader = new CarduinoSerialReader();
        this.reader.addSerialPacketListener(this.parser);
        this.dummyDevice = new CarduinoDummyDeviceHandler(this.reader, device, 115200, service);

        return this.dummyDevice.connectDevice();
    }

    @Override
    protected long getTimeout() {
        return 1000;
    }

    private void deviceDetected(char deviceType) {
        this.dummyDevice.disconnectDevice();
        switch (deviceType) {
            case 'L':
                this.deviceDetected(new CarduinoLegacyDeviceHandler(this.device, 115200, this.service));
                break;
            case 'C':
                this.deviceDetected(new CarduinoCanDeviceHandler(this.device, 115200, this.service));
                break;
            case 'P':
                this.deviceDetected(new CarduinoPowerDeviceHandler(this.device, 115200, this.service));
                break;
        }
    }

    private class CarduinoDetectionParser extends CarduinoPacketParser {
        @Override
        protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
            return CarduinoPacketType.META.equals(packet.getPacketType()) && packet.getPacketId() == 0x00;
        }

        @Override
        protected void handlePacket(CarduinoSerialPacket packet) {
            CarduinoUsbDeviceDetector.this.deviceDetected((char)packet.readByte(3));
        }
    }
}
