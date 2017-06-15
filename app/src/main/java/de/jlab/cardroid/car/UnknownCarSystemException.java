package de.jlab.cardroid.car;

public class UnknownCarSystemException extends Exception {
    UnknownCarSystemException(byte identifier) {
        super("Unknown car system type 0x" + byteToHexString(identifier));
    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String byteToHexString(byte data) {
        char[] hexChars = new char[2];
        int v = data & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }
}
