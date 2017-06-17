package de.jlab.cardroid.usb;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public abstract class UsbStatsPreference extends Preference implements UsageStatistics.UsageStatisticsListener {
    private Handler uiHandler;

    public UsbStatsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UsbStatsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UsbStatsPreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View layout = super.onCreateView(parent);
        this.uiHandler = new Handler(Looper.getMainLooper());
        return layout;
    }

    @Override
    public void onInterval(final int count, final UsageStatistics statistics) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = getString(count, statistics);
                setSummary(text);
            }
        });
    }

    protected abstract String getString(int currentValue, UsageStatistics statistics);

    private void runOnUiThread(Runnable runnable) {
        if (uiHandler != null) {
            uiHandler.post(runnable);
        }
    }
}
