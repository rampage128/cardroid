package de.jlab.cardroid.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class DeviceConnectionRequest {

    private Long waitingSince;
    private DeviceUid predictedDeviceUid;
    private Device device;

    public DeviceConnectionRequest(@NonNull Device device, @Nullable DeviceUid predictedDeviceUid) {
        this.device = device;
        this.predictedDeviceUid = predictedDeviceUid;
        this.waitingSince = System.nanoTime();
    }

    public boolean hasIdentity() {
        return this.predictedDeviceUid != null;
    }

    public boolean shouldYieldFor(@NonNull Device otherDevice) {
        return this.predictedDeviceUid != null &&
                this.device.getClass().equals(otherDevice.getClass()) &&
                otherDevice.isDevice(this.predictedDeviceUid);
    }

    public boolean hasTimedOut(long timeout) {
        return System.nanoTime() - waitingSince > timeout * 1000000;
    }

    @NonNull
    public Device getDevice() {
        return this.device;
    }

    @NonNull
    @Override
    public String toString() {
        return "!" + this.predictedDeviceUid + "@" + this.device.getConnectionId();
    }
}
