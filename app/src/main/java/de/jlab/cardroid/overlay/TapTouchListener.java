package de.jlab.cardroid.overlay;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class TapTouchListener implements View.OnTouchListener {

    public enum Action {
        START_HOLD,
        MOVE_HOLD,
        STOP_HOLD
    }

    private Handler handler;
    private Runnable checkHolding = this::startHolding;

    private TouchActionListener touchActionListener;
    private TouchActionListener multiTouchActionListener;
    private MotionEvent event;
    private View view;
    private boolean isHolding = false;
    private TouchActionListener activeListener = null;
    private long touchDuration;

    public TapTouchListener(@NonNull Handler handler, @NonNull TouchActionListener touchActionListener, @NonNull TouchActionListener multiTouchActionListener, long touchDuration) {
        this.handler = handler;
        this.touchActionListener = touchActionListener;
        this.multiTouchActionListener = multiTouchActionListener;
        this.touchDuration = touchDuration;
    }

    private void startHolding() {
        this.activeListener.onAction(this.view, Action.START_HOLD, this.event);
        this.isHolding = true;
    }

    private void startCheckHolding(@NonNull View view, @NonNull MotionEvent event) {
        this.stopCheckHolding();
        this.view = view;
        this.event = event;
        this.activeListener = event.getPointerCount() > 1 ? this.multiTouchActionListener : this.touchActionListener;
        this.handler.postDelayed(this.checkHolding, this.touchDuration);
    }

    private void stopCheckHolding() {
        this.handler.removeCallbacks(this.checkHolding);
    }

    private void stopHolding() {
        this.activeListener.onAction(this.view, Action.STOP_HOLD, this.event);
        this.view = null;
        this.event = null;
        this.isHolding = false;
        this.activeListener = null;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        boolean isReleased = event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL;
        boolean isPressed = event.getAction() == MotionEvent.ACTION_DOWN;
        boolean isMoved = event.getAction() == MotionEvent.ACTION_MOVE && this.isHolding;

        if (isReleased) {
            this.stopCheckHolding();
            if (!this.isHolding) {
                view.performClick();
            } else {
                this.stopHolding();
            }
        } else if (isPressed) {
            this.startCheckHolding(view, event);
        } else if (isMoved) {
            // It seems in an ACTION_DOWN the pointer count is always 1, so we need to check this when moving
            if (event.getPointerCount() > 1) {
                this.activeListener = this.multiTouchActionListener;
            }
            this.activeListener.onAction(view, Action.MOVE_HOLD, event);
        }

        return true;
    }

    public interface TouchActionListener {
        void onAction(@NonNull View view, @NonNull Action action, @NonNull MotionEvent motionEvent);
    }

}
