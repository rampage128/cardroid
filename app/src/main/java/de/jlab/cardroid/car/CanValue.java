package de.jlab.cardroid.car;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.variables.ObservableValue;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableStore;

public final class CanValue extends ObservableValue {

    public enum DataType {
        BIG_ENDIAN((bitIndex, bitLength, packet) -> packet.readBigEndian(bitIndex, bitLength)),
        LITTLE_ENDIAN((bitIndex, bitLength, packet) -> packet.readLittleEndian(bitIndex, bitLength)),
        STRING((bitIndex, bitLength, packet) -> new String(packet.readBytes(bitIndex, bitLength))),
        FLAG((bitIndex, bitLength, packet) -> packet.readFlag(bitIndex)),
        BYTES((bitIndex, bitLength, packet) -> packet.readBytes(bitIndex, bitLength));

        private DataTypeReader reader;

        DataType(DataTypeReader reader) {
            this.reader = reader;
        }

        public Object read(int bitIndex, int bitLength, CanPacket packet) {
            return this.reader.read(bitIndex, bitLength, packet);
        }

        private interface DataTypeReader {
            Object read(int bitIndex, int bitLength, CanPacket packet);
        }
    }

    private DataType type;
    private int bitIndex;
    private int bitLength;
    private long maxValue;
    private String name;
    private String expression;

    public CanValue(@NonNull String name, @Nullable String expression, int bitIndex, int bitLength, @NonNull DataType type, long defaultValue) {
        super(defaultValue);

        this.name = name;
        this.expression = expression;
        this.bitIndex = bitIndex;
        this.bitLength = bitLength;
        this.type = type;
        this.maxValue = (long)Math.pow(2, this.bitLength) - 1;
    }

    public void register(@NonNull VariableStore variableStore, @NonNull ScriptEngine scriptEngine) {
        if (this.expression != null && this.expression.trim().length() > 0 && !this.expression.trim().equals("value")) {
            variableStore.registerVariable(Variable.createFromExpression(this.name, this.expression, this, new ObservableValue(this.maxValue), scriptEngine));
        } else {
            variableStore.registerVariable(Variable.createPlain(this.name, this));
        }
    }

    public long getMaxValue() {
        return this.maxValue;
    }

    public int getBitIndex() {
        return this.bitIndex;
    }

    public int getBitLength() {
        return this.bitLength;
    }

    public String getName() {
        return this.name;
    }

    public void updateFromCanPacket(CanPacket packet, int offset) {
        Object newValue = this.type.read(this.bitIndex + offset, this.bitLength, packet);
        this.change(newValue);
    }

}
