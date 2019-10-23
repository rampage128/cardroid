package de.jlab.cardroid.car.nissan370z;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.jlab.cardroid.car.CanInteractable;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.CanPacket;
import de.jlab.cardroid.car.CanPacketDescriptor;
import de.jlab.cardroid.car.CanValue;
import de.jlab.cardroid.variables.ObservableValue;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableStore;

public class CarCanController {

    private VariableStore variableStore = new VariableStore();
    private ScriptEngine scriptEngine = new ScriptEngine();
    private ArrayList<CanPacketDescriptor> packetDescriptors = new ArrayList<>();
    private CanInteractable interactable;
    private CanObservable observable;
    private ArrayList<CarCanControllerListener> listeners = new ArrayList<>();

    private CanObservable.CanPacketListener listener = packet -> {
        for (int i = 0; i < this.packetDescriptors.size(); i++) {
            CanPacketDescriptor descriptor = this.packetDescriptors.get(i);
            if (descriptor.getCanId() == packet.getCanId()) {
                descriptor.onReceive(packet);
            }
        }
        for (CarCanControllerListener listener: CarCanController.this.listeners) {
            listener.onVariablesUpdated(packet);
        }
    };

    public CarCanController(CanInteractable interactable, CanObservable observable) {
        this.interactable = interactable;
        this.observable = observable;
        this.observable.addListener(this.listener);

        CanPacketDescriptor descriptor = new CanPacketDescriptor(0x002);
        descriptor.addCanValue(this.registerCanVariable(
                "steeringWheelAngle", "(value - (max / 2)) / 10",
                new CanValue(0, 16, CanValue.DataType.LITTLE_ENDIAN, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x180);
        descriptor.addCanValue(this.registerCanVariable(
                "rpm", "value / 10",
                new CanValue(0, 16, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "gasPedalPosition", "value / 8",
                new CanValue(40, 10, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x280);
        descriptor.addCanValue(this.registerCanVariable(
                "speed", "value / 100",
                new CanValue(32, 16, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x35D);
        descriptor.addCanValue(this.registerCanVariable(
                "wipersMoving", null,
                new CanValue(16, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "wipersOn", null,
                new CanValue(17, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "wipersFast", null,
                new CanValue(18, 1, CanValue.DataType.FLAG, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x421);
        descriptor.addCanValue(this.registerCanVariable(
                "gear", "value == 16 ? \"R\" : (value == 24 ? \"N\" : ((value - 120) / 8))",
                new CanValue(0, 8, CanValue.DataType.BIG_ENDIAN, 24)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "synchroRev", null,
                new CanValue(8, 1, CanValue.DataType.FLAG, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x54A);
        descriptor.addCanValue(this.registerCanVariable(
                "hvacTargetTemperature", "value / 2",
                new CanValue(32, 8, CanValue.DataType.BIG_ENDIAN, 32)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x54B);
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAutomatic", null,
                new CanValue(14, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAirductWindshield", "value == 0xA0 || value == 0xA8 ? 1 : 0",
                new CanValue(16, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAirductFace", "value == 0x88 || value == 0x90 ? 1 : 0",
                new CanValue(16, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAirductFeet", "value == 0x90 || value == 0x98 || value == 0xA0 ? 1 : 0",
                new CanValue(16, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsWindshieldHeating", null,
                new CanValue(24, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsRecirculation", null,
                new CanValue(31, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacFanLevel", "(value - 4) / 8",
                new CanValue(32, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x54C);
        descriptor.addCanValue(this.registerCanVariable(
                "hvacEvaporatorTemperature", null,
                new CanValue(0, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacEvaporatorTargetTemperature", null,
                new CanValue(8, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        // TODO might need a special flag rule with mask and comparison value since multiple bits might be involved
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAcOn", null,
                new CanValue(16, 1, CanValue.DataType.FLAG, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x5C5);
        descriptor.addCanValue(this.registerCanVariable(
                "hazardLights", "value > 0 ? 1 : 0",
                new CanValue(0, 2, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "parkingBreakWarning", null,
                new CanValue(5, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "odometer", null,
                new CanValue(8, 24, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x625);
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsRearWindowHeating", null,
                new CanValue(7, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "runningLights", null,
                new CanValue(9, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "lowBeamLights", null,
                new CanValue(10, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "highBeamLights", null,
                new CanValue(11, 1, CanValue.DataType.FLAG, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x551);
        descriptor.addCanValue(this.registerCanVariable(
                "coolantTemperature", "value - 48",
                new CanValue(0, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "cruiseControlSpeed", "value >= 254 ? 0 : value",
                new CanValue(32, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "cruiseControlMaster", null,
                new CanValue(41, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "cruiseControlActive", null,
                new CanValue(43, 1, CanValue.DataType.FLAG, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x580);
        descriptor.addCanValue(this.registerCanVariable(
                "oilTemperature", "value - 52",
                new CanValue(32, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        this.subscribeCanId(descriptor);

        descriptor = new CanPacketDescriptor(0x60D);
        descriptor.addCanValue(this.registerCanVariable(
                "driverDoor", null,
                new CanValue(4, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "passengerDoor", null,
                new CanValue(3, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "rightTurnSignal", null,
                new CanValue(9, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "leftTurnSignal", null,
                new CanValue(10, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "ignition", null,
                new CanValue(13, 1, CanValue.DataType.FLAG, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "acc", null,
                new CanValue(14, 1, CanValue.DataType.FLAG, 0)
        ));
        this.subscribeCanId(descriptor);
    }

    public void dispose() {
        for (int i = 0; i < this.packetDescriptors.size(); i++) {
            this.unregisterCanId(this.packetDescriptors.get(i));
        }
        observable.removeListener(this.listener);
    }

    public void monitorVariable(String name, Variable.VariableChangeListener listener) {
        this.variableStore.subscribe(name, listener);
    }

    public void stopMonitoringVariable(String name, Variable.VariableChangeListener listener) {
        this.variableStore.unsubscribe(name, listener);
    }

    private void subscribeCanId(CanPacketDescriptor descriptor) {
        this.packetDescriptors.add(descriptor);
        this.registerCanId(descriptor);
    }

    private void registerCanId(CanPacketDescriptor descriptor) {
        if (this.interactable != null) {
            this.interactable.registerCanId(descriptor);
        }
    }

    private void unsubscribeCanId(CanPacketDescriptor descriptor) {
        this.packetDescriptors.remove(descriptor);
        this.unregisterCanId(descriptor);
    }

    private void unregisterCanId(CanPacketDescriptor descriptor) {
        if (this.interactable != null) {
            this.interactable.unregisterCanId(descriptor);
        }
    }

    private CanValue registerCanVariable(@NonNull String variableName, @Nullable String expression, @NonNull CanValue value) {
        if (expression != null && expression.trim().length() > 0 && !expression.trim().equals("value")) {
            this.variableStore.registerVariable(Variable.createFromExpression(variableName, expression, value, new ObservableValue(value.getMaxValue()), this.scriptEngine));
        } else {
            this.variableStore.registerVariable(Variable.createPlain(variableName, value));
        }

        return value;
    }

    public VariableStore getVariableStore() {
        return this.variableStore;
    }

    public void addListener(CarCanControllerListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(CarCanControllerListener listener) {
        this.listeners.remove(listener);
    }

    public interface CarCanControllerListener {
        void onVariablesUpdated(CanPacket lastPacketReceived);
    }

}
