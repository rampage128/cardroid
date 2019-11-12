package de.jlab.cardroid.devices.usb.serial.carduino;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.usb.serial.UsbSerialDevice;

public final class KeepAlive {

    private long timeout;
    private long interval;
    private UsbSerialDevice device;
    private byte[] keepAliveData;

    private Handler handler;
    private Runnable runner = this::send;

    public KeepAlive(long timeout, long interval, @NonNull byte[] keepAliveData, @NonNull UsbSerialDevice device, @NonNull Context context) {
        this.timeout = timeout;
        this.interval = interval;
        this.device = device;
        this.keepAliveData = keepAliveData;
        this.handler = new Handler(context.getMainLooper());
    }

    public void start() {
        this.run(this.timeout);
    }

    public void stop() {
        this.handler.removeCallbacks(runner);
    }

    private void run(long delay) {
        this.stop();
        if (this.device.getState() != Device.State.INVALID) {
            this.handler.postDelayed(runner, delay);
        }
    }

    private void send() {
        this.device.send(this.keepAliveData);
        this.run(this.interval);
    }

}
