package de.jlab.cardroid.car;

import android.util.Log;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.serial.can.CanPacket;
import de.jlab.cardroid.variables.ObservableValue;
import de.jlab.cardroid.variables.ObservableValueBag;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.Variable;

/**
 * @deprecated This has to be rewritten with the new CanPacket architecture in mind
 */
@Deprecated
public class CarDataParser {

    private ArrayList<Integer> byteIndexMap = new ArrayList<>();
    private ArrayList<CarVariable> variables = new ArrayList<>();

    private byte byteMask = 0x00;
    private int byteCount = 0;

    private void addVariable(String name, VariableBuilder builder) {
        int byteIndex = builder.getByteIndex();
        if (!byteIndexMap.contains(byteIndex)) {
            this.byteIndexMap.add(byteIndex);
        }
        this.variables.add(builder.get(name));
        for (int i = builder.getByteIndex(); i < builder.getByteIndex() + builder.getLength(); i++) {
            this.byteMask |= 1 << (7 - i);
        }
    }

    private void addBytes(int byteCount) {
        this.byteCount += byteCount;
    }

    public void updateFromSerialPacket(@NonNull CanPacket packet) {
        for (int i = 0; i < this.variables.size(); i++) {
            this.variables.get(i).updateFromSerialPacket(packet, this.byteCount, this.byteIndexMap);
        }
    }

    public byte getByteMask() {
        return this.byteMask;
    }

    public Variable[] getVariables() {
        return this.variables.toArray(new Variable[0]);
    }

    // TODO return if any of the variables are observed. This can then be used by Car to tell carduino to stop or start broadcasting this canId
    public boolean isObserved() {
        return true;
    }

    public static ParserBuilder build() {
        return new ParserBuilder();
    }

    public static class ParserBuilder {
        private CarDataParser parser;
        private ArrayList<Integer> byteList = new ArrayList<>();

        public ParserBuilder() {
            this.parser = new CarDataParser();
        }

        public ParserBuilder addVariable(@NonNull String name, @NonNull VariableBuilder builder) {
            int byteIndex = builder.getByteIndex();
            if (!this.byteList.contains(byteIndex)) {
                this.parser.addBytes(builder.getLength());
            }
            this.byteList.add(byteIndex);
            this.parser.addVariable(name, builder);
            return this;
        }

        public CarDataParser get() {
            return this.parser;
        }
    }

    private static class CarVariable extends Variable {
        private CarData data;

        public CarVariable(@NonNull String name, @NonNull ObservableValue value, @NonNull CarData data) {
            super(name, value);
            this.data = data;
        }

        public void updateFromSerialPacket(@NonNull CanPacket packet, int expectedSize, @NonNull ArrayList<Integer> byteIndexMap) {
            int offset = 0;
            int byteIndex = this.data.getByteIndex();
            if (packet.getDataLength() == expectedSize + 4) {
                int trueIndex = byteIndexMap.indexOf(byteIndex);
                offset = trueIndex - byteIndex;
            }
            try {
                this.data.updateFromSerialPacket(packet, offset + 4);
            } catch (Exception e) {
                String eB = "Error parsing packet \"" + String.format("%02x", packet.getCanId()) + "\"" +
                        " (offset: " + (offset + 4) +
                        ", expected size: " + expectedSize +
                        ", realSize: " + packet.getDataLength() +
                        ")";
                Log.e(this.getClass().getSimpleName(), eB, e);
            }
        }
    }

    public static class VariableBuilder {
        private int byteIndex = 0;
        private CarData.Type type = null;
        private int length = 0;
        private Object defaultValue = null;

        private String expressionString = null;
        private ScriptEngine scriptEngine = null;

        public VariableBuilder parse(int byteIndex, @NonNull CarData.Type type, int length, @NonNull Object defaultValue) {
            this.byteIndex = byteIndex;
            this.type = type;
            this.length = length;
            this.defaultValue = defaultValue;
            return this;
        }

        public VariableBuilder setExpression(@NonNull String expressionString, @NonNull ScriptEngine engine) {
            this.expressionString = expressionString;
            this.scriptEngine = engine;
            return this;
        }

        public int getByteIndex() {
            return this.byteIndex;
        }

        public int getLength() {
            int length = this.length;
            if (type == CarData.Type.FLAG) {
                length = 1;
            }
            return length;
        }

        public CarVariable get(@NonNull String name) {
            if (this.type == null) {
                throw new IllegalStateException("Can not get variable from builder, did you call parse()?");
            }

            CarData data = new CarData(this.byteIndex, this.type, this.length, this.defaultValue);
            ObservableValue targetValue = data;
            if (this.expressionString != null && this.expressionString.trim().length() > 0 && !this.expressionString.trim().equals("value")) {
                ObservableValueBag variables = ObservableValueBag.build().addVariable("value", data).get();
                targetValue = this.scriptEngine.createExpression(this.expressionString, variables, name);
            }

            return new CarVariable(name, targetValue, data);
        }

    }

}
