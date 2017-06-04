package de.jlab.cardroid;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rampage on 04.06.2017.
 */

public class CarduinoManager {

    private static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private Context context;

    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serial;

    private ArrayList<CarduinoListener> listeners = new ArrayList<>();

    private UsbSerialInterface.UsbReadCallback readCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            for (CarduinoListener listener : listeners) {
                listener.onReceiveData(bytes);
            }
        }
    };

    public CarduinoManager(Context context) {
        this.context = context;
    }

    /**
     * Connects to the carduino USB-Device
     * @return true if connection was established or false if not
     */
    public boolean connect() {
        UsbManager manager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = manager.getDeviceList();
        if(usbDevices.isEmpty()) {
            return false;
        }

        for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            device = entry.getValue();
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();
            if (deviceVID == 0x1a86 && devicePID == 0x7523) {
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                        ACTION_USB_PERMISSION), 0);
                IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
                manager.requestPermission(device, mPermissionIntent);
                boolean hasPermision = manager.hasPermission(device);

                connection = manager.openDevice(device);
                if (!hasPermision || connection == null) {
                    return false;
                }

                createSerialDevice();

                for (CarduinoListener listener : listeners) {
                    listener.onConnect();
                }
                return true;
            }
        }

        return false;
    }

    public void disconnect() {
        if (this.serial != null) {
            this.serial.close();
            this.serial = null;
        }
        if (this.connection != null) {
            this.connection.close();
            this.connection = null;
        }
        this.device = null;

        for (CarduinoListener listener : listeners) {
            listener.onDisconnect();
        }
    }

    private boolean createSerialDevice() {
        this.serial = UsbSerialDevice.createUsbSerialDevice(device, connection);
        // SERIALPORT WILL BE NULL IF NO DRIVER IS FOUND
        if(this.serial != null) {
            // SERIALPORT WILL NOT OPEN IF THERE IS AN I/O ERROR OR THE WRONG DRIVER WAS CHOSEN
            if (this.serial.open()) {
                this.serial.setBaudRate(115200);
                this.serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                this.serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                this.serial.setParity(UsbSerialInterface.PARITY_NONE);
                this.serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                this.serial.read(readCallback);

                return true;
            }
        }

        return false;
    }

    public void addListener(CarduinoListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(CarduinoListener listener) {
        this.listeners.remove(listener);
    }

    public interface CarduinoListener {
        void onReceiveData(byte[] arg0);
        void onConnect();
        void onDisconnect();
    }

}
