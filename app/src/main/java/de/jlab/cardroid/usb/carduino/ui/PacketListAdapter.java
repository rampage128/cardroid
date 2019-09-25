package de.jlab.cardroid.usb.carduino.ui;

import android.content.Context;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.carduino.serial.SerialCanPacket;
import de.jlab.cardroid.usb.carduino.serial.SerialDataPacket;
import de.jlab.cardroid.usb.carduino.serial.SerialPacket;

public class PacketListAdapter extends BaseAdapter {

    private Context context;
    private LinkedHashMap<Long, PacketContainer> packets = new LinkedHashMap<>();

    static class ViewHolder {
        TextView id;
        TextView raw;
        TextView hex;
        TextView binary;
    }

    static class PacketContainer {
        long lastUpdate;
        SerialCanPacket packet;
    }

    public PacketListAdapter(Context context) {
        this.context = context;
    }

    public void flushPackets() {
        Iterator<Long> keyIt = this.packets.keySet().iterator();
        while (keyIt.hasNext()) {
            Long key = keyIt.next();
            PacketContainer container = this.packets.get(key);
            if (container != null) {
                if (System.currentTimeMillis() - container.lastUpdate > 3000) {
                    this.packets.remove(key);
                }
            }
        }
    }

    public void updatePacket(SerialCanPacket packet) {
        long id = packet.getCanId();
        PacketContainer container = this.packets.get(id);
        if (container == null) {
            container = new PacketContainer();
            this.packets.put(id, container);
        }
        container.lastUpdate = System.currentTimeMillis();
        container.packet = packet;
        flushPackets();
    }

    @Override
    public int getCount() {
        return this.packets.size();
    }

    @Override
    public Object getItem(int position) {
        return this.packets.values().toArray(new PacketContainer[0])[position];
    }

    @Override
    public long getItemId(int position) {
        return this.packets.values().toArray(new PacketContainer[0])[position].packet.getCanId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SerialCanPacket packet = ((PacketContainer)getItem(position)).packet;

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            final LayoutInflater layoutInflater = LayoutInflater.from(this.context);
            convertView = layoutInflater.inflate(R.layout.listitem_carduino_packet, null);
            holder.id = (TextView) convertView.findViewById(R.id.id);
            holder.raw = (TextView) convertView.findViewById(R.id.raw);
            holder.hex = (TextView) convertView.findViewById(R.id.hex);
            holder.binary = (TextView) convertView.findViewById(R.id.binary);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        byte[] payload = new byte[0];
        if (packet != null) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                packet.serialize(bout);
                payload = bout.toByteArray();
            }
            catch (IOException exception) { /* Intentionally left blank */ }
            holder.id.setText(String.format("%02X", packet.getCanId()));
        }

        if (payload.length > 0) {
            holder.raw.setText(new String(payload));
            holder.hex.setText(bytesToHex(payload));
            holder.binary.setText(bytesToBin(payload));
        }
        else {
            holder.raw.setText("");
            holder.hex.setText("");
            holder.binary.setText("");
        }

        return convertView;
    }

    private String bytesToBin(byte[] bytes) {
        StringBuilder binaryString = new StringBuilder(bytes.length * 9 - 1);
        for ( int j = 0; j < bytes.length; j++ ) {
            if (j > 0) {
                binaryString.append(" ");
            }
            binaryString.append(String.format("%8s", Integer.toBinaryString(bytes[j] & 0xFF)).replace(' ', '0'));
        }
        return binaryString.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 3 - 1);
        for ( int j = 0; j < bytes.length; j++ ) {
            if (j > 0) {
                hexString.append(" ");
            }
            hexString.append(byteToHex(bytes[j]));
        }
        return hexString.toString();
    }

    private final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private String byteToHex(byte input) {
        int v = input & 0xFF;
        return String.valueOf(hexArray[v >>> 4]) + hexArray[v & 0x0F];
    }
}
