package de.jlab.cardroid.devices.serial.can;

import org.junit.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.BitSet;

import static org.junit.Assert.*;

class ComparisonData {
    public byte[] canBytes;
    public int bitNumber;
    public int length;
    public ByteBuffer expectedResult;

    public ComparisonData(byte[] canBytes, byte[] expectedResult, int bitNumber, int length) {
        this.canBytes = canBytes;
        this.bitNumber = bitNumber;
        this.length = length;
        this.expectedResult = ByteBuffer.wrap(expectedResult);
    }
}

// Left here for regression testing and benchmarking.
// The Current implementation has the fastest (and correct) read.
class CanPacketBenchmark {

    private long canId = 0;
    private BitSet bits;
    private byte[] data;

    public CanPacketBenchmark(long canId, byte[] data) {
        this.canId = canId;
        this.data = data;

        // BitSet wants to be initialized with an array in Little Endian.
        // reverse the array
        byte[] reversed = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            reversed[i] = data[data.length - 1 - i];
        }
        this.bits = BitSet.valueOf(reversed);
    }

    public long readBitSet(int start, int length, ByteOrder order) {
        BitSet readValue = this.bits.get(start, start + length);
        ByteBuffer bytes = ByteBuffer.wrap(readValue.toByteArray());
        // bytes are in Little Endian. But because we've swapped endianness in the intialized
        // we need to invert endianness here.
        if (order == ByteOrder.LITTLE_ENDIAN) {
            bytes.order(ByteOrder.BIG_ENDIAN);
        } else {
            bytes.order(ByteOrder.LITTLE_ENDIAN);
        }

        if (length <= 8) {
            return bytes.get();
        } else if (length <= 16) {
            return bytes.getShort();
        } else if (length <= 32) {
            return bytes.getInt();
        } else {
            return bytes.getLong();
        }
    }

    public long readOriginal(int startBit, int bitLength, ByteOrder order) {
        long result = 0;
        int currentByte = (int)Math.floor((startBit + 1) / 8f);
        int currentBit;

        for (int i = startBit; i < startBit + bitLength; i++) {
            // get the offset bit index in current data byte
            int offsetBit = i % 8;
            // Retrieve byte for bit-index from data array
            if (offsetBit == 0) {
                currentByte = (int)Math.floor((i + 1) / 8f);
                // TODO make readBigEndian shift whole bytes to make a little endian version
            }

            result <<= 1;

            // transfer current bit if 1
            currentBit = (this.data[currentByte] >> (7 - offsetBit)) & 0x01;
            if (currentBit != 0) {
                result |= 0x01;
            }
        }

        if (order == ByteOrder.LITTLE_ENDIAN) {
            //swap bytes in the result
            if (bitLength <= 8) {
                return (byte)result;
            } else if (bitLength <= 16) {
                return Short.reverseBytes((short)result);
            } else if (bitLength <= 32) {
                return Integer.reverseBytes((int)result);
            } else {
                return Long.reverseBytes(result);
            }
        }

        return result;
    }

    public long readOptimized(int startBit, int bitLength, ByteOrder order) {
        long result = 0;
        int currentByte = startBit / 8;
        int currentBit;

        for (int i = startBit; i < startBit + bitLength;) {
            // get the offset bit index in current data byte
            int offsetBit = i % 8;
            // Calculate how many bits can we read simultaneously
            byte bitSize = (byte) (Math.min(8, bitLength + startBit - i) - offsetBit);
            currentByte = i / 8;
            if (bitSize == 8) {
                // fast path
                result = result << 8 | this.data[currentByte];
            } else {
                result <<= bitSize;
                // offset the chunk we are interested in, then mask the whole thing
                result |= (byte)((this.data[currentByte] >> (bitSize - offsetBit)) & (( 1 << bitSize ) - 1));
            }

            i += bitSize;
        }

        if (order == ByteOrder.LITTLE_ENDIAN) {
            //swap bytes in the result
            if (bitLength <= 8) {
                return (byte)result;
            } else if (bitLength <= 16) {
                return Short.reverseBytes((short)result);
            } else if (bitLength <= 32) {
                return Integer.reverseBytes((int)result);
            } else {
                return Long.reverseBytes(result);
            }
        }

        return result;
    }

}

public class CanPacketTest {

    public byte[] testBytes = { (byte)0B01000011, (byte)0B00111100, (byte)0B10101111 };

    public byte[] shortOffset4 = { (byte)0B00110011, (byte)0B11001010 };
    public byte[] shortOffset8 = { (byte)0B00111100, (byte)0B10101111 };
    public byte[] shortOffset6Length10 = { (byte)0B00000011, (byte)0B00111100 };
    public byte[] shortOffset8Length10 = { (byte)0B00000000, (byte)0B11110010 };

    public ComparisonData data1 = new ComparisonData(testBytes, shortOffset4, 4, 16);
    public ComparisonData data2 = new ComparisonData(testBytes, shortOffset8, 8, 16);
    public ComparisonData data3 = new ComparisonData(testBytes, shortOffset6Length10, 6, 10);
    public ComparisonData data4 = new ComparisonData(testBytes, shortOffset8Length10, 8, 10);

    public ComparisonData[] testBenchData = { data1, data2, data3, data4 };

    private void testByteOrder(ByteOrder order) {
        for (ComparisonData data: testBenchData) {
            CanPacket p = new CanPacket(0, data.canBytes);
            ByteBuffer expectedBytes = data.expectedResult;
            expectedBytes.order(order);
            long result = order == ByteOrder.BIG_ENDIAN ?
                    p.readBigEndian(data.bitNumber, data.length):
                    p.readLittleEndian(data.bitNumber, data.length);
            long expectedResult = expectedBytes.getShort();
            assertTrue(result == expectedResult);
        }
    }

    @Test
    public void readBigEndian() {
        testByteOrder(ByteOrder.BIG_ENDIAN);
    }

    @Test
    public void readLittleEndian() {
        testByteOrder(ByteOrder.LITTLE_ENDIAN);
    }

    void bitSetImplementationLittle() {
        CanPacketBenchmark p = new CanPacketBenchmark(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readBitSet(4, 16, ByteOrder.LITTLE_ENDIAN);
        assertTrue(result == expectedResult);
    }

    void bitSetImplementationBig() {
        CanPacketBenchmark p = new CanPacketBenchmark(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.BIG_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readBitSet(4, 16, ByteOrder.BIG_ENDIAN);
        assertTrue(result == expectedResult);
    }

    void originalImplementationLittle() {
        CanPacketBenchmark p = new CanPacketBenchmark(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readOriginal(4, 16, ByteOrder.LITTLE_ENDIAN);
        assertTrue(result == expectedResult);
    }

    void originalImplementationBig() {
        CanPacketBenchmark p = new CanPacketBenchmark(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.BIG_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readOriginal(4, 16, ByteOrder.BIG_ENDIAN);
        assertTrue(result == expectedResult);
    }

    void optimizedImplementationLittle() {
        CanPacketBenchmark p = new CanPacketBenchmark(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.LITTLE_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readOptimized(4, 16, ByteOrder.LITTLE_ENDIAN);
        assertTrue(result == expectedResult);
    }

    void optimizedImplementationBig() {
        CanPacketBenchmark p = new CanPacketBenchmark(0, testBytes);
        ByteBuffer bytes = ByteBuffer.wrap(shortOffset4);
        bytes.order(ByteOrder.BIG_ENDIAN);
        short expectedResult = bytes.getShort();
        short result = (short)p.readOptimized(4, 16, ByteOrder.BIG_ENDIAN);
        assertTrue(result == expectedResult);
    }

    @Test
    public void comparePerformance() {

        // iterate over the methods loopCount times each
        int loopCount = 1000000;
        // Get current time
        long start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            bitSetImplementationBig();
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
            optimizedImplementationBig();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("optimized Big: " + elapsedTime);

        start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            bitSetImplementationLittle();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("BitSet Little: " + elapsedTime);

        start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            originalImplementationLittle();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("original Little: " + elapsedTime);

        start = System.currentTimeMillis();
        for (int i=0; i<loopCount; i++) {
            optimizedImplementationLittle();
        }
        elapsedTime = (System.currentTimeMillis()-start)/1000F;
        System.out.println("optimized Little: " + elapsedTime);

    }
}