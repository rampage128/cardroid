package de.jlab.cardroid.car;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.variables.ObservableValue;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.Variable;

public final class CanDataProvider extends DeviceDataProvider {

    private ArrayList<CanObservable.CanPacketListener> externalListeners = new ArrayList<>();
    private ArrayList<CanPacketDescriptor> packetDescriptors = new ArrayList<>();
    private boolean isSniffing = false;

    private DeviceService service;

    private CanObservable.CanPacketListener listener = packet -> {
        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onReceive(packet);
        }
        for (int i = 0; i < this.packetDescriptors.size(); i++) {
            CanPacketDescriptor descriptor = this.packetDescriptors.get(i);
            if (descriptor.getCanId() == packet.getCanId()) {
                descriptor.onReceive(packet);
            }
        }
    };

    public CanDataProvider(@NonNull DeviceService service) {
        super(service);
        this.service = service;

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
                "hvacIsAirductWindshield", "value == 0xA0 || value == 0xA8",
                new CanValue(16, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAirductFace", "value == 0x88 || value == 0x90",
                new CanValue(16, 8, CanValue.DataType.BIG_ENDIAN, 0)
        ));
        descriptor.addCanValue(this.registerCanVariable(
                "hvacIsAirductFeet", "value == 0x90 || value == 0x98 || value == 0xA0",
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

    private CanValue registerCanVariable(@NonNull String variableName, @Nullable String expression, @NonNull CanValue value) {
        if (expression != null && expression.trim().length() > 0 && !expression.trim().equals("value")) {
            ScriptEngine engine = this.service.getScriptEngine();
            this.service.getVariableStore().registerVariable(Variable.createFromExpression(variableName, expression, value, new ObservableValue(value.getMaxValue()), engine));
        } else {
            this.service.getVariableStore().registerVariable(Variable.createPlain(variableName, value));
        }

        return value;
    }

    public void addExternalListener(@NonNull CanObservable.CanPacketListener externalListener) {
        this.externalListeners.add(externalListener);
    }

    public void removeExternalListener(@NonNull CanObservable.CanPacketListener externalListener) {
        this.externalListeners.remove(externalListener);
    }

    public void subscribeCanId(CanPacketDescriptor descriptor) {
        this.packetDescriptors.add(descriptor);
        this.registerCanId(descriptor);
    }

    private void registerCanId(CanPacketDescriptor descriptor) {
        ArrayList<DeviceHandler> devices = this.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            CanInteractable interactable = devices.get(i).getInteractable(CanInteractable.class);
            if (interactable != null) {
                interactable.registerCanId(descriptor);
            }
        }
    }

    public void unsubscribeCanId(CanPacketDescriptor descriptor) {
        this.packetDescriptors.remove(descriptor);
        this.unregisterCanId(descriptor);
    }

    private void unregisterCanId(CanPacketDescriptor descriptor) {
        ArrayList<DeviceHandler> devices = this.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            CanInteractable interactable = devices.get(i).getInteractable(CanInteractable.class);
            if (interactable != null) {
                interactable.unregisterCanId(descriptor);
            }
        }
    }

    public void startCanSniffer() {
        this.isSniffing = true;
        ArrayList<DeviceHandler> devices = this.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            CanInteractable interactable = devices.get(i).getInteractable(CanInteractable.class);
            if (interactable != null) {
                interactable.startSniffer();
            }
        }
    }

    public void stopCanSniffer() {
        this.isSniffing = false;
        ArrayList<DeviceHandler> devices = this.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            CanInteractable interactable = devices.get(i).getInteractable(CanInteractable.class);
            if (interactable != null) {
                interactable.stopSniffer();
            }
        }
    }

    private void deviceRemoved(DeviceHandler device) {
        for (int i = 0; i < this.packetDescriptors.size(); i++) {
            this.unregisterCanId(this.packetDescriptors.get(i));
        }
        CanObservable observable = device.getObservable(CanObservable.class);
        if (observable != null) {
            observable.removeCanListener(this.listener);
        }
    }

    private void deviceAdded(DeviceHandler device) {
        CanObservable observable = device.getObservable(CanObservable.class);
        if (observable != null) {
            observable.addCanListener(this.listener);
        }

        for (int i = 0; i < this.packetDescriptors.size(); i++) {
            this.registerCanId(this.packetDescriptors.get(i));
        }
        if (this.isSniffing) {
            ArrayList<DeviceHandler> devices = this.getDevices();
            for (int i = 0; i < devices.size(); i++) {
                CanInteractable interactable = devices.get(i).getInteractable(CanInteractable.class);
                if (interactable != null) {
                    interactable.startSniffer();
                }
            }
        }
    }

    @Override
    protected void onUpdate(@NonNull DeviceHandler previousDevice, @NonNull DeviceHandler newDevice, @NonNull DeviceService service) {
        this.deviceRemoved(previousDevice);
        this.deviceAdded(newDevice);
    }

    @Override
    protected void onStop(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceRemoved(device);
        if (this.getConnectedDeviceCount() < 1) {
            this.service.hideOverlay();
        }
    }

    @Override
    protected void onStart(@NonNull DeviceHandler device, @NonNull DeviceService service) {
        this.deviceAdded(device);
        this.service.showOverlay();
    }

}
