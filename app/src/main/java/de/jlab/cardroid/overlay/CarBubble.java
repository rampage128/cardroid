package de.jlab.cardroid.overlay;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;
import de.jlab.cardroid.utils.ui.UnitValues;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableController;

public final class CarBubble extends Overlay {

    private VariableController variableController;

    private SeekArc fanSeekBar;
    private TextView temperatureText;
    private OverlayController overlayController;
    private int maxFanLevel;
    private int minTemperature;
    private int maxTemperature;
    private boolean enableVolumeControl;
    private long volumeControlTouchDuration;

    private Variable.VariableChangeListener variableListener = this::onVariableChange;
    private TapTouchListener.ActionListener onTapTouch = this::onTapTouch;

    public CarBubble(@NonNull VariableController variableController, @NonNull OverlayController overlayController, @NonNull Context context, int maxFanLevel, int minTemperature, int maxTemperature, boolean enableVolumeControl, long volumeControlTouchDuration) {
        super(context);
        this.variableController = variableController;
        this.overlayController = overlayController;
        this.maxFanLevel = maxFanLevel;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.enableVolumeControl = enableVolumeControl;
        this.volumeControlTouchDuration = volumeControlTouchDuration;
    }

    private void onVariableChange(Object oldValue, Object newValue, String variableName) {
        this.runOnUiThread(() -> {
            switch(variableName) {
                case "hvacTargetTemperature":
                    float temperature = UnitValues.constrainToRange(((Number)newValue).floatValue(), this.minTemperature, this.maxTemperature);
                    CharSequence temperatureText = temperature > 0 ? UnitValues.getFancyDecimalValue(temperature) : getString(R.string.cc_off);
                    this.temperatureText.setText(temperatureText);
                    break;
                case "hvacFanLevel":
                    int fanLevel = UnitValues.constrainToRange(((Number)newValue).intValue(), 0, this.maxFanLevel);
                    this.fanSeekBar.setProgress(fanLevel);
                    break;
            }
        });
    }

    private void onTapTouch(@NonNull View view, @NonNull TapTouchListener.Action action, @NonNull MotionEvent motionEvent) {
        if (action == TapTouchListener.Action.START_HOLD) {
            this.overlayController.showVolumeControls();
        } else if (action == TapTouchListener.Action.STOP_HOLD) {
            this.overlayController.hideVolumeControls();
        } else if (action == TapTouchListener.Action.MOVE_HOLD) {
            int x = Math.round(motionEvent.getX() - (view.getWidth() / 2f));
            int y = Math.round(motionEvent.getY() - (view.getWidth() / 2f));
            this.overlayController.setVolumeFromCoords(x, y);
        }
    }

    private void showCarControls() {
        this.overlayController.showCarControls();
    }

    @Override
    protected void onCreate(@NonNull WindowManager.LayoutParams windowParams, @NonNull Context context) {
        View contentView = this.setContentView(R.layout.overlay_bubble_car);

        this.fanSeekBar = this.findViewById(R.id.progressBar);
        this.temperatureText = this.findViewById(R.id.text);

        this.fanSeekBar.setMax(this.maxFanLevel);
        this.fanSeekBar.setSegments(this.maxFanLevel);

        contentView.setOnClickListener(v -> showCarControls());
        if (this.enableVolumeControl) {
            contentView.setOnTouchListener(new TapTouchListener(new Handler(context.getMainLooper()), this.onTapTouch, this.volumeControlTouchDuration));
        }

        windowParams.width = windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    }

    @Override
    protected void onShow() {
        this.variableController.subscribe(this.variableListener, "hvacTargetTemperature", "hvacFanLevel");
    }

    @Override
    protected void onHide() {
        this.variableController.unsubscribe(this.variableListener, "hvacTargetTemperature", "hvacFanLevel");
    }

    @Override
    protected void onDestroy() {}
}
