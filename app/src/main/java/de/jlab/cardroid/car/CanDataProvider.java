package de.jlab.cardroid.car;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.DeviceDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.can.CanDeviceHandler;

public final class CanDataProvider extends DeviceDataProvider<CanDeviceHandler> {

    private ArrayList<CanDeviceHandler.CanPacketListener> externalListeners = new ArrayList<>();
    private CanDeviceHandler.CanPacketListener listener = packet -> {
        for (int i = 0; i < this.externalListeners.size(); i++) {
            this.externalListeners.get(i).onReceive(packet);
        }
    };

    public CanDataProvider(@NonNull DeviceService service) {
        super(service);
    }

    public void addExternalListener(@NonNull CanDeviceHandler.CanPacketListener externalListener) {
        this.externalListeners.add(externalListener);
    }

    public void removeExternalListener(@NonNull CanDeviceHandler.CanPacketListener externalListener) {
        this.externalListeners.remove(externalListener);
    }

    // FIXME: App has to be able to request can data on carduino devices
    public void requestCanData(int packetId, byte mask) {
        /*
        byte[] payload = ByteBuffer.allocate(5).putInt(packetId).put(mask).array();
        MetaSerialPacket packet = MetaEvent.serialize(MetaEvent.CAR_DATA_DEFINITION, payload);
        this.send(packet);
         */
    }

    public void startCanSniffer() {
        //this.send(MetaEvent.serialize(MetaEvent.START_SNIFFING, null));
    }

    public void stopCanSniffer() {
        //this.send(MetaEvent.serialize(MetaEvent.STOP_SNIFFING, null));
    }

    @Override
    protected void onUpdate(@NonNull CanDeviceHandler previousDevice, @NonNull CanDeviceHandler newDevice, @NonNull DeviceService service) {
        previousDevice.removeCanListener(this.listener);
        newDevice.addCanListener(this.listener);
    }

    @Override
    protected void onStop(@NonNull CanDeviceHandler device, @NonNull DeviceService service) {
        device.removeCanListener(this.listener);
    }

    @Override
    protected void onStart(@NonNull CanDeviceHandler device, @NonNull DeviceService service) {
        device.addCanListener(this.listener);
    }

}
