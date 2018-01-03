package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.Locale;

import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.ClimateControl;

public class OverlayWindow implements CarSystem.ChangeListener<ClimateControl> {
    private static final String LOG_TAG = "OverlayWindow";

    private boolean isAttached = false;

    private Handler uiHandler;
    private Context context;

    private WindowManager windowManager;
    private ClimateControl climateControl;

    private boolean trackingFans = false;
    private boolean trackingTemp = false;

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
        ImageView mainFanIcon;

        View detailView;

        OverlayToggleButton offButton;
        OverlayToggleButton wshButton;
        OverlayToggleButton rwhButton;
        OverlayToggleButton recirculationButton;

        View modeButton;
        View modeDisplay;
        View modeFaceIcon;
        View modeFeetIcon;
        View modeWsIcon;
        SeekBar fanBar;
        TextView fanChangeText;
        OverlayToggleButton autoButton;

        SeekBar temperatureBar;
        TextView temperatureText;
        TextView temperatureChangeText;
        OverlayToggleButton acButton;
    }
    private ViewHolder viewHolder = new ViewHolder();


    public OverlayWindow(Context context, ClimateControl climateControl) {
        this.context = context;
        this.climateControl = climateControl;
    }

    public void create() {
        if (viewHolder.rootView != null) {
            this.destroy();
        }

        this.uiHandler = new Handler();

        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);

        // Request user to grant permission for drawing if needed and bail out
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this.context)) {
            Intent permissionIntent = new Intent(this.context, SettingsActivity.class);
            permissionIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.OverlayPreferenceFragment.class.getName());
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.context.startActivity(permissionIntent);
            return;
        }

        // initialize views
        viewHolder.rootView = LayoutInflater.from(this.context).inflate(R.layout.view_overlay, null);

        viewHolder.toggleButton = viewHolder.rootView.findViewById(R.id.toggleButton);
        viewHolder.mainText = (TextView)viewHolder.rootView.findViewById(R.id.mainText);
        viewHolder.mainFanIcon = (ImageView)viewHolder.rootView.findViewById(R.id.mainFanIcon);

        viewHolder.detailView = viewHolder.rootView.findViewById(R.id.detailContainer);

        viewHolder.offButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.offButton);
        viewHolder.wshButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.wshButton);
        viewHolder.rwhButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.rwhButton);
        viewHolder.recirculationButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.recirculationButton);

        viewHolder.modeButton = viewHolder.rootView.findViewById(R.id.modeButton);
        viewHolder.modeDisplay = viewHolder.rootView.findViewById(R.id.modeDisplay);
        viewHolder.modeFaceIcon = viewHolder.rootView.findViewById(R.id.ductFace);
        viewHolder.modeFeetIcon = viewHolder.rootView.findViewById(R.id.ductFeet);
        viewHolder.modeWsIcon = viewHolder.rootView.findViewById(R.id.ductWindshield);
        viewHolder.fanChangeText = (TextView)viewHolder.rootView.findViewById(R.id.fanChangeText);
        viewHolder.fanBar =(SeekBar)viewHolder.rootView.findViewById(R.id.fanBar);
        viewHolder.autoButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.autoButton);

        viewHolder.temperatureBar = (SeekBar)viewHolder.rootView.findViewById(R.id.temperatureBar);
        viewHolder.temperatureText = (TextView)viewHolder.rootView.findViewById(R.id.temperatureText);
        viewHolder.temperatureChangeText = (TextView)viewHolder.rootView.findViewById(R.id.temperatureChangeText);
        viewHolder.acButton = (OverlayToggleButton)viewHolder.rootView.findViewById(R.id.acButton);

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
                        climateControl.pushOffButton();
                        break;
                    case R.id.wshButton:
                        climateControl.pushWindshieldHeatingButton();
                        break;
                    case R.id.rwhButton:
                        climateControl.pushRearWindowHeatingButton();
                        break;
                    case R.id.recirculationButton:
                        climateControl.pushRecirculationButton();
                        break;
                    case R.id.modeButton:
                        climateControl.pushModeButton();
                        break;
                    case R.id.autoButton:
                        climateControl.pushAutoButton();
                        break;
                    case R.id.acButton:
                        climateControl.pushAcButton();
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
        viewHolder.fanBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 1;
                viewHolder.fanChangeText.setText(String.format(Locale.getDefault(), "%s", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                viewHolder.modeDisplay.setVisibility(View.INVISIBLE);
                viewHolder.fanChangeText.setVisibility(View.VISIBLE);
                uiHandler.removeCallbacks(stopFanInteraction);
                trackingFans = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                climateControl.setFanLevel((byte)(seekBar.getProgress() + 1));
                uiHandler.postDelayed(stopFanInteraction, 1000);
            }
        });

        viewHolder.temperatureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = (progress + 32) / 2f;
                viewHolder.temperatureChangeText.setText(String.format(Locale.getDefault(), "%s", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                viewHolder.temperatureText.setVisibility(View.INVISIBLE);
                viewHolder.temperatureChangeText.setVisibility(View.VISIBLE);
                uiHandler.removeCallbacks(stopTempInteraction);
                trackingTemp = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                climateControl.setTemperature((seekBar.getProgress() + 32) / 2f);
                uiHandler.postDelayed(stopTempInteraction, 1000);
            }
        });

        // Set up initial view state
        viewHolder.detailView.setVisibility(View.GONE);

        this.climateControl.addChangeListener(this);
    }

    private int getStatusImage(boolean status) {
        return status ? R.mipmap.ic_shortcut_status_on : R.mipmap.ic_shortcut_status_off;
    }

    private int getLevelImage(int level) {
        String name = "ic_shortcut_level_" + level;
        try {
            Field field = R.mipmap.class.getField(name);
            return field.getInt(null);
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Cannot find level image " + name);
            return R.mipmap.ic_shortcut_level_0;
        }
    }

    public void toggle() {
        if (this.viewHolder.detailView != null) {
            if (this.viewHolder.detailView.getVisibility() == View.GONE) {
                this.viewHolder.detailView.setVisibility(View.VISIBLE);
                windowManager.updateViewLayout(
                        this.viewHolder.rootView,
                        getWindowLayout(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT
                        )
                );
            }
            else {
                this.viewHolder.detailView.setVisibility(View.GONE);
                windowManager.updateViewLayout(
                        this.viewHolder.rootView,
                        getWindowLayout(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT
                        )
                );
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

        WindowManager.LayoutParams params =  new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        return params;
    }

    public void destroy() {
        if (this.isAttached) {
            this.windowManager.removeView(this.viewHolder.rootView);
            this.isAttached = false;
        }
        this.climateControl.removeChangeListener(this);
    }

    @Override
    public void onChange(final ClimateControl system) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int fanLevel = system.getFanLevel();
                int fanResourceId = getLevelImage(fanLevel);
                float temperature = system.getTemperature();
                boolean isOff = fanLevel == 0;
                String temperatureText = String.format(Locale.getDefault(), "%s", temperature);

                viewHolder.mainText.setText(temperatureText);
                viewHolder.mainFanIcon.setImageResource(fanResourceId);

                viewHolder.offButton.setState(!isOff);
                viewHolder.wshButton.setState(system.isWindshieldHeating());
                viewHolder.rwhButton.setState(system.isRearWindowHeating());
                viewHolder.recirculationButton.setState(system.isRecirculation());

                if (!trackingFans) {
                    if (isOff) {
                        viewHolder.modeDisplay.setVisibility(View.INVISIBLE);
                        viewHolder.fanChangeText.setVisibility(View.VISIBLE);
                        viewHolder.fanChangeText.setText(R.string.cc_off);
                    } else {
                        viewHolder.modeDisplay.setVisibility(View.VISIBLE);
                        viewHolder.fanChangeText.setVisibility(View.INVISIBLE);
                        viewHolder.modeFaceIcon.setVisibility(system.isDuctFaceActive() ? View.VISIBLE : View.INVISIBLE);
                        viewHolder.modeFeetIcon.setVisibility(system.isDuctFeetActive() ? View.VISIBLE : View.INVISIBLE);
                        viewHolder.modeWsIcon.setVisibility(system.isDuctWindshieldActive() ? View.VISIBLE : View.INVISIBLE);
                    }
                    viewHolder.fanBar.setProgress(Math.max(0, fanLevel - 1));
                }
                viewHolder.autoButton.setState(system.isAuto());

                if (!trackingTemp) {
                    viewHolder.temperatureText.setText(temperatureText);
                    viewHolder.temperatureBar.setProgress((int) ((temperature - 16) * 2));
                }
                viewHolder.acButton.setState(system.isAcOn());
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }
}
