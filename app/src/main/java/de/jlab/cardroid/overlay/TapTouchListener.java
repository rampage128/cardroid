package de.jlab.cardroid.overlay;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public final class TapTouchListener implements View.OnTouchListener {

    public enum Action {
        START_HOLD,
        MOVE_HOLD,
        STOP_HOLD
    }

    private Handler handler;
    private Runnable checkHolding = this::startHolding;

    private ActionListener actionListener;
    private MotionEvent event;
    private View view;
    private boolean isHolding = false;

    public TapTouchListener(@NonNull Handler handler, @NonNull ActionListener actionListener) {
        this.handler = handler;
        this.actionListener = actionListener;
    }

    private void startHolding() {
        this.actionListener.onAction(this.view, Action.START_HOLD, this.event);
        this.isHolding = true;
    }

    private void startCheckHolding(@NonNull View view, @NonNull MotionEvent event) {
        this.stopCheckHolding();
        this.view = view;
        this.event = event;
        this.handler.postDelayed(this.checkHolding, 100);
    }

    private void stopCheckHolding() {
        this.handler.removeCallbacks(this.checkHolding);
    }

    private void stopHolding() {
        this.actionListener.onAction(this.view, Action.STOP_HOLD, this.event);
        this.view = null;
        this.event = null;
        this.isHolding = false;
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
            this.actionListener.onAction(view, Action.MOVE_HOLD, event);
        }

        return true;
    }

    public interface ActionListener {
        void onAction(@NonNull View view, @NonNull Action action, @NonNull MotionEvent motionEvent);
    }

}
