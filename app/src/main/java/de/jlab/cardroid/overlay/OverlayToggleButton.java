package de.jlab.cardroid.overlay;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;

/**
 * @deprecated This is deprecated together with OverlayWindow
 */
public class OverlayToggleButton extends FrameLayout {

    private ImageView iconImage;
    private ImageView statusDisplay;

    private int srcId;

    public OverlayToggleButton(@NonNull Context context) {
        super(context);
        inflate(context, null);
    }

    public OverlayToggleButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflate(context, attrs);
    }

    public OverlayToggleButton(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, attrs);
    }

    private void inflate(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_overlay_togglebutton, this);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OverlayToggleButton);
            this.srcId = a.getResourceId(R.styleable.OverlayToggleButton_src, -1);
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        this.iconImage = (ImageView)this.findViewById(R.id.iconImage);
        this.statusDisplay = (ImageView)this.findViewById(R.id.statusDisplay);

        if (this.srcId != -1) {
            this.iconImage.setImageResource(this.srcId);
        }
    }

    public void setState(boolean state) {
        this.statusDisplay.setImageResource(state ? R.drawable.ic_cc_status_small_on : R.drawable.ic_cc_status_small_off);
    }

    public void setIconResource(int resourceId) {
        this.srcId = resourceId;
        this.iconImage.setImageResource(this.srcId);
    }
}
