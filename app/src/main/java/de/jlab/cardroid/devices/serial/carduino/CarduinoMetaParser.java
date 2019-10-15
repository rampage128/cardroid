package de.jlab.cardroid.devices.serial.carduino;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;

public final class CarduinoMetaParser extends CarduinoPacketParser {

    private static final byte PROTOCOL_MAJOR = 0x01;
    private static final int OFFSET_CARDUINO_ID = 3;
    private static final int LENGTH_CARDUINO_ID = 3;

    private CarduinoUsbDeviceHandler device;
    private CarduinoSerialReader reader;
    private boolean allowConnection = false;

    private static final CarduinoSerialPacket PACKET_CONNECTION_REQUEST = CarduinoMetaType.createPacket(CarduinoMetaType.CONNECTION_REQUEST, new byte[] { PROTOCOL_MAJOR });
    private static final CarduinoSerialPacket PACKET_BAUD_RATE_REQUEST = CarduinoMetaType.createPacket(CarduinoMetaType.BAUD_RATE_REQUEST, ByteBuffer.allocate(4).putInt(115200).array());

    public CarduinoMetaParser(@NonNull CarduinoUsbDeviceHandler device, @NonNull CarduinoSerialReader reader) {
        this.device = device;
        this.reader = reader;
    }

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.META.equals(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        int eventType = packet.getPacketId();

        if (eventType == 0x00) {
            this.carduinoIdReceived(packet.readBytes(OFFSET_CARDUINO_ID, LENGTH_CARDUINO_ID));

            if (this.allowConnection) {
                this.requestConnection(packet);
            }
        }
        else if (eventType == 0x01) {
            this.acceptHandshake();
        }
        else if (eventType == 0x02) {
            int baudRate = (int)packet.readDWord(0);
            this.device.setBaudRate(baudRate);
        }
        else if (eventType == 0x03) {
            this.device.disconnectDevice();
        }
        else if (eventType == 0x49) { // TODO: check out if this packet is obsolete
            this.carduinoIdReceived(packet.readBytes(0, LENGTH_CARDUINO_ID));
        }
    }

    private void carduinoIdReceived(@NonNull byte[] carduinoId) {
        this.device.onCarduinoIdReceived(carduinoId);
    }

    public void allowHandshake() {
        this.allowConnection = true;
    }

    private void acceptHandshake() {
        this.device.onHandshake(this.reader);
            /* FIXME: per device baud-rate configuration has to be added!
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getService());
            String defaultBaudRateString = String.valueOf(device.getDefaultBaudrate());
            String baudRateString = prefs.getString("car_baud_rate", defaultBaudRateString);
            int baudRate = Integer.valueOf(baudRateString != null ? baudRateString : defaultBaudRateString);
        FIXME: baud rate request is not available until baud-rate configuration is fixed
        try {
            this.device.send(PACKET_BAUD_RATE_REQUEST);
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error sending baud rate request", e);
        }
             */
    }

    private void requestConnection(CarduinoSerialPacket sourcePacket) {
        int major = sourcePacket.readByte(0);
        //int minor = packet.readByte(1);
        //int revision = packet.readByte(2);

        if (major == PROTOCOL_MAJOR) {
            this.device.sendImmediately(PACKET_CONNECTION_REQUEST);
        } else {
            this.device.disconnectDevice();
        }
    }

}
