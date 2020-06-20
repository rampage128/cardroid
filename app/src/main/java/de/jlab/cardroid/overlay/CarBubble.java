package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.jlab.cardroid.R;
import de.jlab.cardroid.utils.ui.UnitValues;

public final class CarBubble extends Overlay {

    private SeekArc fanSeekBar;
    private TextView temperatureText;
    private AbstractOverlayController overlayController;
    private int maxFanLevel;
    private int minTemperature;
    private int maxTemperature;
    private boolean enableVolumeControl;
    private long volumeControlTouchDuration;

    private TapTouchListener.TouchActionListener onTapTouch = this::onTapTouch;
    private TapTouchListener.TouchActionListener onMultiTapTouch = this::onMultiTapTouch;

    private MotionHelper motionHelper = new MotionHelper(this);

    public CarBubble(@NonNull AbstractOverlayController overlayController, @Nullable OverlayToggleListener toggleListener, @NonNull Context context, int maxFanLevel, int minTemperature, int maxTemperature, boolean enableVolumeControl, long volumeControlTouchDuration) {
        super(context, toggleListener);
        this.overlayController = overlayController;
        this.maxFanLevel = maxFanLevel;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.enableVolumeControl = enableVolumeControl;
        this.volumeControlTouchDuration = volumeControlTouchDuration;
    }

    public void setTemperature(float desiredTemperature) {
        this.runOnUiThread(() -> {
            float temperature = UnitValues.constrainToRange(desiredTemperature, this.minTemperature, this.maxTemperature);
            CharSequence temperatureText = temperature > 0 ? UnitValues.getFancyDecimalValue(temperature) : getString(R.string.cc_off);
            this.temperatureText.setText(temperatureText);
        });
    }

    public void setFanLevel(int desiredFanLevel) {
        this.runOnUiThread(() -> {
            int fanLevel = UnitValues.constrainToRange(desiredFanLevel, 0, this.maxFanLevel);
            this.fanSeekBar.setProgress(fanLevel);
        });
    }

    private void onTapTouch(@NonNull View view, @NonNull TapTouchListener.Action action, @NonNull MotionEvent motionEvent) {
        if (!this.enableVolumeControl) {
            return;
        }

        if (action == TapTouchListener.Action.START_HOLD) {
            if (motionEvent.getPointerCount() == 1) {
                View contentView = this.getContentView();

                if (contentView != null) {
                    int[] location = new int[2];
                    contentView.getLocationOnScreen(location);

                    this.overlayController.showVolumeControls(location[0] + contentView.getWidth() / 2, location[1] + contentView.getHeight() / 2);
                }
            }
        } else if (action == TapTouchListener.Action.STOP_HOLD) {
            this.overlayController.hideVolumeControls();
        } else if (action == TapTouchListener.Action.MOVE_HOLD) {
            int x = Math.round(motionEvent.getX() - (view.getWidth() / 2f));
            int y = Math.round(motionEvent.getY() - (view.getWidth() / 2f));
            this.overlayController.setVolumeFromCoords(x, y);
        }
    }

    private void onMultiTapTouch(@NonNull View view, @NonNull TapTouchListener.Action action, @NonNull MotionEvent motionEvent) {
        if (action == TapTouchListener.Action.START_HOLD) {
            // START_HOLD is never given, because the initial event will always only have 1 touch point
        } else if (action == TapTouchListener.Action.STOP_HOLD) {
            this.motionHelper.stopMove(motionEvent, this::onMove);
        } else if (action == TapTouchListener.Action.MOVE_HOLD) {
            this.motionHelper.updateMove(motionEvent, view.getContext());
        }
    }

    private void onMove(int x, int y) {
        this.overlayController.updateBubblePosition(x, y);
    }

    private void showCarControls() {
        this.overlayController.showCarControls();
    }

    @Override
    protected void onCreate(@NonNull WindowManager.LayoutParams windowParams, @NonNull Context context) {
        View contentView = this.setContentView(R.layout.overlay_bubble_car);

        this.fanSeekBar = this.findViewById(R.id.progressBar);
        this.temperatureText = this.findViewById(R.id.text);

        this.fanSeekBar.setMax(this.maxFanLevel);
        this.fanSeekBar.setSegments(this.maxFanLevel);

        contentView.setOnClickListener(v -> showCarControls());
        contentView.setOnTouchListener(new TapTouchListener(new Handler(context.getMainLooper()), this.onTapTouch, this.onMultiTapTouch, this.volumeControlTouchDuration));

        windowParams.width = windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int x = prefs.getInt("overlay_bubble_x", -1);
        int y = prefs.getInt("overlay_bubble_y", -1);

        if (x >= 0 && y >= 0) {
            windowParams.gravity = Gravity.TOP | Gravity.START;
            windowParams.x = x;
            windowParams.y = y;
        } else {
            windowParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        }
    }

    @Override
    protected void onShow() {}

    @Override
    protected void onHide() {}

    @Override
    protected void onDestroy() {}

    private static class MotionHelper {
        private Overlay overlay;
        private boolean isMoving = false;
        private int offsetX = 0;
        private int offsetY = 0;

        private int x = 0;
        private int y = 0;

        private int dockTolerance = 64;

        private Rect borderMargin = new Rect(0, 0, 0, 16);

        private Point screenSize = new Point();

        public MotionHelper(@NonNull Overlay overlay) {
            this.overlay = overlay;
        }

        public void startMove(@NonNull MotionEvent event, @NonNull Context context) {
            this.offsetX = (int)event.getX();
            this.offsetY = (int)event.getY();
            this.isMoving = true;

            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getSize(this.screenSize);
        }

        public void updateMove(@NonNull MotionEvent event, @NonNull Context context) {
            if (!this.isMoving) {
                startMove(event, context);
            }
            updateCoords(event);
            // calculate docking zones and show component in final position
            moveOverlay();
        }

        public void stopMove(@NonNull MotionEvent event, @Nullable MoveListener moveListener) {
            // calculate docking zones and move to final position
            moveOverlay();
            this.offsetX = 0;
            this.offsetY = 0;
            this.isMoving = false;
            if (moveListener != null) {
                moveListener.onMove(this.x, this.y);
            }
        }

        private void updateCoords(@NonNull MotionEvent event) {
            this.x = (int) event.getRawX() - this.offsetX;
            this.y = (int) event.getRawY() - this.offsetY;

            boolean dockedBorderX = false;
            boolean dockedBorderY = false;

            int dockLeft = this.borderMargin.left;
            int dockRight = screenSize.x - (this.borderMargin.right + this.overlay.getWidth());
            int dockTop = this.borderMargin.top;
            int dockBottom = screenSize.y - (this.borderMargin.bottom + this.overlay.getHeight());

            int dockCenterX = (screenSize.x / 2) - (this.overlay.getWidth() / 2);
            int dockCenterY = (screenSize.y / 2) - (this.overlay.getHeight() / 2);

            if (closeToOrLess(this.y, dockTop, this.dockTolerance)) {
                this.y = dockTop;
                dockedBorderY = true;
            } else if (closeToOrMore(this.y, dockBottom, this.dockTolerance)) {
                this.y = dockBottom;
                dockedBorderY = true;
            }

            if (closeToOrLess(this.x, dockLeft, this.dockTolerance)) {
                this.x = dockLeft;
                dockedBorderX = true;
            } else if (closeToOrMore(this.x, dockRight, this.dockTolerance)) {
                this.x = dockRight;
                dockedBorderX = true;
            }

            if (dockedBorderY) {
                if (closeTo(this.x, dockCenterX, this.dockTolerance)) {
                    this.x = dockCenterX;
                }
            }

            if (dockedBorderX) {
                if (closeTo(this.y, dockCenterY, this.dockTolerance)) {
                    this.y = dockCenterY;
                }
            }
        }

        private boolean closeTo(int value, int reference, int tolerance) {
            return value <= reference + tolerance && value >= reference - tolerance;
        }

        private boolean closeToOrMore(int value, int reference, int tolerance) {
            return value >= reference || closeTo(value, reference, tolerance);
        }

        private boolean closeToOrLess(int value, int reference, int tolerance) {
            return value <= reference || closeTo(value, reference, tolerance);
        }

        private void moveOverlay() {
            this.overlay.move(this.x, this.y);
        }

        public interface MoveListener {
            void onMove(int x, int y);
        }
    }
}
