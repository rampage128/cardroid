package de.jlab.cardroid.devices.serial.can;

import android.util.Log;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

public class CanPacketTest {

    public byte[] testBytes = { (byte)0B01000011, (byte)0B00111100, (byte)0B10101111 };
    public byte[] shortOffset4 = {(byte)0B00110011, (byte)0B11001010};
    @Test
    public void readBigEndian() {
        CanPacket p = new CanPacket(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.BIG_ENDIAN);
        short expectedResult = bytes.getShort();
        long result = p.readBigEndian(4, 16);
        assertTrue(result == expectedResult);
    }

    @Test
    public void readLittleEndian() {
        CanPacket p = new CanPacket(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        short expectedResult = bytes.getShort();
        long result = p.readLittleEndian(4, 16);
        assertTrue(result == expectedResult);
    }

    @Test
    public void originalImplementationLittle() {
        CanPacket p = new CanPacket(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readOriginal(4, 16, ByteOrder.LITTLE_ENDIAN);
        assertTrue(result == expectedResult);
    }

    @Test
    public void originalImplementationBig() {
        CanPacket p = new CanPacket(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.BIG_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readOriginal(4, 16, ByteOrder.BIG_ENDIAN);
        assertTrue(result == expectedResult);
    }

    @Test
    public void comparePerformance() {

        // iterate over the methods 100000 times each
        int loopCount = 1000000;
        // Get current time
        long start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            readBigEndian();
        }
        float elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("BitSet Big: " + elapsedTime);

        start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            originalImplementationBig();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("original Big: " + elapsedTime);

        start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            readLittleEndian();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("BitSet Little: " + elapsedTime);

        start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            originalImplementationLittle();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("original Little: " + elapsedTime);

    }
}