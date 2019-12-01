package de.jlab.cardroid.utils.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.card.MaterialCardView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class RoundCardView extends MaterialCardView {

    private int measuredCornerRadius = 0;

    public RoundCardView(@NonNull Context context) {
        super(context);
    }

    public RoundCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int newMeasuredCornerRadius = Math.min(this.getMeasuredWidth(), this.getMeasuredHeight());
        if (newMeasuredCornerRadius != this.measuredCornerRadius) {
            this.measuredCornerRadius = newMeasuredCornerRadius;
            this.setRadius(newMeasuredCornerRadius / 2f);
        }
    }
}
