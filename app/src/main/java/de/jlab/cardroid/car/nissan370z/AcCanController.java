package de.jlab.cardroid.car.nissan370z;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.car.CanInteractable;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceController;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;

public final class AcCanController {

    private byte roll = 0x00;

    private int buttonBits = 0x00;

    private ByteBuffer canPacket540 = ByteBuffer.wrap(new byte[8]);
    private ByteBuffer canPacket541 = ByteBuffer.wrap(new byte[8]);
    private ByteBuffer canPacket542 = ByteBuffer.wrap(new byte[8]);

    private DeviceController deviceController;
    private Timer broadCastTimer;
    private Device.FeatureChangeObserver<CanInteractable> writeFilter;

    public AcCanController(@NonNull DeviceController deviceController, @Nullable DeviceUid deviceUid) {
        this.deviceController = deviceController;

        this.canPacket540.put(0, (byte)0x20);
        this.canPacket540.put(1, (byte)0x64);

        this.canPacket542.put(0, (byte)0x18);
        this.canPacket542.put(1, (byte)0x2F);

        this.writeFilter = this::canWriterStateChange;

        deviceController.subscribeFeature(this.writeFilter, CanInteractable.class, deviceUid);
    }

    public void dispose() {
        this.deviceController.unsubscribeFeature(this.writeFilter, CanInteractable.class);
        this.stopBroadCast();
    }

    public void pushOffButton() {
        flipButtonBit(23);
        //this.buttonBits.flip(23);
        this.updateButtonBits();
    }

    public void pushWindshieldButton() {
        flipButtonBit(7);
        this.updateButtonBits();
    }

    public void pushRearHeaterButton() {
        setButtonBit(8);
        this.updateButtonBits();
    }

    private void resetRearHeaterButton() {
        clearButtonBit(8);
        this.updateButtonBits();
    }

    public void pushRecirculationButton() {
        flipButtonBit(14);
        this.updateButtonBits();
    }

    public void pushModeButton() {
        flipButtonBit(3);
        this.updateButtonBits();
    }

    public void pushAutoButton() {
        flipButtonBit(24);
        this.updateButtonBits();
    }

    public void pushAcButton() {
        flipButtonBit(21);
        this.updateButtonBits();
    }

    public void changeFanLevel(int newLevel) {
        this.canPacket542.put(0, (byte)(newLevel * 8));
        flipButtonBit(27);
        this.updateButtonBits();
    }

    public void changeTargetTemperature(int newTemperature) {
        this.canPacket542.put(1, (byte)newTemperature);
        flipButtonBit(16);
        this.updateButtonBits();
    }

    private void flipButtonBit(int index) {
        this.buttonBits ^= 0x01 << 31 - index;
    }

    private void setButtonBit(int index) {
        this.buttonBits |= 0x01 << 31 - index;
    }

    private void clearButtonBit(int index) {
        this.buttonBits &= ~(0x01 << 31 - index);
    }

    private void updateButtonBits() {
        this.canPacket541.putInt(0, this.buttonBits);
    }

    public void broadcast(CanInteractable interactable) {
        interactable.sendPacket(0x540, this.canPacket540.array());
        interactable.sendPacket(0x541, this.canPacket541.array());
        interactable.sendPacket(0x542, this.canPacket542.array());

        this.resetRearHeaterButton();

        this.roll();
    }

    private void roll() {
        this.canPacket540.put(7, this.roll);
        this.canPacket541.put(7, this.roll);
        this.canPacket542.put(7, this.roll);

        if (this.roll < 3) {
            this.roll += 1;
        } else {
            this.roll = 0;
        }
    }

    private void canWriterStateChange(@NonNull CanInteractable interactable, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            this.startBroadCast(interactable);
        } else {
            this.stopBroadCast();
        }
    }

    private void startBroadCast(@NonNull CanInteractable interactable) {
        if (this.broadCastTimer == null) {
            this.broadCastTimer = new Timer();
            this.broadCastTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    AcCanController.this.broadcast(interactable);
                }
            }, 0, 250);
        }
    }

    private void stopBroadCast() {
        if (this.broadCastTimer != null) {
            this.broadCastTimer.cancel();
            this.broadCastTimer = null;
        }
    }

}
