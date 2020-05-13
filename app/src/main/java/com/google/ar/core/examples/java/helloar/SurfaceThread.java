package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfaceThread extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = SurfaceThread.class.getSimpleName();
    private Thread thread;

    private SurfaceHolder surfaceHolder = null;

    private Paint paint;

    private int screenWidth = 0;

    private int screenHeight = 0;

    private boolean threadRunning = false;

    private int circleX = 50;
    private int circleY = 50;


    public SurfaceThread(Context context) {
        super(context);
        setFocusable(true);

        // Get SurfaceHolder object.
        surfaceHolder = this.getHolder();
        // Add current object as the callback listener.
        surfaceHolder.addCallback(this);


        // Create the paint object which will draw the text.
        paint = new Paint();


        // Set the SurfaceView object at the top of View object.
        setZOrderOnTop(true);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Create the child thread when SurfaceView is created.
        thread = new Thread(this);
        // Start to run the child thread.
        thread.start();
        // Set thread running flag to true.
        threadRunning = true;

        // Get screen width and height.
        screenHeight = getHeight();
        screenWidth = getWidth();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        threadRunning = false;
    }

    @Override
    public void run() {
        while(threadRunning)
        {
            Log.i(TAG, "REDRAW" + circleY + " - " + circleX);
            long startTime = System.currentTimeMillis();


            if(circleX > screenWidth)
            {
                circleX = screenWidth;
            }

            if(circleY > screenHeight)
            {
                circleY = screenHeight;
            }

            drawCircle();

            long endTime = System.currentTimeMillis();

            long deltaTime = endTime - startTime;

            if(deltaTime < 30)
            {
                try {
                    Thread.sleep(30 - deltaTime);
                }catch (InterruptedException ex)
                {
                    Log.e(TAG, ex.getMessage());
                }

            }
        }
    }

    private void drawCircle()
    {
        int margin = 100;

        int left = margin;

        int top = margin;

        int right = screenWidth - margin;

        int bottom = screenHeight - margin;

        Canvas canvas = surfaceHolder.lockCanvas();

        // Draw the specify canvas background color.
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        Paint CirclePaint = new Paint();
        // clear BG
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        CirclePaint.setColor(Color.YELLOW);

        // draw circle
        canvas.drawCircle(circleX, circleY, 25, CirclePaint);


        // Send message to main UI thread to update the drawing to the main view special area.
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    public void setCirclePos(int x, int y){

        if (Math.abs(circleX)-Math.abs(x) > 10){
            
        }
        circleX = x;
        circleY = y;
    }

}
