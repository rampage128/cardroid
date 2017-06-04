package de.jlab.cardroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

public class ClimateControlActivity extends AppCompatActivity {

    private CarduinoManager carduino;
    private CarduinoManager.CarduinoListener listener = new CarduinoManager.CarduinoListener() {
        @Override
        public void onReceiveData(final byte[] buffer) {
            final int numBytesRead = buffer.length;

            if (numBytesRead > 6) {

                Log.d("RUNNER", new String(buffer) + ", " + Integer.toBinaryString(buffer[4] & 0xFF) + ", " + bytesToHex(buffer) + " (" + numBytesRead + ")");

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
/*
    final Handler handler = new Handler();
    private Runnable runner = new Runnable(){
        @Override
        public void run() {
            byte buffer[] = new byte[8];
            try {
                int numBytesRead = port.read(buffer, 1000);

                TextView debugTextView = (TextView) findViewById(R.id.debugTextView);
                debugTextView.setText(new String(buffer) + ", " + Integer.toBinaryString(buffer[4] & 0xFF) + ", " + bytesToHex(buffer) + " (" + numBytesRead + ")");

                if (numBytesRead > 0) {
                    Log.d("RUNNER", new String(buffer) + ", " + Integer.toBinaryString(buffer[4] & 0xFF) + ", " + bytesToHex(buffer) + " (" + numBytesRead + ")");

                    Switch airConditioningSwitch = (Switch) findViewById(R.id.airConditioningSwitch);
                    airConditioningSwitch.setChecked(testBit(buffer[4], 7));

                    Switch automaticSwitch = (Switch) findViewById(R.id.automaticSwitch);
                    automaticSwitch.setChecked(testBit(buffer[4], 6));

                    Switch windshieldDuctSwitch = (Switch) findViewById(R.id.windshieldDuctSwitch);
                    windshieldDuctSwitch.setChecked(testBit(buffer[4], 5));

                    Switch faceDuctSwitch = (Switch) findViewById(R.id.faceDuctSwitch);
                    faceDuctSwitch.setChecked(testBit(buffer[4], 4));

                    Switch feetDuctSwitch = (Switch) findViewById(R.id.feetDuctSwitch);
                    feetDuctSwitch.setChecked(testBit(buffer[4], 3));

                    Switch windshieldHeatingSwitch = (Switch) findViewById(R.id.windshieldHeatingSwitch);
                    windshieldHeatingSwitch.setChecked(testBit(buffer[4], 2));

                    Switch rearWindowHeatingSwitch = (Switch) findViewById(R.id.rearWindowHeatingSwitch);
                    rearWindowHeatingSwitch.setChecked(testBit(buffer[4], 1));

                    Switch recirculationSwitch = (Switch) findViewById(R.id.recirculationSwitch);
                    recirculationSwitch.setChecked(testBit(buffer[4], 0));

                    TextView temperatureTextView = (TextView) findViewById(R.id.temperatureTextView);
                    temperatureTextView.setText(Float.toString((buffer[6] / 2f)));

                    TextView fanLevelTextView = (TextView) findViewById(R.id.fanLevelTextView);
                    fanLevelTextView.setText(Integer.toString((int) buffer[5]));
                }

                handler.postDelayed(this, 500); // set time here to refresh textView
            } catch (IOException e) {
                Log.e("RUNNER", "Error receiving data", e);
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

    private UsbManager manager;
    private UsbSerialDriver driver;
    private UsbDeviceConnection connection;
    private UsbSerialPort port;
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_climate_control);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(this.getClass().getSimpleName(), "Resuming");

        this.carduino = new CarduinoManager(this);
        this.carduino.addListener(this.listener);

        this.carduino.connect();

/*
        this.manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(this.manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        this.driver = availableDrivers.get(0);
        this.connection = this.manager.openDevice(this.driver.getDevice());
        if (this.connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }

        // Read some data! Most have just one port (port 0).
        this.port = this.driver.getPorts().get(0);
        try {
            this.port.open(this.connection);
            this.port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            this.port.setDTR(false);

            handler.postDelayed(runner, 1000);
            Log.d(this.getClass().getSimpleName(), "USB Initialization successful ... starting handler");
        } catch (IOException e) {
            try {
                this.port.close();
            } catch (IOException e2) {
                e.printStackTrace();
            }
        }
*/
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(this.getClass().getSimpleName(), "Pausing");

        this.carduino.disconnect();

/*
        handler.removeCallbacks(runner);
        try {
            this.port.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
    }
}
