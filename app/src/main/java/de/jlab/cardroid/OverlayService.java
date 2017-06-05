package de.jlab.cardroid;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import de.jlab.cardroid.usb.CarSystemSerialPacket;
import de.jlab.cardroid.usb.CarduinoManager;
import de.jlab.cardroid.usb.SerialPacket;
import de.jlab.cardroid.usb.SerialReader;

/**
 * Created by rampage on 04.06.2017.
 */

public class OverlayService extends Service {
    private static final String LOG_TAG = "SerialReader";

    private WindowManager mWindowManager;
    private View mFloatingView;
    private Handler uiHandler;

    private ConstraintLayout buttonContainer;

    private CarduinoManager carduino;

    private SerialReader.SerialPacketListener listener = new SerialReader.SerialPacketListener() {
        @Override
        public void onReceivePackets(ArrayList<SerialPacket> packets) {
            Log.d(LOG_TAG, "Received " + packets.size() + " packets.");

            for (SerialPacket packet : packets) {
                if (packet instanceof CarSystemSerialPacket) {
                    final CarSystemSerialPacket carPacket = (CarSystemSerialPacket)packet;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] payload = carPacket.getPayload();

                            TextView debugTextView = (TextView) mFloatingView.findViewById(R.id.debugTextView);
                            debugTextView.setText(new String(payload) + ", " + Integer.toBinaryString(payload[0] & 0xFF) + ", " + bytesToHex(payload) + " (" + payload.length + ")");

                            ToggleButton acButton = (ToggleButton) mFloatingView.findViewById(R.id.acButton);
                            acButton.setChecked(carPacket.readFlag(0, 7));

                            ToggleButton autoButton = (ToggleButton) mFloatingView.findViewById(R.id.autoButton);
                            autoButton.setChecked(carPacket.readFlag(0, 6));
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
        SerialReader serialReader = new SerialReader();
        serialReader.addListener(this.listener);
        this.carduino.addListener(serialReader);

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

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SERVICE", "ONSTARTCOMMAND");

        this.carduino.connect();

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
