package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

import androidx.cardview.widget.CardView;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.car.CarSystemEvent;
import de.jlab.cardroid.usb.carduino.CarduinoService;
import de.jlab.cardroid.variables.Variable;
import de.jlab.cardroid.variables.VariableStore;

public class OverlayWindow {
    private static final String LOG_TAG = "OverlayWindow";

    private boolean isAttached = false;

    private Handler uiHandler;
    private CarduinoService service;
    private VariableStore variableStore;

    private WindowManager windowManager;

    private boolean trackingFans = false;
    private boolean trackingTemp = false;

    private int minTemp = 0;

    private float temperature = 0;
    private boolean isDuctFace = false;
    private boolean isDuctFeet = false;
    private boolean isDuctWindshield = false;
    private boolean isWindshieldHeating = false;
    private boolean isRecirculation = false;
    private boolean isAutomatic = false;
    private boolean isAcOn = false;
    private boolean isRearWindowHeating = false;
    private int fanLevel = 0;

    private Runnable stopFanInteraction = new Runnable() {
        @Override
        public void run() {
            trackingFans = false;
        }
    };

    private Runnable stopTempInteraction = new Runnable() {
        @Override
        public void run() {
            trackingTemp = false;
            viewHolder.temperatureText.setVisibility(View.VISIBLE);
            viewHolder.temperatureChangeText.setVisibility(View.INVISIBLE);
        }
    };

    private static class ViewHolder {
        View rootView;

        View toggleButton;
        TextView mainText;
        SeekArc mainFanIcon;

        View detailView;

        OverlayToggleButton offButton;
        OverlayToggleButton wshButton;
        OverlayToggleButton rwhButton;
        OverlayToggleButton recirculationButton;

        View modeButton;
        View modeFaceIcon;
        View modeFeetIcon;
        View modeWsIcon;
        SeekArc fanDial;
        TextView fanChangeText;
        OverlayToggleButton autoButton;

        SeekArc temperatureDial;
        TextView temperatureText;
        TextView temperatureChangeText;
        OverlayToggleButton acButton;
    }
    private ViewHolder viewHolder = new ViewHolder();


    public OverlayWindow(CarduinoService service, VariableStore variableStore) {
        this.service = service;
        this.variableStore = variableStore;
    }

    public void create() {
        if (viewHolder.rootView != null) {
            this.destroy();
        }

        this.uiHandler = new Handler();

        this.windowManager = (WindowManager) this.service.getSystemService(Context.WINDOW_SERVICE);

        // Request user to grant permission for drawing if needed and bail out
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this.service)) {
            Intent permissionIntent = new Intent(this.service, SettingsActivity.class);
            permissionIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.OverlayPreferenceFragment.class.getName());
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.service.startActivity(permissionIntent);
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.service);
        this.minTemp = Integer.valueOf(prefs.getString("overlay_temperature_min", "16"));
        int maxTemp = Integer.valueOf(prefs.getString("overlay_temperature_max", "30"));
        int maxFan = Integer.valueOf(prefs.getString("overlay_fan_max", "7"));

        // initialize views
        viewHolder.rootView = LayoutInflater.from(this.service).inflate(R.layout.view_overlay, null);

        viewHolder.toggleButton = viewHolder.rootView.findViewById(R.id.toggleButton);
        viewHolder.mainText = (TextView)viewHolder.rootView.findViewById(R.id.mainText);
        viewHolder.mainFanIcon = (SeekArc)viewHolder.rootView.findViewById(R.id.mainFanIcon);

        viewHolder.detailView = viewHolder.rootView.findViewById(R.id.detailContainer);

        viewHolder.offButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.offButton);
        viewHolder.wshButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.wshButton);
        viewHolder.rwhButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.rwhButton);
        viewHolder.recirculationButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.recirculationButton);

        viewHolder.modeButton = viewHolder.rootView.findViewById(R.id.modeButton);
        viewHolder.modeFaceIcon = viewHolder.rootView.findViewById(R.id.ductFace);
        viewHolder.modeFeetIcon = viewHolder.rootView.findViewById(R.id.ductFeet);
        viewHolder.modeWsIcon = viewHolder.rootView.findViewById(R.id.ductWindshield);
        viewHolder.fanChangeText = (TextView)viewHolder.rootView.findViewById(R.id.fanChangeText);
        viewHolder.fanDial = (SeekArc)viewHolder.rootView.findViewById(R.id.fanBar);
        viewHolder.autoButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.autoButton);

        viewHolder.temperatureDial = (SeekArc)viewHolder.rootView.findViewById(R.id.temperatureBar);
        viewHolder.temperatureText = (TextView)viewHolder.rootView.findViewById(R.id.temperatureText);
        viewHolder.temperatureChangeText = (TextView)viewHolder.rootView.findViewById(R.id.temperatureChangeText);
        viewHolder.acButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.acButton);

        viewHolder.temperatureDial.setMax((maxTemp - this.minTemp) * 2);
        viewHolder.temperatureDial.setSegments((maxTemp - this.minTemp) * 2);
        viewHolder.fanDial.setMax(maxFan);
        viewHolder.fanDial.setSegments(maxFan);
        viewHolder.mainFanIcon.setMax(maxFan);
        viewHolder.mainFanIcon.setSegments(maxFan);

        // Add rootview to overlay window
        final WindowManager.LayoutParams params = getWindowLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );
        this.windowManager.addView(viewHolder.rootView, params);
        this.isAttached = true;

        // Make overlay toggleable
        viewHolder.toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
        viewHolder.detailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        View.OnClickListener buttonListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.offButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_OFF_BUTTON, null);
                        break;
                    case R.id.wshButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_WSH_BUTTON, null);
                        break;
                    case R.id.rwhButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_RWH_BUTTON, null);
                        break;
                    case R.id.recirculationButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_RECIRCULATION_BUTTON, null);
                        break;
                    case R.id.modeButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_MODE_BUTTON, null);
                        break;
                    case R.id.autoButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_AUTO_BUTTON, null);
                        break;
                    case R.id.acButton:
                        service.sendCarduinoEvent(CarSystemEvent.CC_AC_BUTTON, null);
                        break;
                }
            }
        };
        viewHolder.offButton.setOnClickListener(buttonListener);
        viewHolder.wshButton.setOnClickListener(buttonListener);
        viewHolder.rwhButton.setOnClickListener(buttonListener);
        viewHolder.recirculationButton.setOnClickListener(buttonListener);
        viewHolder.modeButton.setOnClickListener(buttonListener);
        viewHolder.autoButton.setOnClickListener(buttonListener);
        viewHolder.acButton.setOnClickListener(buttonListener);

        // Init events
        viewHolder.fanDial.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int value = Math.max(progress, 1);
                    viewHolder.fanChangeText.setText(String.format(Locale.getDefault(), "%s", value));
                    if (progress < 1) {
                        viewHolder.fanDial.setProgress(1);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekBar) {
                uiHandler.removeCallbacks(stopFanInteraction);
                trackingFans = true;
            }

            @Override
            public void onStopTrackingTouch(SeekArc seekBar) {
                service.sendCarduinoEvent(CarSystemEvent.CC_FAN_LEVEL, new byte[] { (byte)Math.max(seekBar.getProgress(), 1) });
                uiHandler.postDelayed(stopFanInteraction, 1000);
            }
        });

        viewHolder.temperatureDial.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float value = progress / 2f + OverlayWindow.this.minTemp;
                    viewHolder.temperatureChangeText.setText(getFancyDecimalValue(value)); //String.format(Locale.getDefault(), "%s", value)
                }
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekBar) {
                viewHolder.temperatureText.setVisibility(View.INVISIBLE);
                viewHolder.temperatureChangeText.setVisibility(View.VISIBLE);
                uiHandler.removeCallbacks(stopTempInteraction);
                trackingTemp = true;
            }

            @Override
            public void onStopTrackingTouch(SeekArc seekBar) {
                float temperature = seekBar.getProgress() / 2f + OverlayWindow.this.minTemp;
                service.sendCarduinoEvent(CarSystemEvent.CC_TEMPERATURE, new byte[] { (byte)(temperature * 2) });
                uiHandler.postDelayed(stopTempInteraction, 1000);
            }
        });

        // Set up initial view state
        viewHolder.detailView.setVisibility(View.GONE);
        updateUi();
    }

    private void attachBubbleData() {
        attach("hvacTargetTemperature", tempListener);
        attach("hvacFanLevel", fanListener);
    }

    private void attachFullData() {
        attach("hvacIsAirductWindshield", ductWindshieldListener);
        attach("hvacIsAirductFace", ductFaceListener);
        attach("hvacIsAirductFeet", ductFeetListener);
        attach("hvacIsWindshieldHeating", isWindshieldHeatingListener);
        attach("hvacIsRecirculation", isRecirculationListener);
        attach("hvacIsAutomatic", isAutomaticListener);
        attach("hvacIsAcOn", isAcOnListener);
        attach("hvacIsRearWindowHeating", isRearWindowHeatingListener);
    }

    private void attach(String variableName, Variable.VariableChangeListener listener) {
        Variable variable = this.variableStore.getVariable(variableName);
        if (variable != null) {
            variable.addChangeListener(listener);
        }
    }

    private void detachBubbleData() {
        detach("hvacTargetTemperature", tempListener);
        detach("hvacFanLevel", fanListener);
    }

    private void detachFullData() {
        detach("hvacIsAirductWindshield", ductWindshieldListener);
        detach("hvacIsAirductFace", ductFaceListener);
        detach("hvacIsAirductFeet", ductFeetListener);
        detach("hvacIsWindshieldHeating", isWindshieldHeatingListener);
        detach("hvacIsRecirculation", isRecirculationListener);
        detach("hvacIsAutomatic", isAutomaticListener);
        detach("hvacIsAcOn", isAcOnListener);
        detach("hvacIsRearWindowHeating", isRearWindowHeatingListener);
    }

    private void detach(String variableName, Variable.VariableChangeListener listener) {
        Variable variable = this.variableStore.getVariable(variableName);
        if (variable != null) {
            variable.removeChangeListener(listener);
        }
    }

    // BUBBLE LISTENERS

    private Variable.VariableChangeListener tempListener = (oldValue, newValue) -> {
        this.temperature = ((Double)newValue).floatValue();
        updateUi();
    };

    private Variable.VariableChangeListener fanListener = (oldValue, newValue) -> {
        this.fanLevel = ((Double)newValue).intValue();
        updateUi();
    };

    // FULL LISTENERS

    private Variable.VariableChangeListener ductFaceListener = (oldValue, newValue) -> {
        this.isDuctFace = (Boolean)newValue;
        updateUi();
    };

    private Variable.VariableChangeListener ductFeetListener = (oldValue, newValue) -> {
        this.isDuctFeet = (Boolean)newValue;
        updateUi();
    };

    private Variable.VariableChangeListener ductWindshieldListener = (oldValue, newValue) -> {
        this.isDuctWindshield = (Boolean)newValue;
        updateUi();
    };

    private Variable.VariableChangeListener isAcOnListener = (oldValue, newValue) -> {
        this.isAcOn = (Integer)newValue == 1;
        updateUi();
    };

    private Variable.VariableChangeListener isRecirculationListener = (oldValue, newValue) -> {
        this.isRecirculation = (Integer)newValue == 1;
        updateUi();
    };

    private Variable.VariableChangeListener isWindshieldHeatingListener = (oldValue, newValue) -> {
        this.isWindshieldHeating = (Integer)newValue == 1;
        updateUi();
    };

    private Variable.VariableChangeListener isAutomaticListener = (oldValue, newValue) -> {
        this.isAutomatic = (Integer)newValue == 1;
        updateUi();
    };

    private Variable.VariableChangeListener isRearWindowHeatingListener = (oldValue, newValue) -> {
        this.isRearWindowHeating = (Integer)newValue == 1;
        updateUi();
    };

    // LISTENERS END

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

    public void initData() {
        this.attachBubbleData();
    }

    public void toggle() {
        float elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, this.service.getResources().getDisplayMetrics());
        if (this.viewHolder.detailView != null) {
            if (this.viewHolder.detailView.getVisibility() == View.GONE) {
                this.viewHolder.toggleButton.setVisibility(View.GONE);
                this.viewHolder.toggleButton.setElevation(0);
                windowManager.updateViewLayout(
                        this.viewHolder.rootView,
                        getWindowLayout(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT
                        )
                );
                this.viewHolder.detailView.setElevation(elevation);
                this.viewHolder.detailView.setVisibility(View.VISIBLE);
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        roundCardViews(viewHolder.detailView);
                    }
                });
                attachFullData();
            }
            else {
                this.viewHolder.detailView.setVisibility(View.GONE);
                this.viewHolder.detailView.setElevation(0);
                windowManager.updateViewLayout(
                        this.viewHolder.rootView,
                        getWindowLayout(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT
                        )
                );
                this.viewHolder.toggleButton.setElevation(elevation);
                this.viewHolder.toggleButton.setVisibility(View.VISIBLE);
                detachFullData();
            }
        }
    }

    private WindowManager.LayoutParams getWindowLayout(int width, int height) {
        /*
        Point fullSize = new Point();
        Point usableSize = new Point();

        windowManager.getDefaultDisplay().getRealSize(fullSize);
        windowManager.getDefaultDisplay().getSize(usableSize);

        int navigationBarOffset = usableSize.y - fullSize.y;
        */

        int windowType = WindowManager.LayoutParams.TYPE_PHONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            windowType = WindowManager.LayoutParams.TYPE_TOAST;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        WindowManager.LayoutParams params =  new WindowManager.LayoutParams(
                width,
                height,
                windowType,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        return params;
    }

    public void destroy() {
        if (this.isAttached) {
            this.windowManager.removeView(this.viewHolder.rootView);
            this.isAttached = false;
        }
        detachBubbleData();
        detachFullData();
    }

    private void updateUi() {
        runOnUiThread(() -> {
            boolean isFanOff = fanLevel == 0;
            boolean isTempOff = temperature == 0;
            Spanned temperatureText  = getFancyDecimalValue(temperature); //String.format(Locale.getDefault(), "%s", temperature);
            String fanText          = String.format(Locale.getDefault(), "%s", fanLevel);

            viewHolder.mainFanIcon.setProgress(fanLevel);

            viewHolder.offButton.setState(!isFanOff);
            viewHolder.wshButton.setState(isWindshieldHeating);
            viewHolder.rwhButton.setState(isRearWindowHeating);
            viewHolder.recirculationButton.setState(isRecirculation);

            if (!trackingFans) {
                if (isFanOff) {
                    viewHolder.modeFaceIcon.setVisibility(View.INVISIBLE);
                    viewHolder.modeFeetIcon.setVisibility(View.INVISIBLE);
                    viewHolder.modeWsIcon.setVisibility(View.INVISIBLE);
                    viewHolder.fanChangeText.setText(R.string.cc_off);
                } else {
                    viewHolder.modeFaceIcon.setVisibility(isDuctFace ? View.VISIBLE : View.INVISIBLE);
                    viewHolder.modeFeetIcon.setVisibility(isDuctFeet ? View.VISIBLE : View.INVISIBLE);
                    viewHolder.modeWsIcon.setVisibility(isDuctWindshield ? View.VISIBLE : View.INVISIBLE);
                    viewHolder.fanChangeText.setText(fanText);
                }
                viewHolder.fanDial.setProgress(Math.max(0, fanLevel));
            }
            viewHolder.autoButton.setState(isAutomatic);

            if (!trackingTemp) {
                viewHolder.temperatureDial.setProgress((int) ((temperature - OverlayWindow.this.minTemp) * 2));
            }
            if (isTempOff) {
                viewHolder.temperatureText.setText(R.string.cc_off);
                viewHolder.mainText.setText(R.string.cc_off);
            }
            else {
                viewHolder.temperatureText.setText(temperatureText);
                viewHolder.mainText.setText(temperatureText);
            }
            viewHolder.acButton.setState(isAcOn);
        });
    }

    private Spanned getFancyDecimalValue(float value) {
        String[] parts = Float.toString(value).split("\\.");
        String decimals = parts.length > 1 ? "<small>." + parts[1] + "</small>" : "";
        return Html.fromHtml(parts[0] + decimals);
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }
}
