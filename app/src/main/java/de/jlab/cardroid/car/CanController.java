package de.jlab.cardroid.car;

import android.util.SparseArray;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.VariableController;

public final class CanController {

    private DeviceController deviceController;
    private HashMap<DeviceUid, CanConfig> canConfigs = new HashMap<>();

    public CanController(@NonNull DeviceController deviceController, @NonNull VariableController variableStore, @NonNull ScriptEngine scriptEngine) {
        this.deviceController = deviceController;

        // load variables from database
        this.loadVariables(variableStore, scriptEngine);
    }

    public void dispose() {
        // register all CanPacketDescriptors with VariableStore
        for (CanConfig config : this.canConfigs.values()) {
            config.dispose(this.deviceController);
        }
    }

    private void addCanValues(int canId, @Nullable DeviceUid deviceUid, @NonNull CanValue... values) {
        CanConfig config = this.canConfigs.get(deviceUid);
        if (config == null) {
            config = new CanConfig(deviceUid, this.deviceController);
            this.canConfigs.put(deviceUid, config);
        }

        config.addCanValues(canId, values);
    }

    private void loadVariables(@NonNull VariableController variableStore, @NonNull ScriptEngine scriptEngine) {
        // TODO: read can variables from database
        this.addCanValues(0x002, null,
                new CanValue("steeringWheelAngle", "(value - (max / 2)) / 10",
                        0, 16, CanValue.DataType.LITTLE_ENDIAN, 0)
        );

        this.addCanValues(0x180, null,
                new CanValue("rpm", "value / 10",
                        0, 16, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("gasPedalPosition", "value / 8",
                        40, 10, CanValue.DataType.BIG_ENDIAN, 0)
        );

        this.addCanValues(0x280, null,
                new CanValue("speed", "value / 100",
                        32, 16, CanValue.DataType.BIG_ENDIAN, 0)
        );

        this.addCanValues(0x35D, null,
                new CanValue("wipersMoving", null,
                        16, 1, CanValue.DataType.FLAG, 0),
                new CanValue("wipersOn", null,
                        17, 1, CanValue.DataType.FLAG, 0),
                new CanValue("wipersFast", null,
                        18, 1, CanValue.DataType.FLAG, 0)
        );

        this.addCanValues(0x421, null,
                new CanValue("gear", "value == 16 ? \"R\" : (value == 24 ? \"N\" : ((value - 120) / 8))",
                        0, 8, CanValue.DataType.BIG_ENDIAN, 24),
                new CanValue("synchroRev", null,
                        8, 1, CanValue.DataType.FLAG, 0)
        );

        this.addCanValues(0x54A, null,
                new CanValue("hvacTargetTemperature", "value / 2",
                        32, 8, CanValue.DataType.BIG_ENDIAN, 32)
        );

        this.addCanValues(0x54B, null,
                new CanValue("hvacIsAutomatic", null,
                        14, 1, CanValue.DataType.FLAG, 0),
                new CanValue("hvacIsAirductWindshield", "value == 0xA0 || value == 0xA8 ? 1 : 0",
                        16, 8, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("hvacIsAirductFace", "value == 0x88 || value == 0x90 ? 1 : 0",
                        16, 8, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("hvacIsAirductFeet", "value == 0x90 || value == 0x98 || value == 0xA0 ? 1 : 0",
                        16, 8, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("hvacIsWindshieldHeating", null,
                        24, 1, CanValue.DataType.FLAG, 0),
                new CanValue("hvacIsRecirculation", null,
                        31, 1, CanValue.DataType.FLAG, 0),
                new CanValue("hvacFanLevel", "(value - 4) / 8",
                        32, 8, CanValue.DataType.BIG_ENDIAN, 0)

        );

        this.addCanValues(0x54C, null,
                new CanValue("hvacEvaporatorTemperature", null,
                        0, 8, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("hvacEvaporatorTargetTemperature", null,
                        8, 8, CanValue.DataType.BIG_ENDIAN, 0),
                // TODO might need a special flag rule with mask and comparison value since multiple bits might be involved
                new CanValue("hvacIsAcOn", null,
                        16, 1, CanValue.DataType.FLAG, 0)
        );

        this.addCanValues(0x5C5, null,
                new CanValue("hazardLights", "value > 0 ? 1 : 0",
                        0, 2, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("parkingBreakWarning", null,
                        5, 1, CanValue.DataType.FLAG, 0),
                new CanValue("odometer", null,
                        8, 24, CanValue.DataType.BIG_ENDIAN, 0)
        );

        this.addCanValues(0x625, null,
                new CanValue("hvacIsRearWindowHeating", null,
                        7, 1, CanValue.DataType.FLAG, 0),
                new CanValue("runningLights", null,
                        9, 1, CanValue.DataType.FLAG, 0),
                new CanValue("lowBeamLights", null,
                        10, 1, CanValue.DataType.FLAG, 0),
                new CanValue("highBeamLights", null,
                        11, 1, CanValue.DataType.FLAG, 0)
        );

        this.addCanValues(0x551, null,
                new CanValue("coolantTemperature", "value - 48",
                        0, 8, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("cruiseControlSpeed", "value >= 254 ? 0 : value",
                        32, 8, CanValue.DataType.BIG_ENDIAN, 0),
                new CanValue("cruiseControlMaster", null,
                        41, 1, CanValue.DataType.FLAG, 0),
                new CanValue("cruiseControlActive", null,
                        43, 1, CanValue.DataType.FLAG, 0)
        );

        this.addCanValues(0x580, null,
                new CanValue("oilTemperature", "value - 52",
                        32, 8, CanValue.DataType.BIG_ENDIAN, 0)
        );

        this.addCanValues(0x60D, null,
                new CanValue("driverDoor", null,
                        4, 1, CanValue.DataType.FLAG, 0),
                new CanValue("passengerDoor", null,
                        3, 1, CanValue.DataType.FLAG, 0),
                new CanValue("rightTurnSignal", null,
                        9, 1, CanValue.DataType.FLAG, 0),
                new CanValue("leftTurnSignal", null,
                        10, 1, CanValue.DataType.FLAG, 0),
                new CanValue("ignition", null,
                        13, 1, CanValue.DataType.FLAG, 0),
                new CanValue("acc", null,
                        14, 1, CanValue.DataType.FLAG, 0)
        );

        // register all CanPacketDescriptors with VariableStore
        for (CanConfig config : this.canConfigs.values()) {
            config.registerVariables(variableStore, scriptEngine);
        }
    }

    private static class CanConfig {
        // FIXME CanConfig creates a lot of Observers. Maybe we can reduce this in a smart way?
        private Device.FeatureChangeObserver<CanInteractable> sendFilter;
        private Device.FeatureChangeObserver<CanObservable> receiveFilter;

        private SparseArray<CanPacketDescriptor> packetDescriptors = new SparseArray<>();

        public CanConfig(@Nullable DeviceUid deviceUid, @NonNull DeviceController deviceController) {
            // Register feature filters for deviceUid in DeviceController
            this.sendFilter = this::interactableStateChange;
            this.receiveFilter = this::observableStateChange;
            deviceController.subscribeFeature(this.receiveFilter, CanObservable.class, deviceUid);
            deviceController.subscribeFeature(this.sendFilter, CanInteractable.class, deviceUid);
        }

        public void addCanValues(int canId, @NonNull CanValue... values) {
            // find existing descriptor based on canId
            CanPacketDescriptor descriptor = this.packetDescriptors.get(canId);
            if (descriptor == null) {
                descriptor = new CanPacketDescriptor(canId);
                this.packetDescriptors.put(canId, descriptor);
            }

            // add the provided can values to the descriptor
            for (CanValue value : values) {
                descriptor.addCanValue(value);
            }
        }

        public void registerVariables(@NonNull VariableController variableStore, @NonNull ScriptEngine scriptEngine) {
            for(int i = 0; i < this.packetDescriptors.size(); i++) {
                this.packetDescriptors.valueAt(i).registerVariables(variableStore, scriptEngine);
            }
        }

        public void dispose(@NonNull DeviceController deviceController) {
            deviceController.unsubscribeFeature(this.sendFilter, CanInteractable.class);
            deviceController.unsubscribeFeature(this.receiveFilter, CanObservable.class);
        }

        private void interactableStateChange(@NonNull CanInteractable interactable, @NonNull Feature.State state) {
            if (state == Feature.State.AVAILABLE) {
                for (int i = 0; i < this.packetDescriptors.size(); i++) {
                    CanPacketDescriptor descriptor = this.packetDescriptors.valueAt(i);
                /*
                StringBuilder sb = new StringBuilder("Register can id " + String.format("%04X", this.packetDescriptors.valueAt(i).getCanId()) + ": ");
                for (int y = 0; y < 8; y++) {
                    sb.append((descriptor.getByteMask() >> (7 - y)) & 0x01);
                }
                Log.e(this.getClass().getSimpleName(), sb.toString());
                 */
                    interactable.registerCanId(descriptor);
                }
            } else {
                for(int i = 0; i < this.packetDescriptors.size(); i++) {
                    interactable.unregisterCanId(this.packetDescriptors.valueAt(i));
                }
            }
        }

        private void observableStateChange(@NonNull CanObservable observable, @NonNull Feature.State state) {
            if (state == Feature.State.AVAILABLE) {
                for (int i = 0; i < this.packetDescriptors.size(); i++) {
                    observable.addListener(this.packetDescriptors.valueAt(i));
                }
            } else {
                for(int i = 0; i < this.packetDescriptors.size(); i++) {
                    observable.removeListener(this.packetDescriptors.valueAt(i));
                }
            }
        }
    }

}
