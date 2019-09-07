package de.jlab.cardroid.usb.carduino;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.jlab.cardroid.usb.CarSystemSerialPacket;

public enum SerialPacketFactory {
    META(0x61, MetaSerialPacket.class),
    CAN_BUS(0x62, SerialCanPacket.class),
    COMMAND(0x63, SerialCarButtonEventPacket.class),
    ERROR(0x65, SerialErrorPacket.class),
    CAR_SYSTEM(0x73, CarSystemSerialPacket.class);

    private static final String LOG_TAG = "SerialPacketFactory";

    private byte identifier;
    private Class<? extends SerialPacket> packetClass;

    SerialPacketFactory(int identifier, Class<? extends SerialPacket> packetClass) {
        this.identifier = (byte)identifier;
        this.packetClass   = packetClass;
    }

    private static byte getIdentifier(SerialPacket packet) throws UnknownPacketTypeException {
        for (SerialPacketFactory packetType : SerialPacketFactory.values()) {
            if (packet.getClass().isAssignableFrom(packetType.packetClass)) {
                return packetType.identifier;
            }
        }

        throw new UnknownPacketTypeException(packet.getClass());
    }

    public static SerialPacket getPacketFromData(ByteArrayInputStream stream) throws DeserializationException, UnknownPacketTypeException {
        int identifier = stream.read();
        for (SerialPacketFactory packetType : SerialPacketFactory.values()) {
            if (identifier == packetType.identifier) {
                try {
                    Constructor<? extends SerialPacket> constructor = packetType.packetClass.getConstructor(ByteArrayInputStream.class);
                    return constructor.newInstance(stream);
                } catch (NoSuchMethodException e) {
                    throw new DeserializationException("Cannot find constructor " + packetType.packetClass.getSimpleName() + "(ByteArrayInputStream)", e);
                } catch (IllegalAccessException e) {
                    throw new DeserializationException("Cannot access constructor of packet " + packetType.packetClass.getSimpleName(), e);
                } catch (InstantiationException e) {
                    throw new DeserializationException("Cannot create instance of packet " + packetType.packetClass.getSimpleName(), e);
                } catch (InvocationTargetException e) {
                    throw new DeserializationException("An error occured while creating packet " + packetType.packetClass.getSimpleName(), e.getTargetException());
                }
            }
        }

        throw new UnknownPacketTypeException((byte)identifier);
    }

    public static byte[] serialize(SerialPacket packet) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(SerialPacketStructure.HEADER);
            outputStream.write(getIdentifier(packet));
            packet.serialize(outputStream);
            outputStream.write(SerialPacketStructure.FOOTER);
            return outputStream.toByteArray();
        }
        catch (UnknownPacketTypeException e) {
            throw new UnsupportedOperationException("Packet " + packet.getClass().getCanonicalName() + " cannot be sent.", e);
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Error serializing packet: " + packet.getClass().getSimpleName(), e);
        }
        return null;
    }
}
