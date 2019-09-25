package de.jlab.cardroid.car;

import android.util.Log;
import android.util.LongSparseArray;

import androidx.annotation.NonNull;
import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.usb.carduino.serial.SerialCanPacket;
import de.jlab.cardroid.usb.carduino.serial.SerialPacket;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableStore;

public class Car implements CarduinoService.PacketHandler<SerialCanPacket> {

    private LongSparseArray<CarDataParser> data = new LongSparseArray<>();
    private CarduinoService carduino;
    private VariableStore variableStore;

    public Car(CarduinoService carduino, VariableStore variableStore) {
        this.carduino = carduino;
        this.variableStore = variableStore;
    }

    public void initVariables(ScriptEngine scriptEngine) {
        // TODO load cardata definitions from database

        registerCarData(0x002, CarDataParser.build()
                .addVariable("steeringWheelAngle", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.NUMBER_LITTLE_ENDIAN, 2, 0)
                        .setExpression("value / 10", scriptEngine))
                .get());

        // TODO Needs a custom parse rule, since only bit 0 and 1 of the second byte are used
        //this.addVariable(0x160, "gasPedalPosition", "value / 8", 0, CarData.Type.NUMBER_BIG_ENDIAN, 2, scriptEngine);

        registerCarData(0x180, CarDataParser.build()
                .addVariable("rpm", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.NUMBER_BIG_ENDIAN, 2, 0)
                        .setExpression("value / 10", scriptEngine))
                .get());

        registerCarData(0x280, CarDataParser.build()
                .addVariable("speed", new CarDataParser.VariableBuilder()
                        .parse(4, CarData.Type.NUMBER_BIG_ENDIAN, 2, 0)
                        .setExpression("value / 100", scriptEngine))
                .get());

        registerCarData(0x35D, CarDataParser.build()
                .addVariable("wipersContinuous", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.FLAG, 0, 0))
                .addVariable("wipersOn", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.FLAG, 1, 0))
                .addVariable("wipersFast", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.FLAG, 2, 0))
                .get());

        registerCarData(0x421, CarDataParser.build()
                .addVariable("gear", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.NUMBER_BIG_ENDIAN, 1, 24)
                        .setExpression("value == 16 ? \"R\" : (value == 24 ? \"N\" : ((value - 120) / 8))", scriptEngine))
                .addVariable("synchroRev", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 1, 0))
                .get());

        registerCarData(0x54A, CarDataParser.build()
                .addVariable("hvacTargetTemperature", new CarDataParser.VariableBuilder()
                        .parse(4, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value / 2", scriptEngine))
                .get());

        registerCarData(0x54B, CarDataParser.build()
                .addVariable("hvacIsAutomatic", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 6, 0))
                .addVariable("hvacIsAirductWindshield", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value == 0xA0 || value == 0xA8", scriptEngine))
                .addVariable("hvacIsAirductFace", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value == 0x88 || value == 0x90", scriptEngine))
                .addVariable("hvacIsAirductFeet", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value == 0x90 || value == 0x98 || value == 0xA0", scriptEngine))
                .addVariable("hvacIsWindshieldHeating", new CarDataParser.VariableBuilder()
                        .parse(3, CarData.Type.FLAG, 0, 0))
                .addVariable("hvacIsRecirculation", new CarDataParser.VariableBuilder()
                        .parse(3, CarData.Type.FLAG, 7, 0))
                .addVariable("hvacFanLevel", new CarDataParser.VariableBuilder()
                        .parse(4, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("(value - 4) / 8", scriptEngine))
                .get());

        registerCarData(0x54C, CarDataParser.build()
                .addVariable("hvacEvaporatorTemperature", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0))
                .addVariable("hvacEvaporatorTargetTemperature", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0))
                // TODO might need a special flag rule with mask and comparison value since multiple bits might be involved
                .addVariable("hvacIsAcOn", new CarDataParser.VariableBuilder()
                        .parse(2, CarData.Type.FLAG, 0, 0))
                .get());

        registerCarData(0x5C5, CarDataParser.build()
                .addVariable("hazardLights", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.FLAG, 1, 0))
                .addVariable("parkingBreakWarning", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.FLAG, 5, 0))
                .addVariable("odometer", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.NUMBER_BIG_ENDIAN, 3, 0))
                .get());

        registerCarData(0x625, CarDataParser.build()
                .addVariable("hvacIsRearWindowHeating", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.FLAG, 7, 0))
                .addVariable("runningLights", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 1, 0))
                .addVariable("lowBeamLights", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 2, 0))
                .addVariable("highBeamLights", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 3, 0))
                .get());

        registerCarData(0x551, CarDataParser.build()
                .addVariable("coolantTemperature", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value - 48", scriptEngine))
                .addVariable("cruiseControlSpeed", new CarDataParser.VariableBuilder()
                        .parse(4, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value >= 254 ? 0 : value", scriptEngine))
                .addVariable("cruiseControlMaster", new CarDataParser.VariableBuilder()
                        .parse(6, CarData.Type.FLAG, 1, 0))
                .addVariable("cruiseControlActive", new CarDataParser.VariableBuilder()
                        .parse(6, CarData.Type.FLAG, 3, 0))
                .get());

        registerCarData(0x580, CarDataParser.build()
                .addVariable("oilTemperature", new CarDataParser.VariableBuilder()
                        .parse(4, CarData.Type.NUMBER_BIG_ENDIAN, 1, 0)
                        .setExpression("value - 52", scriptEngine))
                .get());

        registerCarData(0x60D, CarDataParser.build()
                .addVariable("driverDoor", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.FLAG, 4, 0))
                .addVariable("passengerDoor", new CarDataParser.VariableBuilder()
                        .parse(0, CarData.Type.FLAG, 3, 0))
                .addVariable("rightTurnSignal", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 1, 0))
                .addVariable("leftTurnSignal", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 2, 0))
                .addVariable("ignition", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 5, 0))
                .addVariable("acc", new CarDataParser.VariableBuilder()
                        .parse(1, CarData.Type.FLAG, 6, 0))
                .get());
    }

    private void registerCarData(int canId, CarDataParser parser) {
        this.data.put(canId, parser);
        this.carduino.enableCarData(canId, parser.getByteMask());
        Variable[] variables = parser.getVariables();
        for (Variable variable : variables) {
            this.variableStore.registerVariable(variable);
        }
    }

    @Override
    public void handleSerialPacket(@NonNull SerialCanPacket packet) {
        Log.e("CarData", packet.payloadAsHexString());
        long packetCanId = packet.getCanId();
        CarDataParser data = this.data.get(packetCanId);
        if (data != null) {
            data.updateFromSerialPacket(packet);
        } else {
            Log.w("CarData", "Can id \"" + packetCanId + "\" has no parser!");
        }
    }

    @Override
    public boolean shouldHandlePacket(@NonNull SerialPacket packet) {
        return packet instanceof SerialCanPacket;
    }

    public long[] getUsedCanIds() {
        int length = this.data.size();
        long[] canIds = new long[length];
        for (int i = 0; i < length; i++) {
            canIds[i] = this.data.keyAt(i);
        }
        return canIds;
    }

}
