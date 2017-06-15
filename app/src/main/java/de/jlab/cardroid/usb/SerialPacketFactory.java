package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

enum SerialPacketFactory {
    META(0x61, SerialPacket.class),
    COMMAND(0x63, SerialCommandPacket.class),
    ERROR(0x65, SerialPacket.class),
    CAR_SYSTEM(0x73, CarSystemSerialPacket.class);

    private byte identifier;
    private Class<? extends SerialPacket> packetClass;

    SerialPacketFactory(int identifier, Class<? extends SerialPacket> packetClass) {
        this.identifier = (byte)identifier;
        this.packetClass   = packetClass;
    }

    private static byte getIdentifier(SerialPacket packet) throws UnknownPacketTypeException {
        for (SerialPacketFactory packetType : SerialPacketFactory.values()) {
            if (packetType.packetClass.isInstance(packet)) {
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

    public static void serialize(SerialPacket packet, ByteArrayOutputStream output) throws UnknownPacketTypeException, IOException {
        output.write(getIdentifier(packet));
        packet.serialize(output);
    }
}
