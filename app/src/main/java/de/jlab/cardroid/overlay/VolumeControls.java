package de.jlab.cardroid.overlay;

import android.content.Context;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;

public final class VolumeControls extends Overlay {

    private AudioManager audioManager;
    private SeekArc muteDial;
    private SeekArc volumeDial;
    private TextView volumeText;
    private int steps;

    public VolumeControls(@NonNull Context context, int steps) {
        super(context);

        this.steps = steps;
    }

    @Override
    protected void onCreate(@NonNull WindowManager.LayoutParams windowParams, @NonNull Context context) {
        this.setContentView(R.layout.overlay_volume);

        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        this.muteDial = findViewById(R.id.muteDial);
        this.volumeDial = findViewById(R.id.volumeDial);
        this.volumeText = findViewById(R.id.text);

        this.volumeDial.setMax(this.steps);
        this.volumeDial.setSegments(this.steps);

        windowParams.width = windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    }

    public void setProgressFromCoords(int x, int y) {
        if (this.volumeDial.setProgressFromCoords(x, y, true)) {
            updateOther();
            setVolume();
        }
    }

    private void setVolume() {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int newVolume = Math.round(maxVolume * (this.volumeDial.getProgress() / (float) this.volumeDial.getMax()));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
    }

    private void updateOther() {
        int progress = this.volumeDial.getProgress();
        String text = ((100f / this.volumeDial.getMax()) * progress) + "%";
        this.volumeText.setText(text);
        this.muteDial.setProgress(progress > 0 ? 0 : 1);
    }

    @Override
    protected void onShow() {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int initialProgress = Math.round(this.volumeDial.getMax() * (currentVolume / (float)maxVolume));
        this.volumeDial.setProgress(initialProgress);
        updateOther();
    }

    @Override
    protected void onHide() {

    }

    @Override
    protected void onDestroy() {

    }
}
