package de.jlab.cardroid.usb;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;

import de.jlab.cardroid.R;

public class UsbPacketPreference extends UsbStatsPreference {
    public UsbPacketPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UsbPacketPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UsbPacketPreference(Context context) {
        super(context);
    }

    @Override
    protected String getString(int currentValue, UsageStatistics statistics) {
        int averageValue = Math.round(statistics.getAverage());
        int averageReliability = Math.round(statistics.getAverageReliability() * 100f);

        Resources res = getContext().getResources();
        return res.getString(R.string.usb_stats_packets, currentValue, averageValue, averageReliability);
    }
}
