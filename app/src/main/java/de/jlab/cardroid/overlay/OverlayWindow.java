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
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.Locale;

import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.car.CarSystem;
import de.jlab.cardroid.car.ClimateControl;

public class OverlayWindow implements CarSystem.ChangeListener<ClimateControl> {
    private static final String LOG_TAG = "OverlayWindow";

    private Handler uiHandler;
    private Context context;

    private WindowManager windowManager;

    private ClimateControl climateControl;

    static class ViewHolder {
        View rootView;

        View toggleButton;
        ConstraintLayout detailView;

        TextView mainTextView;
        ImageView mainImageView;
        TextView temperatureTextView;
        ImageView fanButton;
        TextView autoTextView;
        ImageView acButton;
        ImageView recirculationButton;
        ImageView windshieldHeaterButton;
        ImageView rearHeaterButton;
    }
    private ViewHolder viewHolder = new ViewHolder();


    public OverlayWindow(Context context, ClimateControl climateControl) {
        this.context = context;
        this.climateControl = climateControl;
    }

    public void create() {
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
        viewHolder.toggleButton = viewHolder.rootView.findViewById(R.id.toggleContainer);
        viewHolder.detailView = (ConstraintLayout) viewHolder.rootView.findViewById(R.id.buttonContainer);
        viewHolder.mainTextView = (TextView) viewHolder.rootView.findViewById(R.id.mainTextView);
        viewHolder.mainImageView = (ImageView) viewHolder.rootView.findViewById(R.id.mainImageView);
        viewHolder.temperatureTextView = (TextView) viewHolder.rootView.findViewById(R.id.temperatureTextView);
        viewHolder.fanButton = (ImageView) viewHolder.rootView.findViewById(R.id.fanButton);
        viewHolder.autoTextView = (TextView) viewHolder.rootView.findViewById(R.id.automaticTextView);
        viewHolder.acButton = (ImageView) viewHolder.rootView.findViewById(R.id.airConditioningButton);
        viewHolder.recirculationButton = (ImageView) viewHolder.rootView.findViewById(R.id.recirculationButton);
        viewHolder.windshieldHeaterButton = (ImageView) viewHolder.rootView.findViewById(R.id.windshieldHeaterButton);
        viewHolder.rearHeaterButton = (ImageView) viewHolder.rootView.findViewById(R.id.rearHeaterButton);

        // Add rootview to overlay window
        final WindowManager.LayoutParams params = getWindowLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );
        this.windowManager.addView(viewHolder.rootView, params);

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

        // Init events


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
        if (this.viewHolder.rootView != null) {
            this.windowManager.removeView(this.viewHolder.rootView);
        }
        this.climateControl.removeChangeListener(this);
    }

    @Override
    public void onChange(final ClimateControl system) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int fanResourceId = getLevelImage(system.getFanLevel());
                String temperature = String.format(Locale.getDefault(), "%s", system.getTemperature());

                viewHolder.mainTextView.setText(temperature);
                viewHolder.mainImageView.setImageResource(fanResourceId);
                viewHolder.temperatureTextView.setText(temperature);
                viewHolder.fanButton.setImageResource(fanResourceId);
                viewHolder.autoTextView.setVisibility(system.isAuto() ? View.VISIBLE : View.INVISIBLE);
                viewHolder.acButton.setImageResource(getStatusImage(system.isAcOn()));
                viewHolder.recirculationButton.setImageResource(getStatusImage(system.isRecirculation()));
                viewHolder.windshieldHeaterButton.setImageResource(getStatusImage(system.isWindshieldHeating()));
                viewHolder.rearHeaterButton.setImageResource(getStatusImage(system.isRearWindowHeating()));
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }
}
