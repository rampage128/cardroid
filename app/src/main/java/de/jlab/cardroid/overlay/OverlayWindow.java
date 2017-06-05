package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import de.jlab.cardroid.ClimateControlActivity;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.CarSystemSerialPacket;

public class OverlayWindow {

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
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 0;

        //Add the view to the window
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.windowManager.addView(mFloatingView, params);

        this.buttonContainer = (ConstraintLayout) mFloatingView.findViewById(R.id.buttonContainer);
        final TextView debugTextView = (TextView) mFloatingView.findViewById(R.id.debugTextView);

        debugTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonContainer.getVisibility() == View.GONE) {
                    buttonContainer.setVisibility(View.VISIBLE);
                }
                else {
                    buttonContainer.setVisibility(View.GONE);
                }
            }
        });

        buttonContainer.setVisibility(View.GONE);
    }

    public void updateFromPacket(CarSystemSerialPacket packet) {
        ToggleButton acButton = (ToggleButton) mFloatingView.findViewById(R.id.acButton);
        acButton.setChecked(packet.readFlag(0, 7));

        ToggleButton autoButton = (ToggleButton) mFloatingView.findViewById(R.id.autoButton);
        autoButton.setChecked(packet.readFlag(0, 6));

        /*
            airConditioningSwitch.setChecked(testBit(buffer[4], 7));
            automaticSwitch.setChecked(testBit(buffer[4], 6));
            windshieldDuctSwitch.setChecked(testBit(buffer[4], 5));
            faceDuctSwitch.setChecked(testBit(buffer[4], 4));
            feetDuctSwitch.setChecked(testBit(buffer[4], 3));
            windshieldHeatingSwitch.setChecked(testBit(buffer[4], 2));
            rearWindowHeatingSwitch.setChecked(testBit(buffer[4], 1));
            recirculationSwitch.setChecked(testBit(buffer[4], 0));
            temperatureTextView.setText(Float.toString((buffer[6] / 2f)));
            fanLevelTextView.setText(Integer.toString((int) buffer[5]));
         */
    }

    public void destroy() {
        if (mFloatingView != null) {
            this.windowManager.removeView(mFloatingView);
        }
    }

}
