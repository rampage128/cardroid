package de.jlab.cardroid.devices.serial.carduino;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.serial.BinarySerialPacket;

public final class CarduinoSerialPacket extends BinarySerialPacket {

    public static final byte HEADER = 0x7b;
    public static final byte FOOTER = 0x7d;

    private static final int OFFSET_TYPE = 0;
    private static final int OFFSET_ID = 1;
    private static final int OFFSET_SIZE = 2;
    private static final int OFFSET_PAYLOAD = 3;

    public CarduinoSerialPacket(@NonNull byte[] rawData) {
        super(rawData);
    }

    public static CarduinoSerialPacket create(byte packetType, byte packetId, @Nullable byte[] payload) {
        byte[] data;
        if (payload == null || payload.length < 1) {
            data = new byte[2];
        } else {
            data = new byte[payload.length + 3];
            data[OFFSET_SIZE] = (byte)payload.length;
            System.arraycopy(payload, 0, data, 3, payload.length);
        }

        data[OFFSET_ID] = packetId;
        data[OFFSET_TYPE] = packetType;

        return new CarduinoSerialPacket(data);
    }

    public byte getPacketType() {
        return this.data[0];
    }

    public byte getPacketId() {
        return this.data[1];
    }

    public int readByte(int index) {
        return super.readByte(index + OFFSET_PAYLOAD);
    }

    public long readDWord(int index) {
        return super.readDWord(index + OFFSET_PAYLOAD);
    }

    public byte[] readBytes(int startIndex, int length) {
        return super.readBytes(startIndex + OFFSET_PAYLOAD, length);
    }

    protected byte[] readBytes(int startIndex) {
        return super.readBytes(startIndex + OFFSET_PAYLOAD);
    }

    public byte getPayloadSize() {
        byte value = this.data[2];
        if (value == FOOTER) {
            return 0;
        }
        return value;
    }

    @Override
    public void serialize(@NonNull ByteArrayOutputStream stream) throws IOException {
        stream.write(HEADER);
        super.serialize(stream);
        stream.write(FOOTER);
    }
}
