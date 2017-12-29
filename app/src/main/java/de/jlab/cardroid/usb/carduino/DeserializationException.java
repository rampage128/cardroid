package de.jlab.cardroid.usb.carduino;

class DeserializationException extends Exception {
    public DeserializationException(String message) {
        super(message);
    }

    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
