package de.jlab.cardroid.usb.carduino.serial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import androidx.annotation.NonNull;
import de.jlab.cardroid.usb.carduino.CarduinoService;

public class ErrorPacketHandler implements CarduinoService.PacketHandler<SerialErrorPacket> {
    private LinkedHashMap<Byte, Error> errors = new LinkedHashMap<>();

    private ArrayList<ErrorListener> errorHandlers = new ArrayList<>();

    @Override
    public void handleSerialPacket(@NonNull SerialErrorPacket packet) {
        byte errorNumber = packet.getId();
        Error error = this.errors.get(errorNumber);
        if (error == null) {
            error = new Error(errorNumber);
            this.errors.put(errorNumber, error);
        }
        error.count();

        Error[] errors = this.errors.values().toArray(new Error[0]);
        Arrays.sort(errors, (error1, error2) -> (int)(error1.getLastOccurence() - error2.getLastOccurence()));
        for (ErrorListener handler : errorHandlers) {
            handler.onError(error, errors);
        }
    }

    @Override
    public boolean shouldHandlePacket(@NonNull SerialPacket packet) {
        return packet instanceof SerialErrorPacket;
    }

    public void addListener(ErrorListener listener) {
        this.errorHandlers.add(listener);
    }

    public void removeListener(ErrorListener listener) {
        this.errorHandlers.remove(listener);
    }

    public class Error {
        private byte errorNumber;
        private long lastOccurence;
        private int count;

        public Error(byte errorNumber) {
            this.errorNumber = errorNumber;
        }

        private void count() {
            this.lastOccurence = System.currentTimeMillis();
            this.count++;
        }

        public long getLastOccurence() {
            return lastOccurence;
        }

        public int getCount() {
            return this.count;
        }

        public String getErrorCode() {
            return String.format("%02X", 0xFF & this.errorNumber);
        }
    }

    public interface ErrorListener {
        void onError(Error error, Error[] errors);
    }
}
