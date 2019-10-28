package de.jlab.cardroid.car;

import android.util.Log;
import android.util.SparseIntArray;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import de.jlab.cardroid.variables.ScriptEngine;
import de.jlab.cardroid.variables.VariableController;

public final class CanPacketDescriptor implements CanObservable.CanPacketListener {

    private long canId;
    private byte byteMask = 0x00;
    private ArrayList<CanValue> values = new ArrayList<>();
    private SparseIntArray bitOffsets = new SparseIntArray();
    private int compressedPacketLength = 0;

    public CanPacketDescriptor(long canId) {
        this.canId = canId;
    }

    public void registerVariables(@NonNull VariableController variableStore, @NonNull ScriptEngine scriptEngine) {
        for (CanValue value : this.values) {
            value.register(variableStore, scriptEngine);
        }
    }

    public void addCanValue(@NonNull CanValue value) {
        this.values.add(value);

        int startByteIndex = value.getBitIndex() / 8;
        int endByteIndex = (value.getBitIndex() + value.getBitLength() - 1) / 8;

        this.recalculateByteMask(startByteIndex, endByteIndex);
        this.recalculateCompressedPacketLength();
        this.recalculateOffsets();
    }

    private void recalculateByteMask(int startByteIndex, int endByteIndex) {
        for (int i = startByteIndex; i <= endByteIndex; i++) {
            this.byteMask |= 1 << (7 - i);
        }

        StringBuilder sb = new StringBuilder("Recalculated byte mask for " + String.format("%02x", this.getCanId()) + " (" + startByteIndex + " -> " + endByteIndex + "): ");
        for (int i = 0; i < 8; i++) {
            sb.append((this.byteMask >> (7 - i)) & 0x01);
        }
        Log.e(this.getClass().getSimpleName(), sb.toString());
    }

    private void recalculateOffsets() {
        Log.e(this.getClass().getSimpleName(), "Recalculating bit offsets for: " + String.format("%02x", this.getCanId()));
        for (int i = 0; i < this.values.size(); i++) {
            CanValue value = this.values.get(i);
            int bitIndex = value.getBitIndex();
            int startByteIndex = (int)Math.floor((bitIndex + 1) / 8f);
            int byteCount = getUnusedByteCountUntil(startByteIndex);
            int byteOffset = byteCount * -1;
            int bitOffset = (byteOffset * 8);
            this.bitOffsets.put(value.getBitIndex(), bitOffset);
            Log.e(this.getClass().getSimpleName(), "Recalculating bit offset: " + bitIndex + " " + bitOffset + " = " + (bitIndex + bitOffset) + " (start byte: " + startByteIndex + ", unused bytes before: " + byteCount + ", byte offset: " + byteOffset + ")");
        }
    }

    private void recalculateCompressedPacketLength() {
        int byteCount = 0;
        for (int i = 0; i < 8; i++) {
            byteCount += (this.byteMask >> (7 - i)) & 0x01;
        }
        Log.e(this.getClass().getSimpleName(), "Recalculating compressed packet length for: " + String.format("%02x", this.getCanId()) + " - Result: " + byteCount);
        this.compressedPacketLength = byteCount;
    }

    private int getUnusedByteCountUntil(int endByteIndex) {
        int byteCount = 0;
        for (int i = 0; i < endByteIndex; i++) {
            byteCount += (this.byteMask >> (7 - i)) & 0x01 ^ 0x01;
        }
        return byteCount;
    }

    public long getCanId() {
        return this.canId;
    }

    public byte getByteMask() {
        return this.byteMask;
    }

    @Override
    public void onReceive(@NonNull CanPacket packet) {
        // FIXME: CanPacketListeners should be addable using a filter, then we don't have to do that internally
        if (packet.getCanId() != this.canId) {
            return;
        }

        for (int i = 0; i < this.values.size(); i++) {
            int bitOffset = 0;
            CanValue value = this.values.get(i);
            if (this.compressedPacketLength == packet.getDataLength()) {
                bitOffset = this.bitOffsets.get(value.getBitIndex());
            }

            try {
                value.updateFromCanPacket(packet, bitOffset);
            } catch (ArrayIndexOutOfBoundsException e) {
                // IGNORE
                Log.e(this.getClass().getSimpleName(), "Error parsing value \"" + value.getName() + "\" from can packet \"" + packet + "\" with offset " + bitOffset + "(" + packet.getDataLength() + " <> " + this.compressedPacketLength + ").");
            }
        }
    }

}
