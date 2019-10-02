package de.jlab.cardroid.devices.serial.carduino;

import java.util.ArrayList;

public final class CarduinoErrorParser extends CarduinoPacketParser {

    private ArrayList<ErrorListener> errorHandlers = new ArrayList<>();

    @Override
    protected boolean shouldHandlePacket(CarduinoSerialPacket packet) {
        return CarduinoPacketType.ERROR.equals(packet.getPacketType());
    }

    @Override
    protected void handlePacket(CarduinoSerialPacket packet) {
        int errorNumber = packet.getPacketId();

        //Error[] errors = this.errors.values().toArray(new Error[0]);
        //Arrays.sort(errors, (error1, error2) -> (int)(error1.getLastOccurence() - error2.getLastOccurence()));
        for (ErrorListener handler : errorHandlers) {
            handler.onError(errorNumber);
        }
    }

    public void addListener(ErrorListener listener) {
        this.errorHandlers.add(listener);
    }

    public void removeListener(ErrorListener listener) {
        this.errorHandlers.remove(listener);
    }

    public interface ErrorListener {
        void onError(int errorNumber);
    }
}
