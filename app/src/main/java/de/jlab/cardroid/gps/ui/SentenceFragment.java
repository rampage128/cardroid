package de.jlab.cardroid.gps.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.serial.gps.GpsPosition;
import de.jlab.cardroid.gps.GpsObservable;
import de.jlab.cardroid.utils.UsageStatistics;
import de.jlab.cardroid.utils.ui.FeatureActivity.FeatureActivity;
import de.jlab.cardroid.utils.ui.UnitValues;

public final class SentenceFragment extends FeatureActivity.FeatureFragment {

    private Device.FeatureChangeObserver<GpsObservable> onGpsStateChange = this::onGpsStateChange;
    // FIXME: Add a second listener type to GpsObservable that directly allows listening for sentences
    private GpsObservable.PositionListener onPositionChange = this::onPositionChange;

    private TextView textView;
    private ScrollView scrollView;

    private StringBuilder rawText = new StringBuilder();
    private int sentenceCounter = 0;

    private UsageStatistics sentenceStatistics = new UsageStatistics(1000, 60);
    private UsageStatistics.UsageStatisticsListener onSentenceStatisticUpdate = this::onSentenceStatisticUpdate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gps_sentences, container, false);

        this.textView = rootView.findViewById(R.id.rawDataTextView);
        this.scrollView = rootView.findViewById(R.id.rawDataScrollView);

        this.textView.setMovementMethod(new ScrollingMovementMethod());

        return rootView;
    }

    @Override
    protected void initDataItems(@NonNull DataItemConsumer consumer, @NonNull Context context) {
        consumer.put(R.string.gps_status_sps, context.getResources().getString(R.string.status_unavailable));
    }

    @Override
    protected void onStart(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureSubscriptionConsumer consumer) {
        consumer.subscribeFeature(this.onGpsStateChange, GpsObservable.class);
    }

    @Override
    protected void onStop(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureUnsubscriptionConsumer consumer) {
        consumer.unsubscribeFeature(this.onGpsStateChange, GpsObservable.class);
    }

    private void onSentenceStatisticUpdate(int count, UsageStatistics statistics) {
        String value = UnitValues.getStatisticString(count, Math.round(statistics.getAverage()), R.string.unit_sentences_per_second, getContext());
        this.updateDataItem(R.string.gps_status_sps, value);
    }

    private void onPositionChange(GpsPosition position, String sentence) {
        this.sentenceStatistics.count();

        if (this.sentenceCounter > 1) {
            this.rawText.append("\n");
        }
        this.rawText.append(sentence);
        if (this.sentenceCounter >= 100) {
            this.rawText.replace(0, this.rawText.indexOf("\n") + 1, "");
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                this.textView.setText(this.rawText);
                this.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            });
        }
    }

    private void onGpsStateChange(@NonNull GpsObservable feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            feature.addListener(this.onPositionChange);
            this.sentenceStatistics.addListener(this.onSentenceStatisticUpdate);
        } else {
            feature.removeListener(this.onPositionChange);
            this.sentenceStatistics.removeListener(this.onSentenceStatisticUpdate);
            if (getContext() != null) {
                this.updateDataItem(R.string.gps_status_sps, getContext().getResources().getString(R.string.status_unavailable));
            }
        }
    }
}
