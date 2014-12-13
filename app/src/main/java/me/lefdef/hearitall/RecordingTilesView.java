package me.lefdef.hearitall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Brigham on 12/13/2014.
 */
public class RecordingTilesView extends TextView implements View.OnTouchListener{
    Context appContext; // reference to main activity context
    public RecordingTilesView(Context context) {
        super(context);
        appContext = context;
        setFocusable(true);
        setFocusableInTouchMode(true);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        RectF rect = new RectF();
        rect.left = getPaddingLeft();
        rect.top = getPaddingTop();
        rect.bottom = getPaddingBottom();
        rect.right = getPaddingRight();
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.BLUE);

        canvas.drawRect(rect, rectPaint);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        int minh = (40);
        int h = minh;
        setMeasuredDimension(w, h);
    }
}