package me.lefdef.hearitall;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Brigham on 12/14/2014.
 */
public class CircleButton extends ImageView {

    private int _centerX;
    private int _centerY;
    private int _radius;
    private String _label = "";
    private Paint _circlePaint;
    private Paint _textPaint;


    public CircleButton(Context context) {
        super(context);
        initializeComponents(context, null);
    }
    public CircleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeComponents(context, attrs);
    }

    public CircleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeComponents(context, attrs);
    }

    public void setLabel(String label) {
        _label = label;
        invalidate();
    }
    public void setColor(int color) {
        _circlePaint.setColor(color);

        invalidate();
    }

    private void initializeComponents(Context context, AttributeSet attrs) {
        this.setFocusable(true);
        this.setScaleType(ScaleType.CENTER_INSIDE);
        setClickable(true);

        int circleColor = Color.parseColor("#F50057"); // pink

        if(attrs != null) {
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleButton);
            circleColor = typedArray.getColor(R.styleable.CircleButton_cb_color, circleColor);
            _label = typedArray.getString(R.styleable.CircleButton_cb_label);
            typedArray.recycle();
        }

        _circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _circlePaint.setStyle(Paint.Style.FILL);
        _circlePaint.setColor(circleColor);

        _textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        _textPaint.setColor(circleColor == Color.WHITE ? Color.BLACK : Color.WHITE); // don't make text same color as circle
        _textPaint.setTextSize(20);
        _textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(_centerX, _centerY, _radius, _circlePaint);
        if(!_label.equals("")) {
            canvas.drawText(_label, _centerX, (int)(1.8 * _radius), _textPaint);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        _centerX = w / 2;
        _centerY = h / 2;
        _radius = Math.min(w, h) / 2;
    }
}
