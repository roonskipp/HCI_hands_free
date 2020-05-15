package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
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

    private int touchX = 0;
    private int touchY = 0;

    private int touchRadius = 40;

    private boolean touched = false;
    private boolean touchedTimerStarted = false;

    private long touchTime;


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
        // setZOrderOnTop(true);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holder.setFormat(PixelFormat.TRANSPARENT);

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

            if(touched){
                if(!touchedTimerStarted){
                    touchTime = System.currentTimeMillis();
                    touchedTimerStarted = true;
                }
                else{
                    long currTime = System.currentTimeMillis();
                    if(currTime - touchTime > 500){
                        touched = false;
                        touchedTimerStarted = false;
                        touchRadius = 40;
                    }
                    else{
                        touchRadius -= 3;
                    }
                }
            }

            //Log.i(TAG, "REDRAW" + circleY + " - " + circleX);
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

    public void touchScreen(){
        if(!touched) {
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis() + 100;
            int metaState = 0;

            MotionEvent motionEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_DOWN,
                    circleX,
                    circleY,
                    metaState
            );
            MotionEvent motionEvent2 = MotionEvent.obtain(
                    downTime + 105,
                    eventTime + 205,
                    MotionEvent.ACTION_UP,
                    circleX,
                    circleY,
                    metaState
            );
            this.getRootView().dispatchTouchEvent(motionEvent);
            this.getRootView().dispatchTouchEvent(motionEvent2);

            touchX = circleX;
            touchY = circleY;

            touched = true;
        }
    }



    private void drawCircle()
    {
        if(!touched) {


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
        else{
            Canvas canvas = surfaceHolder.lockCanvas();

            // Draw the specify canvas background color.
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);
            Paint TouchPaint = new Paint();
            // clear BG
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            TouchPaint.setColor(Color.RED);


            // draw circle
            canvas.drawCircle(touchX, touchY, touchRadius, TouchPaint);


            // Send message to main UI thread to update the drawing to the main view special area.
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }



    public void setCirclePos(int x, int y){

        circleX = x;
        circleY = y;

    }

}
