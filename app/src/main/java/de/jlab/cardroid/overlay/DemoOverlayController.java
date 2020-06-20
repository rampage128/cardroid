package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

public class DemoOverlayController extends AbstractOverlayController {

    private CarBubble bubble;
    private CarControls carControls;
    private VolumeControls volumeControls;

    private long animationDurationShort;
    private long animationDurationMedium;

    public DemoOverlayController(@NonNull Context context) {
        super(context);

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

        // TODO add car Controls demo mode
        //this.carControls = new CarControls(this::onControlToggle, this.acController, context, maxFanLevel, minTemperature, maxTemperature);
        //this.carControls.create();
        this.volumeControls = new VolumeControls(context, volumeSteps);
        this.volumeControls.create();
    }

    private void onBubbleToggle(boolean isVisible) {
        if (isVisible) {
            this.bubble.setFanLevel(3);
            this.bubble.setTemperature(24.5f);
        }
    }

    private void onControlToggle(boolean isVisible) {

    }

    @Override
    public void updateBubblePosition(int x, int y) {
        this.volumeControls.recalculateDials(x, y);
    }

    public void showCarControls() {
        //this.carControls.fadeIn(this.animationDurationShort);
    }

    public void showVolumeControls(int x, int y) {
        Point sourcePosition = new Point(x, y);
        this.volumeControls.fadeIn(this.animationDurationMedium, sourcePosition);
    }

    public void hideVolumeControls() {
        this.volumeControls.fadeOut(this.animationDurationMedium);
    }

    public void setVolumeFromCoords(int x, int y) {
        this.volumeControls.setProgressFromCoords(x, y);
    }

    @Override
    protected void onStart() {
        this.bubble.show(null);
    }

    @Override
    protected void onStop() {
        this.bubble.hide();
    }

    @Override
    protected void onDispose() {

    }
}