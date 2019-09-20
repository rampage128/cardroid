package de.jlab.cardroid.usb.gps.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.gps.GpsSatellite;

public class GpsSatView extends View {

    private Paint paint = new Paint();
    private GpsSatellite[] satellites = new GpsSatellite[0];

    public GpsSatView(Context context) {
        super(context);
    }

    public GpsSatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GpsSatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GpsSatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float padding = getResources().getDisplayMetrics().density * 32f;
        float satSize = getResources().getDisplayMetrics().density * 12f;
        float strokeWidth = getResources().getDisplayMetrics().density * 3f;
        float fontSize = getResources().getDisplayMetrics().scaledDensity * 16f;
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        float square = Math.min(width, height);
        float radius = square / 2 - padding;

        this.paint.setAntiAlias(true);
        this.paint.setColor(getResources().getColor(android.R.color.darker_gray));
        this.paint.setStrokeWidth(strokeWidth);
        this.paint.setStyle(Paint.Style.STROKE);

        canvas.drawCircle(width / 2,  height / 2, radius, paint);
        canvas.drawCircle(width / 2,  height / 2, radius / 2, paint);
        canvas.drawCircle(width / 2,  height / 2, satSize, paint);

        canvas.drawLine(width / 2 - radius, height / 2, width / 2 - satSize, height / 2, paint);
        canvas.drawLine(width / 2 + satSize, height / 2, width / 2 + radius, height / 2, paint);
        canvas.drawLine(width / 2, height / 2 - radius, width / 2, height / 2 - satSize, paint);
        canvas.drawLine(width / 2, height / 2 + satSize, width / 2, height / 2 + radius, paint);

        this.paint.setTextSize(fontSize);
        this.paint.setTextAlign(Paint.Align.CENTER);
        this.paint.setStyle(Paint.Style.FILL);

        canvas.drawText("N", width / 2, height / 2 - radius - padding / 2 + fontSize / 3, paint);
        canvas.drawText("E", width / 2 + radius + padding / 2, height / 2 + fontSize / 3, paint);
        canvas.drawText("S", width / 2, height / 2 + radius + padding / 2 + fontSize / 3, paint);
        canvas.drawText("W", width / 2 - radius - padding / 2, height / 2 + fontSize / 3, paint);

        for (GpsSatellite satellite : this.satellites) {
            this.paint.setStyle(Paint.Style.FILL);
            if (System.currentTimeMillis() - satellite.getLastUpdate() > 5000) {
                this.paint.setColor(getResources().getColor(R.color.colorGpsSatelliteTimeout));
            }
            else if (satellite.getSnr() == -1) {
                this.paint.setColor(getResources().getColor(R.color.colorGpsSatelliteNoFix));
            }
            else {
                this.paint.setColor(getResources().getColor(R.color.colorGpsSatelliteFix));
            }

            float r = (90f - Math.max(0, satellite.getElevation())) / 90f * radius;
            float satX = (float)(width / 2f + r * Math.cos(Math.toRadians(satellite.getAzimuth() - 90f)));
            float satY = (float)(height / 2f + r * Math.sin(Math.toRadians(satellite.getAzimuth() - 90f)));

            canvas.drawCircle(satX,  satY, satSize, paint);
            this.paint.setColor(getResources().getColor(android.R.color.white));
            canvas.drawText(Integer.toString(satellite.getPrn()), satX, satY + fontSize / 3, paint);
        }
    }

    public void setSatellites(GpsSatellite[] satellites) {
        this.satellites = satellites;
    }
}
