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
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.car.CanDataProvider;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.serial.carduino.CarduinoEventProvider;
import de.jlab.cardroid.devices.serial.carduino.CarduinoEventType;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDeviceDetector;
import de.jlab.cardroid.variables.Variable;

/**
 * @deprecated OverlayWindow is static legacy code. This has to be replaced with the coming "screens" feature.
 * This way users will be able to build complete custom hvac-screens based on variable values
 */
@Deprecated
public class OverlayWindow {
    private static final String LOG_TAG = "OverlayWindow";

    private boolean isAttached = false;

    private Handler uiHandler;
    private DeviceService service;

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

    private Timer broadCastTimer;
    private AcCanController acController = new AcCanController();

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

    private Variable.VariableChangeListener bubbleListener = (oldValue, newValue, name) -> {
        switch(name) {
            case "hvacTargetTemperature":
                this.temperature = ((Double)newValue).floatValue();
                break;
            case "hvacFanLevel":
                this.fanLevel = ((Double)newValue).intValue();
                break;
        }
        updateUi();
    };

    private Variable.VariableChangeListener fullChangeListener = (oldValue, newValue, name) -> {
        switch(name) {
            case "hvacIsAirductFace":
                this.isDuctFace = (Boolean)newValue;
                break;
            case "hvacIsAirductFeet":
                this.isDuctFeet = (Boolean)newValue;
                break;
            case "hvacIsAirductWindshield":
                this.isDuctWindshield = (Boolean)newValue;
                break;
            case "hvacIsAcOn":
                this.isAcOn = (Long)newValue == 1;
                break;
            case "hvacIsRecirculation":
                this.isRecirculation = (Long)newValue == 1;
                break;
            case "hvacIsAutomatic":
                this.isAutomatic = (Long)newValue == 1;
                break;
            case "hvacIsRearWindowHeating":
                this.isRearWindowHeating = (Long)newValue == 1;
                break;
            case "hvacIsWindshieldHeating":
                this.isWindshieldHeating = (Long)newValue == 1;
                break;
        }
        updateUi();
    };

    public OverlayWindow(DeviceService service) {
        this.service = service;
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
                CarduinoEventProvider eventProvider = service.getDeviceProvider(CarduinoEventProvider.class);
                if (eventProvider != null) {
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
                acController.changeFanLevel((byte) Math.max(seekBar.getProgress(), 1));
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
                acController.changeTargetTemperature((byte) (temperature * 2));
                uiHandler.postDelayed(stopTempInteraction, 1000);
            }
        });

        // Set up initial view state
        viewHolder.detailView.setVisibility(View.GONE);
        updateUi();
        this.attachBubbleData();
        startBroadCast();
    }

    private void startBroadCast() {
        this.broadCastTimer = new Timer();
        this.broadCastTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                CanDataProvider provider = service.getDeviceProvider(CanDataProvider.class);
                acController.broadcast(provider);
            }
        }, 0, 250);
    }

    private void stopBroadCast() {
        if (this.broadCastTimer != null) {
            this.broadCastTimer.cancel();
        }
    }

    private void sendEvent(@NonNull CarduinoEventType event, @Nullable byte[] payload) {
        CarduinoEventProvider eventProvider = this.service.getDeviceProvider(CarduinoEventProvider.class);
        if (eventProvider != null) {
            eventProvider.sendEvent(event, payload);
        }
    }

    private void attachBubbleData() {
        attach("hvacTargetTemperature", this.bubbleListener);
        attach("hvacFanLevel", this.bubbleListener);
    }

    private void attachFullData() {
        attach("hvacIsAirductWindshield", this.fullChangeListener);
        attach("hvacIsAirductFace", this.fullChangeListener);
        attach("hvacIsAirductFeet", this.fullChangeListener);
        attach("hvacIsWindshieldHeating", this.fullChangeListener);
        attach("hvacIsRecirculation", this.fullChangeListener);
        attach("hvacIsAutomatic", this.fullChangeListener);
        attach("hvacIsAcOn", this.fullChangeListener);
        attach("hvacIsRearWindowHeating", this.fullChangeListener);
    }

    private void attach(String variableName, Variable.VariableChangeListener listener) {
        detach(variableName, listener);
        this.service.getVariableStore().subscribe(variableName, listener);
    }

    private void detachBubbleData() {
        detach("hvacTargetTemperature", this.bubbleListener);
        detach("hvacFanLevel", this.bubbleListener);
    }

    private void detachFullData() {
        detach("hvacIsAirductWindshield", this.fullChangeListener);
        detach("hvacIsAirductFace", this.fullChangeListener);
        detach("hvacIsAirductFeet", this.fullChangeListener);
        detach("hvacIsWindshieldHeating", this.fullChangeListener);
        detach("hvacIsRecirculation", this.fullChangeListener);
        detach("hvacIsAutomatic", this.fullChangeListener);
        detach("hvacIsAcOn", this.fullChangeListener);
        detach("hvacIsRearWindowHeating", this.fullChangeListener);
    }

    private void detach(String variableName, Variable.VariableChangeListener listener) {
        this.service.getVariableStore().unsubscribe(variableName, listener);
    }

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
        stopBroadCast();
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
