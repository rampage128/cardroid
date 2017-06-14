package de.jlab.cardroid.usb;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

enum SerialPacketFactory {
    CARSYSTEM((byte)0x73, CarSystemSerialPacket.class),
    META((byte)0x61, SerialPacket.class),
    ERROR((byte)0x65, SerialPacket.class);

    private byte identifier;
    private Class<? extends SerialPacket> packetClass;

    SerialPacketFactory(byte identifier, Class<? extends SerialPacket> packetClass) {
        this.identifier = identifier;
        this.packetClass   = packetClass;
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
}
