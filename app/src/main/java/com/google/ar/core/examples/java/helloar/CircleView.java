package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class CircleView extends SurfaceView {
    public Rect rect;
    public int x;
    public int y;
    public int radius;


    public CircleView(Context context) {
        super(context);
        init(null);
        setWillNotDraw(false);
    }

    public CircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

    }

    public CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);

    }

    public CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);

    }

    private void init(@Nullable AttributeSet set){

    }

    public void updatePos(int x, int y, int radius){
        System.out.println("updatePos:" + x + " " + y);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.postInvalidate();

    }

    @Override
    protected void onDraw(Canvas canvas){

        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        System.out.println("Drawing circle on:" + " " + this.x + " " + this.y);
        canvas.drawCircle(this.x, this.y, this.radius, paint);
    }
}
