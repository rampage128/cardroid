package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.car.nissan370z.AcCanController;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.utils.permissions.OverlayPermission;
import de.jlab.cardroid.utils.permissions.Permission;
import de.jlab.cardroid.utils.permissions.PermissionReceiver;
import de.jlab.cardroid.utils.permissions.PermissionRequest;
import de.jlab.cardroid.variables.VariableController;

public final class OverlayController {

    public static final PermissionRequest[] PERMISSIONS = new PermissionRequest[] {
        new PermissionRequest(OverlayPermission.PERMISSION_KEY, Permission.Constraint.REQUIRED, R.string.overlay_permission_reason)
    };

    private Context context;

    private CarBubble bubble;
    private CarControls carControls;
    private VolumeControls volumeControls;

    private DeviceController deviceController;
    private AcCanController acController;

    private Device.StateObserver onDeviceStateChange = this::onDeviceStateChange;

    private PermissionReceiver permissionReceiver;

    private long animationDurationShort;
    private long animationDurationMedium;

    private boolean connected = false;
    private boolean wasStarted = false;

    public OverlayController(@NonNull VariableController variableController, @NonNull DeviceController deviceController, @NonNull Context context) {
        this.context = context;
        this.deviceController = deviceController;
        this.permissionReceiver = new PermissionReceiver(context, this.getClass(), this::onOverlayPermissionGranted);

        this.animationDurationShort = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        this.animationDurationMedium = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int maxFanLevel = Integer.valueOf(prefs.getString("overlay_fan_max", "7"));
        int minTemperature = Integer.valueOf(prefs.getString("overlay_temperature_min", "16"));
        int maxTemperature = Integer.valueOf(prefs.getString("overlay_temperature_max", "30"));

        int volumeSteps = Integer.valueOf(prefs.getString("overlay_volume_steps", "10"));
        long volumeTouchDuration = Long.valueOf(prefs.getString("overlay_volume_touch_duration", "100"));
        boolean enableVolumeControls = prefs.getBoolean("overlay_volume_enabled", true);

        this.bubble = new CarBubble(variableController, this, context, maxFanLevel, minTemperature, maxTemperature, enableVolumeControls, volumeTouchDuration);
        this.bubble.create();

        DeviceUid deviceUid = getDeviceUid(context);
        if (deviceUid != null) {
            this.acController = new AcCanController(deviceController, deviceUid);
            this.deviceController.subscribeState(this.onDeviceStateChange, deviceUid);
        }

        this.carControls = new CarControls(variableController, this.acController, context, maxFanLevel, minTemperature, maxTemperature);
        this.carControls.create();
        this.volumeControls = new VolumeControls(context, volumeSteps);
        this.volumeControls.create();
    }

    private void onOverlayPermissionGranted() {
        if (this.wasStarted) {
            this.start();
        }
    }

    private void onDeviceStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        this.connected = state == Device.State.READY;
        if (this.connected) {
            this.start();
        } else {
            this.stop();
        }
    }

    @Nullable
    private static DeviceUid getDeviceUid(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceUid = prefs.getString("overlay_device_uid", null);
        return deviceUid != null ? new DeviceUid(deviceUid) : null;
    }

    public void start() {
        if (!this.connected) {
            return;
        }

        if (this.permissionReceiver.requestPermissions(this.context, PERMISSIONS)) {
            this.bubble.show();
        }

        this.wasStarted = true;
    }

    public void stop() {
        this.bubble.hide();
        this.carControls.hide();
        this.volumeControls.hide();
        this.wasStarted = false;
    }

    public void dispose() {
        this.permissionReceiver.dispose();
        if (this.acController != null) {
            this.acController.dispose();
        }
        this.volumeControls.destroy();
        this.carControls.destroy();
        this.bubble.destroy();
        this.deviceController.unsubscribeState(this.onDeviceStateChange);
        this.context = null;
    }

    public void showCarControls() {
        this.carControls.fadeIn(this.animationDurationShort);
    }

    public void showVolumeControls() {
        this.volumeControls.fadeIn(this.animationDurationMedium);
    }

    public void hideVolumeControls() {
        this.volumeControls.fadeOut(this.animationDurationMedium);
    }

    public void setVolumeFromCoords(int x, int y) {
        this.volumeControls.setProgressFromCoords(x, y);
    }

}
