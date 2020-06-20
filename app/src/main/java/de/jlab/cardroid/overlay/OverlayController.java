package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.car.nissan370z.AcCanController;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableController;

public final class OverlayController extends AbstractOverlayController {

    private CarBubble bubble;
    private CarControls carControls;
    private VolumeControls volumeControls;

    private Variable.VariableChangeListener bubbleVariableListener = this::bubbleVariableChange;
    private Variable.VariableChangeListener controlVariableListener = this::controlVariableChange;

    private DeviceController deviceController;
    private AcCanController acController;
    private VariableController variableController;

    private Device.StateObserver onDeviceStateChange = this::onDeviceStateChange;

    private long animationDurationShort;
    private long animationDurationMedium;

    private boolean connected = false;

    public OverlayController(@NonNull VariableController variableController, @NonNull DeviceController deviceController, @NonNull Context context) {
        super(context);
        this.deviceController = deviceController;
        this.variableController = variableController;

        this.animationDurationShort = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        this.animationDurationMedium = context.getResources().getInteger(android.R.integer.config_mediumAnimTime);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int maxFanLevel = Integer.valueOf(prefs.getString("overlay_fan_max", "7"));
        int minTemperature = Integer.valueOf(prefs.getString("overlay_temperature_min", "16"));
        int maxTemperature = Integer.valueOf(prefs.getString("overlay_temperature_max", "30"));

        int volumeSteps = Integer.valueOf(prefs.getString("overlay_volume_steps", "10"));
        long volumeTouchDuration = Long.valueOf(prefs.getString("overlay_volume_touch_duration", "100"));
        boolean enableVolumeControls = prefs.getBoolean("overlay_volume_enabled", true);

        this.bubble = new CarBubble(this, this::onBubbleToggle, context, maxFanLevel, minTemperature, maxTemperature, enableVolumeControls, volumeTouchDuration);
        this.bubble.create();

        DeviceUid deviceUid = getDeviceUid(context);
        if (deviceUid != null) {
            this.acController = new AcCanController(deviceController, deviceUid);
            this.deviceController.subscribeState(this.onDeviceStateChange, deviceUid);
        }

        this.carControls = new CarControls(this::onControlToggle, this.acController, context, maxFanLevel, minTemperature, maxTemperature);
        this.carControls.create();
        this.volumeControls = new VolumeControls(context, volumeSteps);
        this.volumeControls.create();
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

    protected void onStart() {
        this.bubble.show(null);
    }

    protected void onStop() {
        this.bubble.hide();
        this.carControls.hide();
        this.volumeControls.hide();
    }

    public void onDispose() {
        if (this.acController != null) {
            this.acController.dispose();
        }
        this.volumeControls.destroy();
        this.carControls.destroy();
        this.bubble.destroy();
        this.deviceController.unsubscribeState(this.onDeviceStateChange);
    }

    public void bubbleVariableChange(Object oldValue, Object newValue, String variableName) {
        switch(variableName) {
            case "hvacTargetTemperature":
                this.bubble.setTemperature(((Number)newValue).floatValue());
                break;
            case "hvacFanLevel":
                this.bubble.setFanLevel(((Number)newValue).intValue());
                break;
        }
    }

    private void onBubbleToggle(boolean isVisible) {
        if (isVisible) {
            this.variableController.subscribe(this.bubbleVariableListener, "hvacTargetTemperature", "hvacFanLevel");
        } else {
            this.variableController.unsubscribe(this.bubbleVariableListener, "hvacTargetTemperature", "hvacFanLevel");
        }
    }


    public void controlVariableChange(Object oldValue, Object newValue, String variableName) {
        this.carControls.onVariableChange(oldValue, newValue, variableName);
    }

    private void onControlToggle(boolean isVisible) {
        if (isVisible) {
            this.variableController.subscribe(this.controlVariableListener, CarControls.VARIABLES);
        } else {
            this.variableController.unsubscribe(this.controlVariableListener, CarControls.VARIABLES);
        }
    }

    @Override
    public void updateBubblePosition(int x, int y) {
        this.volumeControls.recalculateDials(x, y);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("overlay_bubble_x", x);
        editor.putInt("overlay_bubble_y", y);
        editor.apply();
    }

    public void showCarControls() {
        this.carControls.fadeIn(this.animationDurationShort, null);
    }

    public void showVolumeControls(int x, int y) {
        Point sourcePosition = new Point(x - this.bubble.getWidth() / 2, y - this.bubble.getHeight() / 2);
        this.volumeControls.fadeIn(this.animationDurationMedium, sourcePosition);
    }

    public void hideVolumeControls() {
        this.volumeControls.fadeOut(this.animationDurationMedium);
    }

    public void setVolumeFromCoords(int x, int y) {
        this.volumeControls.setProgressFromCoords(x, y);
    }

}
