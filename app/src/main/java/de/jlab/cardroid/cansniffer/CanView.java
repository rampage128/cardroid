package de.jlab.cardroid.cansniffer;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.view.View;

import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.carduino.SerialCanPacket;

public class CanView extends View {

    private LongSparseArray<SniffedCanPacket> packets = new LongSparseArray<>();

    private float fontSize = 0;
    private float smallFontSize = 0;

    private float idWidth = 0;
    private float byteWidth = 0;
    private float columnPadding = 0;
    private float rowHeight = 0;

    private int highlightColor = 0;
    private int defaultColor = 0;
    private int backgroundColor = 0;
    private int oddBackgroundColor = 0;

    private ArgbEvaluator evaluator = new ArgbEvaluator();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public CanView(Context context) {
        super(context);
        initMetrics();
    }

    public CanView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CanView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initMetrics();
    }

    protected void initMetrics() {
        this.fontSize = this.getResources().getDisplayMetrics().scaledDensity * 16f;
        this.smallFontSize = this.getResources().getDisplayMetrics().scaledDensity * 8f;
        this.idWidth = this.getResources().getDisplayMetrics().density * 60f;
        this.byteWidth = (this.getWidth() - this.idWidth) / 8;
        this.columnPadding = this.getResources().getDisplayMetrics().density * 8f;

        this.rowHeight = this.fontSize * 2 + this.smallFontSize + this.columnPadding * 3;

        this.defaultColor       = this.getResources().getColor(android.R.color.darker_gray);
        this.highlightColor     = this.getResources().getColor(R.color.colorPrimary);
        this.backgroundColor = this.getResources().getColor(android.R.color.white);
        this.oddBackgroundColor = this.getResources().getColor(R.color.colorAccentBG);
    }

    public boolean flushPackets() {
        boolean needsRepaint = false;
        boolean needsLayout = false;
        for (int i = this.packets.size() - 1; i >= 0; i--) {
            SniffedCanPacket packet = this.packets.valueAt(i);
            if (packet.isExpired(3000)) {
                this.packets.remove(packet.getId());
                needsLayout = needsRepaint = true;
            } else if (!packet.isExpired(1000)) {
                needsRepaint = true;
            }
        }
        if (needsLayout) {
            this.requestLayout();
        }
        return needsRepaint;
    }

    public boolean updatePacket(SerialCanPacket serialPacket) {
        long id = serialPacket.getCanId();
        SniffedCanPacket packet = this.packets.get(id);
        if (packet == null) {
            packet = new SniffedCanPacket(serialPacket);
            this.packets.append(id, packet);
            this.requestLayout();
            return true;
        }
        else {
            return packet.update(serialPacket.getDataRaw());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = Math.round(this.idWidth + this.byteWidth * 8);
        int desiredHeight = Math.max(Math.round(this.packets.size() * this.rowHeight), ((View)this.getParent()).getHeight());

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        this.byteWidth = (w - this.idWidth) / 8;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        this.drawBackground(canvas, this.paint);

        for (int i = 0; i < this.packets.size(); i++) {
            drawPacket(this.packets.valueAt(i), i, canvas, this.textPaint);
        }
    }

    private void drawPacket(SniffedCanPacket packet, int rowIndex, Canvas canvas, TextPaint paint) {
        float rowTop = rowIndex * this.rowHeight;
        float rawTop = rowTop + this.columnPadding;
        float hexTop = rawTop + this.fontSize + this.columnPadding / 2;
        float binTop = hexTop + this.fontSize + this.columnPadding / 2;

        paint.setColor(this.defaultColor);

        canvas.drawLine(this.columnPadding, rowTop, this.getWidth() - this.columnPadding, rowTop, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(this.fontSize);

        paint.setColor(this.getColorForTimeFrame(packet.getAge(), 1000f, this.highlightColor, this.defaultColor));
        this.drawLabelMiddle(packet.getIdHex(), this.idWidth - this.columnPadding, rowTop + this.rowHeight / 2, paint, canvas);

        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 8; i++) {
            SniffedCanPacket.DataByte dataByte = packet.getByte(i);
            if (dataByte == null) {
                break;
            }

            paint.setTextSize(this.fontSize);
            paint.setColor(this.getColorForTimeFrame(dataByte.getAge(), 1000f, this.highlightColor, this.defaultColor));
            this.drawLabelTop(dataByte.getRaw(), this.byteWidth * i + this.idWidth + (this.byteWidth / 2), rawTop, paint, canvas);
            this.drawLabelTop(dataByte.getHex(), this.byteWidth * i + this.idWidth + (this.byteWidth / 2), hexTop, paint, canvas);
            paint.setTextSize(this.smallFontSize);
            this.drawLabelTop(dataByte.getBinary(), this.byteWidth * i + this.idWidth + (this.byteWidth / 2), binTop, paint, canvas);
        }
    }

    private int getColorForTimeFrame(long time, float duration, int color1, int color2) {
        return (int)evaluator.evaluate(Math.min(Math.max((time - 100) / duration, 0), 1), color1, color2);
    }

    private void drawLabelMiddle(String text, float x, float y, TextPaint paint, Canvas canvas) {
        canvas.drawText(text, x, y - paint.ascent() / 2.5f, paint);
    }

    private void drawLabelTop(String text, float x, float y, TextPaint paint, Canvas canvas) {
        canvas.drawText(text, x, y - paint.ascent(), paint);
    }

    private void drawBackground(Canvas canvas, Paint paint) {
        canvas.drawColor(this.backgroundColor, PorterDuff.Mode.SRC_OVER);

        paint.setColor(this.oddBackgroundColor);
        for (int i = 0; i <= 8; i += 2) {
            canvas.drawRect(this.byteWidth * i + this.idWidth, 0f, this.byteWidth * (i+1) + this.idWidth, this.getHeight(), paint);
        }
    }
}
