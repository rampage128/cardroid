package de.jlab.cardroid.usb;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;

import de.jlab.cardroid.R;

public class UsbBandwidthPreference extends UsbStatsPreference {
    public UsbBandwidthPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UsbBandwidthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UsbBandwidthPreference(Context context) {
        super(context);
    }

    @Override
    protected String getString(int currentValue, UsageStatistics statistics) {
        int averageValue = Math.round(statistics.getAverage());
        int averageReliability = Math.round(statistics.getAverageReliability() * 100f);
        int currentUsage = Math.round(100f / 11520 * currentValue);

        Resources res = getContext().getResources();
        return res.getString(R.string.usb_stats_bandwidth, currentValue, averageValue, averageReliability, currentUsage);
    }
}
