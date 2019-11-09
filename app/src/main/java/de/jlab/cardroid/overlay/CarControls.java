package de.jlab.cardroid.overlay;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import de.jlab.cardroid.R;
import de.jlab.cardroid.car.nissan370z.AcCanController;
import de.jlab.cardroid.utils.ui.UnitValues;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableController;

public final class CarControls extends Overlay {

    private static final String[] VARIABLES = new String[] {
            "hvacTargetTemperature",
            "hvacFanLevel",
            "hvacIsAirductFace",
            "hvacIsAirductFeet",
            "hvacIsAirductWindshield",
            "hvacIsAcOn",
            "hvacIsRecirculation",
            "hvacIsAutomatic",
            "hvacIsRearWindowHeating",
            "hvacIsWindshieldHeating"
    };

    private VariableController variableController;
    private AcCanController acController;
    private int maxFanLevel;
    private int minTemperature;
    private int maxTemperature;
    private boolean isRounded = false;
    private boolean isTrackingTemperature = false;
    private boolean isTrackingFanLevel = false;

    private Variable.VariableChangeListener onVariableChange = this::onVariableChange;
    private Runnable onStopTrackingTemperature = this::stopTrackingTemperature;
    private Runnable onStopTrackingFanLevel = this::stopTrackingFanLevel;

    private ViewHolder views = new ViewHolder();
    private AcState state = new AcState();

    private int animationDuration;
    
    public CarControls(@NonNull VariableController variableController, @NonNull AcCanController acController, @NonNull Context context, int maxFanLevel, int minTemperature, int maxTemperature) {
        super(context);
        this.variableController = variableController;
        this.acController = acController;
        this.maxFanLevel = maxFanLevel;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;

        this.animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onCreate(@NonNull WindowManager.LayoutParams windowParams, @NonNull Context context) {
        this.views.contentView = setContentView(R.layout.overlay_car_370z);

        this.views.offButton = this.findViewById(R.id.offButton);
        this.views.wshButton = this.findViewById(R.id.wshButton);
        this.views.rwhButton = this.findViewById(R.id.rwhButton);
        this.views.recirculationButton = this.findViewById(R.id.recirculationButton);

        this.views.modeButton = this.findViewById(R.id.modeButton);
        this.views.modeFaceIcon = this.findViewById(R.id.ductFace);
        this.views.modeFeetIcon = this.findViewById(R.id.ductFeet);
        this.views.modeWsIcon = this.findViewById(R.id.ductWindshield);
        this.views.fanChangeText = this.findViewById(R.id.fanChangeText);
        this.views.fanDial = this.findViewById(R.id.fanBar);
        this.views.autoButton = this.findViewById(R.id.autoButton);

        this.views.temperatureDial = this.findViewById(R.id.temperatureBar);
        this.views.temperatureText = this.findViewById(R.id.temperatureText);
        this.views.temperatureChangeText = this.findViewById(R.id.temperatureChangeText);
        this.views.acButton = this.findViewById(R.id.acButton);

        this.views.temperatureDial.setMax((this.maxTemperature - this.minTemperature) * 2);
        this.views.temperatureDial.setSegments((this.maxTemperature - this.minTemperature) * 2);
        this.views.fanDial.setMax(this.maxFanLevel);
        this.views.fanDial.setSegments(this.maxFanLevel);

        this.views.contentView.setOnClickListener(v -> this.fadeOut(this.animationDuration));

        this.views.offButton.setOnClickListener(this::buttonPushed);
        this.views.wshButton.setOnClickListener(this::buttonPushed);
        this.views.rwhButton.setOnClickListener(this::buttonPushed);
        this.views.recirculationButton.setOnClickListener(this::buttonPushed);
        this.views.modeButton.setOnClickListener(this::buttonPushed);
        this.views.autoButton.setOnClickListener(this::buttonPushed);
        this.views.acButton.setOnClickListener(this::buttonPushed);

        this.views.temperatureDial.setOnSeekArcEventListener(this::onTemperatureEvent);
        this.views.fanDial.setOnSeekArcEventListener(this::onFanLevelEvent);

        this.isRounded = false;

        windowParams.width = windowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
    }

    private void stopTrackingTemperature() {
        this.isTrackingTemperature = false;
        this.views.temperatureText.setVisibility(View.VISIBLE);
        this.views.temperatureChangeText.setVisibility(View.INVISIBLE);
        this.updateUi();
    }

    private void onTemperatureEvent(@NonNull SeekArcEvent event) {
        if (event.isUserEvent()) {
            float temperature = event.getProgress() / 2f + this.minTemperature;

            switch(event.getType()) {
                case TRACKING_STARTED:
                    this.views.temperatureChangeText.setText(UnitValues.getFancyDecimalValue(temperature));
                    this.views.temperatureText.setVisibility(View.INVISIBLE);
                    this.views.temperatureChangeText.setVisibility(View.VISIBLE);
                    this.cancelOnUiThread(this.onStopTrackingTemperature);
                    this.isTrackingTemperature = true;
                    break;
                case PROGRESS_CHANGED:
                    this.views.temperatureChangeText.setText(UnitValues.getFancyDecimalValue(temperature));
                    break;
                case TRACKING_STOPPED:
                    this.acController.changeTargetTemperature((byte) (temperature * 2));
                    this.runOnUiThread(this.onStopTrackingTemperature, 1000);
                    break;
            }
        }
    }

    private void stopTrackingFanLevel() {
        this.isTrackingFanLevel = false;
        this.updateUi();
    }

    private void onFanLevelEvent(@NonNull SeekArcEvent event) {
        if (event.isUserEvent()) {
            int fanLevel = Math.max(event.getProgress(), 1);

            switch (event.getType()) {
                case TRACKING_STARTED:
                    this.cancelOnUiThread(this.onStopTrackingFanLevel);
                    this.isTrackingFanLevel = true;
                    break;
                case PROGRESS_CHANGED:
                    this.views.fanChangeText.setText(String.format(Locale.getDefault(), "%s", fanLevel));
                    if (event.getProgress() < 1) {
                        this.views.fanDial.setProgress(1);
                    }
                    break;
                case TRACKING_STOPPED:
                    this.acController.changeFanLevel((byte) fanLevel);
                    this.runOnUiThread(this.onStopTrackingFanLevel, 1000);
                    break;
            }
        }
    }

    private void buttonPushed(View view) {
        switch (view.getId()) {
            case R.id.offButton:
                acController.pushOffButton();
                break;
            case R.id.wshButton:
                acController.pushWindshieldButton();
                break;
            case R.id.rwhButton:
                acController.pushRearHeaterButton();
                break;
            case R.id.recirculationButton:
                acController.pushRecirculationButton();
                break;
            case R.id.modeButton:
                acController.pushModeButton();
                break;
            case R.id.autoButton:
                acController.pushAutoButton();
                break;
            case R.id.acButton:
                acController.pushAcButton();
                break;
        }
    }

    private void onVariableChange(Object oldValue, Object newValue, String variableName) {
        switch(variableName) {
            case "hvacTargetTemperature":
                this.state.temperature = UnitValues.constrainToRange(((Number)newValue).floatValue(), this.minTemperature, this.maxTemperature);
                break;
            case "hvacFanLevel":
                this.state.fanLevel = UnitValues.constrainToRange(((Number)newValue).intValue(), 0, this.maxFanLevel);
                this.state.isOn = this.state.fanLevel > 0;
                break;
            case "hvacIsAirductFace":
                this.state.airductFace = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsAirductFeet":
                this.state.airductFeet = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsAirductWindshield":
                this.state.airductWsh = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsAcOn":
                this.state.isAcOn = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsRecirculation":
                this.state.isRecirculation = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsAutomatic":
                this.state.isAuto = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsRearWindowHeating":
                this.state.isRearWindowHeating = ((Number)newValue).intValue() == 1;
                break;
            case "hvacIsWindshieldHeating":
                this.state.isWindshieldHeating = ((Number)newValue).intValue() == 1;
                break;
        }

        this.updateUi();
    }

    private void updateUi() {
        this.runOnUiThread(() -> {
            // Update buttons
            this.views.offButton.setState(this.state.isOn);
            this.views.wshButton.setState(this.state.isWindshieldHeating);
            this.views.rwhButton.setState(this.state.isRearWindowHeating);
            this.views.recirculationButton.setState(this.state.isRecirculation);
            this.views.acButton.setState(this.state.isAcOn);
            this.views.autoButton.setState(this.state.isAuto);

            // Update fan views
            if (!this.isTrackingFanLevel) {
                CharSequence fanText = this.state.fanLevel > 0 ? Integer.toString(this.state.fanLevel) : getString(R.string.cc_off);
                this.views.fanChangeText.setText(fanText);

                this.views.fanDial.setProgress(this.state.fanLevel);

                if (this.state.isOn) {
                    this.views.modeFaceIcon.setVisibility(this.state.airductFace ? View.VISIBLE : View.INVISIBLE);
                    this.views.modeFeetIcon.setVisibility(this.state.airductFeet ? View.VISIBLE : View.INVISIBLE);
                    this.views.modeWsIcon.setVisibility(this.state.airductWsh ? View.VISIBLE : View.INVISIBLE);
                } else {
                    this.views.modeFaceIcon.setVisibility(View.INVISIBLE);
                    this.views.modeFeetIcon.setVisibility(View.INVISIBLE);
                    this.views.modeWsIcon.setVisibility(View.INVISIBLE);
                }
            }

            // Update temperature views
            if (!this.isTrackingTemperature) {
                CharSequence temperatureText = this.state.temperature > 0 ? UnitValues.getFancyDecimalValue(this.state.temperature) : getString(R.string.cc_off);
                this.views.temperatureText.setText(temperatureText);

                int temperatureProgress = (int) ((this.state.temperature - this.minTemperature) * 2);
                this.views.temperatureDial.setProgress(temperatureProgress);
            }
        });
    }

    // FIXME: This is time consuming and makes the initial fade-in of the overlay lag for ~500ms ...
    private void roundCardViews(View v) {
        if (v instanceof CardView) {
            ((CardView)v).setRadius(v.getWidth() / 2f);
        }
        else if (v instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) v;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                roundCardViews(child);
            }
        }
    }

    @Override
    protected void onShow() {
        if (!this.isRounded) {
            this.runOnUiThread(() -> {
                this.roundCardViews(this.views.contentView);
                this.isRounded = true;
            });
        }

        this.variableController.subscribe(this.onVariableChange, VARIABLES);
    }

    @Override
    protected void onHide() {
        this.variableController.unsubscribe(this.onVariableChange, VARIABLES);
    }

    @Override
    protected void onDestroy() {}


    private static class AcState {
        private boolean isOn = false;
        private float temperature = 0;
        private int fanLevel = 0;
        private boolean airductFeet = false;
        private boolean airductFace = false;
        private boolean airductWsh = false;
        private boolean isAcOn = false;
        private boolean isRecirculation = false;
        private boolean isAuto = false;
        private boolean isRearWindowHeating = false;
        private boolean isWindshieldHeating = false;
    }

    private static class ViewHolder {
        private OverlayToggleButton offButton = null;
        private OverlayToggleButton wshButton = null;
        private OverlayToggleButton rwhButton = null;
        private OverlayToggleButton recirculationButton = null;
        private OverlayToggleButton autoButton = null;
        private OverlayToggleButton acButton = null;
        private FrameLayout modeButton = null;
        private ImageView modeFaceIcon = null;
        private ImageView modeFeetIcon = null;
        private ImageView modeWsIcon = null;
        private TextView fanChangeText = null;
        private TextView temperatureText = null;
        private TextView temperatureChangeText = null;
        private SeekArc fanDial = null;
        private SeekArc temperatureDial = null;
        private View contentView = null;
    }

}
