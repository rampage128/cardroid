package de.jlab.cardroid;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by rampage on 04.06.2017.
 */

public class OverlayService extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;
    private Handler uiHandler;

    private CarduinoManager carduino;
    private CarduinoManager.CarduinoListener listener = new CarduinoManager.CarduinoListener() {
        @Override
        public void onReceiveData(final byte[] buffer) {
            final int numBytesRead = buffer.length;

            if (numBytesRead > 6) {

                Log.d("RUNNER", new String(buffer) + ", " + Integer.toBinaryString(buffer[4] & 0xFF) + ", " + bytesToHex(buffer) + " (" + numBytesRead + ")");

                final TextView debugTextView = (TextView) mFloatingView.findViewById(R.id.debugTextView);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        debugTextView.setText(new String(buffer) + ", " + Integer.toBinaryString(buffer[4] & 0xFF) + ", " + bytesToHex(buffer) + " (" + numBytesRead + ")");
                    }
                });

                /*
                final TextView debugTextView = (TextView) findViewById(R.id.debugTextView);
                final Switch airConditioningSwitch = (Switch) findViewById(R.id.airConditioningSwitch);
                final Switch automaticSwitch = (Switch) findViewById(R.id.automaticSwitch);
                final Switch windshieldDuctSwitch = (Switch) findViewById(R.id.windshieldDuctSwitch);
                final Switch faceDuctSwitch = (Switch) findViewById(R.id.faceDuctSwitch);
                final Switch feetDuctSwitch = (Switch) findViewById(R.id.feetDuctSwitch);
                final Switch windshieldHeatingSwitch = (Switch) findViewById(R.id.windshieldHeatingSwitch);
                final Switch rearWindowHeatingSwitch = (Switch) findViewById(R.id.rearWindowHeatingSwitch);
                final Switch recirculationSwitch = (Switch) findViewById(R.id.recirculationSwitch);
                final TextView temperatureTextView = (TextView) findViewById(R.id.temperatureTextView);
                final TextView fanLevelTextView = (TextView) findViewById(R.id.fanLevelTextView);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        debugTextView.setText(new String(buffer) + ", " + Integer.toBinaryString(buffer[4] & 0xFF) + ", " + bytesToHex(buffer) + " (" + numBytesRead + ")");
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
                    }
                });
                */
            }
        }

        private final char[] hexArray = "0123456789ABCDEF".toCharArray();
        private String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 3];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 3] = hexArray[v >>> 4];
                hexChars[j * 3 + 1] = hexArray[v & 0x0F];
                hexChars[j * 3 + 2] = ' ';
            }
            return new String(hexChars);
        }

        private boolean testBit(int value, int bitNum) {
            return (value & (1<<bitNum)) != 0;
        }

        @Override
        public void onConnect() {
            Log.d(this.getClass().getSimpleName(), "USB CONNECTED!");
        }

        @Override
        public void onDisconnect() {
            Log.d(this.getClass().getSimpleName(), "USB DISCONNECTED!");
        }
    };

    public OverlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uiHandler = new Handler();
//Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent dialogIntent = new Intent(this, ClimateControlActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
        }

        //Inflate the floating view layout we created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.view_overlay, null);

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
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        this.carduino = new CarduinoManager(this);
        this.carduino.addListener(this.listener);

        this.carduino.connect();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
        this.carduino.disconnect();
    }

    private void runOnUiThread(Runnable runnable) {
        uiHandler.post(runnable);
    }
}
