package de.jlab.cardroid.devices;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDevice;
import de.jlab.cardroid.devices.usb.serial.gps.GpsUsbDevice;

public enum DeviceType {
    UNKNOWN_DEVICE(Device.class.getSimpleName(), R.string.device_type_unknown, R.drawable.ic_device_type_unknown),
    // TODO: This is not very nice. It would be way cooler if we could resolve device icon/name more specifically for carduino devices
    CARDUINO_DEVICE(CarduinoUsbDevice.class.getSimpleName(), R.string.device_type_carduinousbdevicehandler, R.drawable.ic_device_type_carduino),
    GPS_DEVICE(GpsUsbDevice.class.getSimpleName(), R.string.device_type_gpsusbdevicehandler, R.drawable.ic_device_type_gps);

    private String deviceClass;
    private int typeName;
    private int typeIcon;

    DeviceType(@NonNull String deviceClass, @StringRes int typeName, @DrawableRes int typeIcon) {
        this.deviceClass = deviceClass;
        this.typeName = typeName;
        this.typeIcon = typeIcon;
    }

    @StringRes
    public int getTypeName() {
        return this.typeName;
    }

    @DrawableRes
    public int getTypeIcon() {
        return this.typeIcon;
    }

    @NonNull
    public static DeviceType get(@NonNull Class<? extends Device> deviceClass) {
        for (DeviceType type : DeviceType.values()) {
            if (type.deviceClass.equals(deviceClass.getSimpleName())) {
                return type;
            }
        }
        return UNKNOWN_DEVICE;
    }

    @NonNull
    public static DeviceType get(@NonNull DeviceEntity entity) {
        for (DeviceType type : DeviceType.values()) {
            if (type.deviceClass.equals(entity.className)) {
                return type;
            }
        }
        return UNKNOWN_DEVICE;
    }

}
