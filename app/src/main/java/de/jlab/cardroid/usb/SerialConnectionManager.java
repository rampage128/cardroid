package de.jlab.cardroid.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SerialConnectionManager {
    private static final String LOG_TAG = "SerialConnection";

    private static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private Context context;

    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serial;

    private UsageStatistics bandWidthUsage = new UsageStatistics(1000, 60);
    /*
    private UsageStatistics.UsageStatisticsListener bandWidthLogWriter = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(int count, UsageStatistics statistics) {
            Log.d(LOG_TAG, "Bandwidth: " + count + "bps / Avg: " + Math.round(bandWidthUsage.getAverage()) + "bps (" + Math.round(bandWidthUsage.getAverageReliability() * 100f) + "%) / Usage: " + Math.round(100f / 11520 * count) + "%");
        }
    };
    */

    private ArrayList<SerialConnectionListener> listeners = new ArrayList<>();

    private UsbSerialInterface.UsbReadCallback readCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] bytes) {
            bandWidthUsage.count(bytes.length);
            for (SerialConnectionListener listener : listeners) {
                listener.onReceiveData(bytes);
            }
        }
    };

    public SerialConnectionManager(Context context) {
        this.context = context;
        //this.bandWidthUsage.addListener(this.bandWidthLogWriter);
    }

    /**
     * Connects to the carduino USB-Device
     * @return true if connection was established or false if not
     */
    public boolean connect(UsbDevice device) {
        UsbManager manager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        if (manager == null) {
            return false;
        }

        this.device = device;
        if (this.device == null) {
            this.device = findDevice(manager);
            if (this.device == null) {
                return false;
            }
        }

        Log.d(LOG_TAG, this.device.toString());

        if (!manager.hasPermission(this.device)) {
            PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                    ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

            manager.requestPermission(this.device, mPermissionIntent);
            if (!manager.hasPermission(this.device)) {
                return false;
            }
        }

        connection = manager.openDevice(this.device);
        if (connection == null) {
            return false;
        }

        createSerialDevice();

        for (SerialConnectionListener listener : listeners) {
            listener.onConnect();
        }
        return true;
    }

    private UsbDevice findDevice(UsbManager manager) {
        UsbDevice device = null;

        HashMap<String, UsbDevice> usbDevices = new HashMap<>();
        try {
            usbDevices = manager.getDeviceList();
        } catch (Exception e) {
            // Usually there will only be an exception on emulators.
            // But maybe weird device implementations exist aswell?
            Log.e(LOG_TAG, "USB not supported", e);
            return null;
        }

        if (usbDevices.isEmpty()) {
            return null;
        }

        for(Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            UsbDevice deviceEntry = device = entry.getValue();
            int deviceVID = device.getVendorId();
            int devicePID = device.getProductId();
            if (deviceVID == 0x1a86 && devicePID == 0x7523) {
                device = deviceEntry;
            }
        }

        return device;
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

        for (SerialConnectionListener listener : listeners) {
            listener.onDisconnect();
        }
    }

    public boolean isConnected() {
        return this.connection != null && this.serial != null && this.device != null;
    }

    public boolean sendPacket(SerialPacket packet) {
        if (!this.isConnected()) {
            return false;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(SerialPacketStructure.HEADER);
            SerialPacketFactory.serialize(packet, outputStream);
            outputStream.write(SerialPacketStructure.FOOTER);
            byte[] data = outputStream.toByteArray();
            this.serial.write(data);
            return true;
        }
        catch (IOException e) {
            Log.e(LOG_TAG, "Error serializing packet " + packet.getClass().getSimpleName(), e);
            return false;
        } catch (UnknownPacketTypeException e) {
            throw new UnsupportedOperationException("Packet " + packet.getClass().getCanonicalName() + " cannot be sent.", e);
        }
    }

    private boolean createSerialDevice() {
        if (this.device == null || this.connection == null) {
            return false;
        }

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

    public void requestBaudRate(int baudRate) {
        Log.d(LOG_TAG, "Requesting baudRate " + baudRate);
        byte[] payload = ByteBuffer.allocate(4).putInt(baudRate).array();
        this.sendPacket(MetaEvent.serialize(MetaEvent.CHANGE_BAUD_RATE, payload));
    }

    public void setBaudRate(int baudRate) {
        this.serial.setBaudRate(baudRate);
    }

    public void addBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
        this.bandWidthUsage.addListener(listener);
    }

    public void removeBandwidthStatisticsListener(UsageStatistics.UsageStatisticsListener listener) {
        this.bandWidthUsage.removeListener(listener);
    }

    public void addConnectionListener(SerialConnectionListener listener) {
        this.listeners.add(listener);
    }

    public void removeConnectionListener(SerialConnectionListener listener) {
        this.listeners.remove(listener);
    }

    public interface SerialConnectionListener {
        void onReceiveData(byte[] data);
        void onConnect();
        void onDisconnect();
    }
}
