package de.jlab.cardroid.car;

import de.jlab.cardroid.devices.serial.can.CanPacket;
import de.jlab.cardroid.variables.ObservableValue;

/**
 * @deprecated This has to be rewritten and placed somewhere meaningful (maybe as ObservableCanValue in variables)
 */
@Deprecated
public class CarData extends ObservableValue {

    public enum Type {
        FLAG,
        NUMBER_BIG_ENDIAN,
        NUMBER_LITTLE_ENDIAN,
        ARRAY,
        STRING,
    }

    private Type type;
    private int byteIndex;
    private int length;

    public CarData(int byteIndex, Type type, int length, Object defaultValue) {
        super(defaultValue);
        this.byteIndex = byteIndex;
        this.type = type;
        this.length = length;
    }

    public int getByteIndex() {
        return this.byteIndex;
    }

    public void updateFromSerialPacket(CanPacket packet, int byteOffset) {
        int readIndex = this.byteIndex + byteOffset;

        Object newValue;
        if (this.type == Type.ARRAY) {
            byte[] bytes = new byte[this.length];
            for (int i = 0; i < this.length; i++) {
                //bytes[i] = packet.readByte(readIndex + i);
            }
            newValue = bytes;
        } else if (this.type == Type.STRING) {
            //newValue = packet.readString(readIndex, this.length);
        } else if (this.type == Type.FLAG) {
            //newValue = packet.readFlag(readIndex, 7 - this.length) ? 1 : 0;
        } else if (this.type == Type.NUMBER_LITTLE_ENDIAN) {
            //newValue = packet.readNumberLittleEndian(readIndex, this.length);
        } else {
            //newValue = packet.readNumber(readIndex, this.length);
        }

        //this.change(newValue);
    }

}
