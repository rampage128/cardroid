package de.jlab.cardroid.utils.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;

public class CircularMenuView extends View {

    public static final int GRAVITY_OUTSIDE = 1;
    public static final int GRAVITY_MIDDLE  = 0;

    private MenuItem.OnMenuItemClickListener itemClickListener;

    private Paint mRadialMenuPaint = new Paint();
    private Menu menu = null;

    float centerRadius = .2f;
    int backgroundColor = 0;
    int foregroundColor = 0;
    int menuSweep = 360;
    int startAngle = -90;
    int padding = 16;
    int iconSize = 48;
    int fontSize = 14;
    int itemGravity = GRAVITY_MIDDLE;

    private int radius = 0;

    public CircularMenuView(Context context) {
        super(context);

        this.init(context, null, 0);
    }

    public CircularMenuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.init(context, attrs, 0);
    }

    public CircularMenuView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.init(context, attrs, defStyleAttr);
    }

    public CircularMenuView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.init(context, attrs, defStyleAttr);
    }

    public void setOnMenuItemClickListener(@Nullable MenuItem.OnMenuItemClickListener listener) {
        this.itemClickListener = listener;
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyle) {

        float density = context.getResources().getDisplayMetrics().density;
        this.iconSize *= density;
        this.fontSize *= density;
        this.padding *= density;

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.CircularMenuView, defStyle, 0);

            this.centerRadius = a.getFloat(R.styleable.CircularMenuView_innerRadius, .2f);
            this.backgroundColor = a.getColor(R.styleable.CircularMenuView_backgroundColor, getContext().getResources().getColor(R.color.colorAccentBG));
            this.foregroundColor = a.getColor(R.styleable.CircularMenuView_foregroundColor, getContext().getResources().getColor(R.color.dashboard_foreground));

            this.menuSweep = a.getInt(R.styleable.CircularMenuView_sweep, 360);
            this.startAngle = a.getInt(R.styleable.CircularMenuView_angle, -90);
            this.iconSize = Math.round(a.getDimension(R.styleable.CircularMenuView_iconSize, this.iconSize));
            this.fontSize = Math.round(a.getDimension(R.styleable.CircularMenuView_fontSize, this.fontSize));
            this.padding = Math.round(a.getDimension(R.styleable.CircularMenuView_padding, this.padding));
            this.itemGravity = a.getInt(R.styleable.CircularMenuView_itemGravity, GRAVITY_MIDDLE);

            int menuResource = a.getResourceId(R.styleable.CircularMenuView_menu, 0);
            if (menuResource != 0) {
                inflateMenu(menuResource);
            }

            a.recycle();
        }
    }

    private void inflateMenu(@MenuRes int menuResource) {
        this.menu = new PopupMenu(getContext(), null).getMenu();
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(menuResource, this.menu);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float square = Math.min(w, h);
        radius = Math.round(square / 2);
    }

    private Point getPoint(float r, int angle) {
        float square = Math.min(getWidth(), getHeight());
        float rMax = square / 2;

        double angleR = Math.toRadians(angle);
        int rPx = (int) (rMax * r);

        return new Point(
                (int) (rPx * Math.cos(angleR) + getWidth() / 2),
                (int) (rPx * Math.sin(angleR) + getHeight() / 2)
        );
    }

    private Rect getItemBounds(@NonNull String text, int iconSizePx, int padding) {
        Rect bounds = new Rect();
        mRadialMenuPaint.getTextBounds(text, 0, text.length(), bounds);
        bounds.bottom += iconSizePx + padding;
        return bounds;
    }

    private float getItemRadius(@NonNull Rect itemBounds) {
        if (itemGravity == GRAVITY_OUTSIDE) {
            int itemDiameter = (int) Math.round(Math.sqrt(Math.pow(itemBounds.right, 2) + Math.pow(itemBounds.bottom, 2)));
            return 1 - ((itemDiameter / 2 + this.padding) / (float) radius);
        } else {
            return ((1 - centerRadius) / 2) + centerRadius;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float itemSweep = menuSweep / this.menu.size();

        float dividerWidth = 1 * getContext().getResources().getDisplayMetrics().density;
        int iconSizePx = (int)(iconSize * getContext().getResources().getDisplayMetrics().density);
        float fontSizePx = getResources().getDisplayMetrics().scaledDensity * fontSize;

        int textPadding = (int)(16 * getContext().getResources().getDisplayMetrics().density);

        int halfIconSize = (int)(iconSizePx / 2f);

        mRadialMenuPaint.setColor(Color.WHITE);
        mRadialMenuPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(0, 0, getWidth(), getHeight(), 0, 360, true, mRadialMenuPaint);

        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);

            Rect itemBounds = getItemBounds(item.getTitle().toString(), iconSizePx, textPadding);
            int itemOffsetY = Math.round(itemBounds.bottom / 2 * -1);

            int itemAngle = Math.round((startAngle + itemSweep * i) + 360) % 360;
            int itemCenterAngle = Math.round((itemAngle + itemSweep / 2) + 360) % 360;

            float menuItemRadius = getItemRadius(itemBounds);
            Point itemCoords = getPoint(menuItemRadius, itemCenterAngle);

            mRadialMenuPaint.setAntiAlias(true);

            // draw background
            //mRadialMenuPaint.setColor(this.backgroundColor);
            //canvas.drawArc(0, 0, getWidth(), getHeight(), i * itemSweep, itemSweep, true, mRadialMenuPaint);

            // draw divider
            float circularPadding = (padding / (float)radius);
            Point separatorStart = getPoint(centerRadius + circularPadding, itemAngle);
            Point separatorEnd = getPoint(1 - circularPadding, itemAngle);
            mRadialMenuPaint.setStrokeWidth(dividerWidth);
            mRadialMenuPaint.setColor(getContext().getResources().getColor(R.color.divider));
            canvas.drawLine(separatorStart.x, separatorStart.y, separatorEnd.x, separatorEnd.y, mRadialMenuPaint);

            // draw icon
            Drawable icon = item.getIcon();
            icon.setBounds(itemCoords.x - halfIconSize, itemCoords.y + itemOffsetY, itemCoords.x + halfIconSize, itemCoords.y + itemOffsetY + iconSizePx);
            item.getIcon().draw(canvas);

            // draw text
            int textY = itemCoords.y + itemOffsetY + iconSizePx + textPadding;
            mRadialMenuPaint.setTextAlign(Paint.Align.CENTER);
            mRadialMenuPaint.setTextSize(fontSizePx);
            mRadialMenuPaint.setColor(this.foregroundColor);
            canvas.drawText(item.getTitle().toString(), itemCoords.x, textY, mRadialMenuPaint);
        }
    }

    private MenuItem getItemFromXY(int x, int y) {
        float itemSweep = menuSweep / this.menu.size();

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        float angle = (float) (Math.toDegrees(Math.atan2(y - cy, x - cx)) + 360 - startAngle) % 360;
        int itemIndex = (int)Math.floor(angle / itemSweep);
        return menu.getItem(itemIndex);
    }

    private MenuItem touchedItem;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        MenuItem currentItem = getItemFromXY(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(currentItem != null && menu.size() > 0) {
                    touchedItem = currentItem;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentItem != null && touchedItem == currentItem) {
                    if (this.itemClickListener != null) {
                        this.itemClickListener.onMenuItemClick(currentItem);
                    }
                    touchedItem = null;
                    invalidate();
                }
                break;
        }

        return true;
    }



}
