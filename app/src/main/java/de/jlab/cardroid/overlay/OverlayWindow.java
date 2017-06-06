package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
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
import android.widget.ToggleButton;

import java.lang.reflect.Field;
import java.util.Locale;

import de.jlab.cardroid.ClimateControlActivity;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.CarSystemSerialPacket;

public class OverlayWindow {
    private static final String LOG_TAG = "OverlayWindow";

    private Context context;

    private ConstraintLayout buttonContainer;
    private WindowManager windowManager;
    private View mFloatingView;

    public OverlayWindow(Context context) {
        this.context = context;
    }

    public void create() {
        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this.context)) {
            Intent dialogIntent = new Intent(this.context, ClimateControlActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.context.startActivity(dialogIntent);
        }

        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this.context).inflate(R.layout.view_overlay, null);

        //Add the view to the window.
        final WindowManager.LayoutParams params = getWindowLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );

        //Add the view to the window
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.windowManager.addView(mFloatingView, params);

        this.buttonContainer = (ConstraintLayout) mFloatingView.findViewById(R.id.buttonContainer);
        FrameLayout toggleContainer = (FrameLayout) mFloatingView.findViewById(R.id.toggleContainer);

        toggleContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        buttonContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        buttonContainer.setVisibility(View.GONE);
    }

    public void updateFromPacket(CarSystemSerialPacket packet) {
        int fanResourceId = getLevelImage((int) packet.readByte(1));
        String temperature = String.format(Locale.getDefault(), "%s", packet.readByte(2) / 2f);

        TextView mainTextView = (TextView) mFloatingView.findViewById(R.id.mainTextView);
        mainTextView.setText(temperature);
        ImageView mainImageView = (ImageView) mFloatingView.findViewById(R.id.mainImageView);
        mainImageView.setImageResource(fanResourceId);


        TextView temperatureTextView = (TextView) mFloatingView.findViewById(R.id.temperatureTextView);
        temperatureTextView.setText(temperature);

        ImageView fanButton = (ImageView) mFloatingView.findViewById(R.id.fanButton);
        fanButton.setImageResource(fanResourceId);
        TextView autoTextView = (TextView) mFloatingView.findViewById(R.id.automaticTextView);
        autoTextView.setVisibility(packet.readFlag(0, 6) ? View.VISIBLE : View.INVISIBLE);

        ImageView acButton = (ImageView) mFloatingView.findViewById(R.id.airConditioningButton);
        acButton.setImageResource(getStatusImage(packet.readFlag(0, 7)));

        ImageView recirculationButton = (ImageView) mFloatingView.findViewById(R.id.recirculationButton);
        recirculationButton.setImageResource(getStatusImage(packet.readFlag(0, 0)));

        ImageView windshieldHeaterButton = (ImageView) mFloatingView.findViewById(R.id.windshieldHeaterButton);
        windshieldHeaterButton.setImageResource(getStatusImage(packet.readFlag(0, 2)));

        ImageView rearHeaterButton = (ImageView) mFloatingView.findViewById(R.id.rearHeaterButton);
        rearHeaterButton.setImageResource(getStatusImage(packet.readFlag(0, 1)));

        /*
            windshieldDuctSwitch.setChecked(testBit(buffer[4], 5));
            faceDuctSwitch.setChecked(testBit(buffer[4], 4));
            feetDuctSwitch.setChecked(testBit(buffer[4], 3));
         */
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
        if (buttonContainer != null) {
            if (buttonContainer.getVisibility() == View.GONE) {
                buttonContainer.setVisibility(View.VISIBLE);
                windowManager.updateViewLayout(
                        mFloatingView,
                        getWindowLayout(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT
                        )
                );
            }
            else {
                buttonContainer.setVisibility(View.GONE);
                windowManager.updateViewLayout(
                        mFloatingView,
                        getWindowLayout(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT
                        )
                );
            }
        }
    }

    private WindowManager.LayoutParams getWindowLayout(int width, int height) {
        WindowManager.LayoutParams params =  new WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;

        return params;
    }

    public void destroy() {
        if (mFloatingView != null) {
            this.windowManager.removeView(mFloatingView);
        }
    }

}
