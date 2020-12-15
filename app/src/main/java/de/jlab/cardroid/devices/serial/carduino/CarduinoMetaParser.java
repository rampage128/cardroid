package de.jlab.cardroid.devices.serial.carduino;

import android.app.Application;
import android.util.Log;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDevice;

public final class CarduinoMetaParser extends CarduinoPacketParser {

    private static final int MAX_ID_RETRIES = 3;

    private static final byte PROTOCOL_MAJOR = 0x01;
    private static final int OFFSET_CARDUINO_ID = 3;
    private static final int LENGTH_CARDUINO_ID = 3;

    private Application app;
    private CarduinoUsbDevice device;
    private boolean pingReceived = false;
    private int idRetryCount = 0;

    private static final CarduinoSerialPacket PACKET_CONNECTION_REQUEST = CarduinoMetaType.createPacket(CarduinoMetaType.CONNECTION_REQUEST, new byte[] { PROTOCOL_MAJOR });
    private static final CarduinoSerialPacket PACKET_BAUD_RATE_REQUEST = CarduinoMetaType.createPacket(CarduinoMetaType.BAUD_RATE_REQUEST, ByteBuffer.allocate(4).putInt(115200).array());

    public CarduinoMetaParser(@NonNull CarduinoUsbDevice device, @NonNull Application app) {
        this.device = device;
        this.app    = app;
    }

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.META.equals(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        int eventType = packet.getPacketId();

        if (eventType == 0x00 && !this.pingReceived) {
            this.pingReceived(packet);
        }
        else if (eventType == 0x01) {
            this.acceptHandshake();
        }
        else if (eventType == 0x02) {
            int baudRate = (int)packet.readDWord(0);
            this.device.setBaudRate(baudRate);
        }
        else if (eventType == 0x03) {
            Log.d(this.getClass().getSimpleName(), "Disconnect request received from " + this.device);
            this.device.close();
        }
        else if (eventType == 0x49) {
            this.newIdReceived(packet);
        }
    }

    private void pingReceived(@NonNull CarduinoSerialPacket packet) {
        this.pingReceived = true;
        byte[] carduinoId = packet.readBytes(OFFSET_CARDUINO_ID, LENGTH_CARDUINO_ID);
        if (CarduinoUidGenerator.isValidId(carduinoId)) {
            Log.d(this.getClass().getSimpleName(), "Ping received with valid ID from " + this.device);
            this.device.onCarduinoIdReceived(carduinoId);
            this.requestConnection(packet);
        } else {
            Log.d(this.getClass().getSimpleName(), "Ping with invalid ID from " + this.device);
            this.requestNewId();
        }
    }

    private void newIdReceived(@NonNull CarduinoSerialPacket packet) {
        byte[] carduinoId = packet.readBytes(0, LENGTH_CARDUINO_ID);
        if (CarduinoUidGenerator.isValidId(carduinoId)) {
            Log.d(this.getClass().getSimpleName(), "New valid carduino ID " + new String(carduinoId) + " received from " + this.device);
            this.device.onCarduinoIdReceived(carduinoId);
            this.requestConnection(packet);
        } else if (this.idRetryCount < MAX_ID_RETRIES){
            this.idRetryCount++;
            Log.d(this.getClass().getSimpleName(), "New invalid ID " + new String(carduinoId) + " received from " + this.device);
            this.requestNewId();
        } else {
            Log.d(this.getClass().getSimpleName(), "Too many invalid IDs received from " + this.device);
            this.device.close();
        }
    }

    private void requestNewId() {
        byte[] newId = CarduinoUidGenerator.generateId(this.app).getBytes();
        this.device.sendImmediately(CarduinoMetaType.createPacket(CarduinoMetaType.SET_UID, newId));
    }

    private void acceptHandshake() {
        Log.d(this.getClass().getSimpleName(), "Accepting Handshake from " + this.device);
        this.device.onHandshake();
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
            Log.d(this.getClass().getSimpleName(), "Requesting Connection from " + this.device);
            this.device.sendImmediately(PACKET_CONNECTION_REQUEST);
        } else {
            Log.e(this.getClass().getSimpleName(), "Rejecting wrong protocol from " + this.device);
            this.device.close();
        }
    }

}
