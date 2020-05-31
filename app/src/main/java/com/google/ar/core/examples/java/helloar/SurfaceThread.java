package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SurfaceThread extends SurfaceView implements View.OnTouchListener, SurfaceHolder.Callback, Runnable {

    private static final String TAG = SurfaceThread.class.getSimpleName();
    private Thread thread;

    private SurfaceHolder surfaceHolder = null;

    private Paint paint;
    private Paint dotPaint;
    private Paint touchPaint;

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

    private boolean oldValsInit = false;
    private int oldCircleX;
    private int oldCircleY;

    private int fittsStage;
    private int prevFittsStage;
    private static final int FITTS_STAGE_SMALL = 222220;
    private static final int FITTS_STAGE_MEDIUM = 222221;
    private static final int FITTS_STAGE_LARGE = 222222;


    // For fitts
    private ArrayList<ArrayList> missClicks;
    private ArrayList<ArrayList> hitClicks;
    private Paint fittsPaint;
    private int fittsRadius;
    private int fittsX;
    private int fittsY;
    private int hits;
    private boolean FittsStarted;
    private int T1L1C;
    private int T1L2C;
    private int T1L3C;
    private int T1L4C;
    private int T2L1C;
    private int T2L2C;
    private int T2L3C;
    private int T2L4C;
    private int T3L1C;
    private int T3L2C;
    private int T3L3C;
    private int T3L4C;
    private int T4L1C;
    private int T4L2C;
    private int T4L3C;
    private int T4L4C;


    private int CURRENT_SCREEN_AREA;
    private int prevArea;
    private final static int SCREEN_T1_L1 = 1000000;
    private final static int SCREEN_T1_L2 = 1000001;
    private final static int SCREEN_T1_L3 = 1000002;
    private final static int SCREEN_T1_L4 = 1000003;
    private final static int SCREEN_T2_L1 = 1000004;
    private final static int SCREEN_T2_L2 = 1000005;
    private final static int SCREEN_T2_L3 = 1000006;
    private final static int SCREEN_T2_L4 = 1000007;
    private final static int SCREEN_T3_L1 = 1000008;
    private final static int SCREEN_T3_L2 = 1000009;
    private final static int SCREEN_T3_L3 = 1000010;
    private final static int SCREEN_T3_L4 = 1000011;
    private final static int SCREEN_T4_L1 = 1000012;
    private final static int SCREEN_T4_L2 = 1000013;
    private final static int SCREEN_T4_L3 = 1000014;
    private final static int SCREEN_T4_L4 = 1000015;

    // used to count how many circles in each area
    private HashMap areaCounters;
    private Random rand;

    // the amount of circles to be drawn in each area of the screen
    private int fittsPartMax;
    private ArrayList AreasList;

    private final static int FITTS_RADIUS_SMALL = 40;
    private final static int FITTS_RADIUS_MEDIUM = 60;
    private final static int FITTS_RADIUS_LARGE = 80;

    private long lastHitTime;
    private ArrayList nextPos;
    private ArrayList prevPos;

    public SurfaceThread(Context context) {
        super(context);
        setFocusable(true);

        // Get SurfaceHolder object.
        surfaceHolder = this.getHolder();
        // Add current object as the callback listener.
        surfaceHolder.addCallback(this);


        // Create the paint object which will draw the text.
        paint = new Paint();

        // paint for fitts
        fittsPaint = new Paint();
        fittsPaint.setColor(Color.BLUE);
        fittsRadius = FITTS_RADIUS_SMALL;
        hits = 0;

        rand = new Random();


        // Set the SurfaceView object at the top of View object.
        setZOrderOnTop(true);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "hi");
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

        fittsPartMax = 1;

        // create touchListener for fitts
        this.setOnTouchListener(this);
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setAlpha(130);
        dotPaint = new Paint();
        dotPaint.setColor(Color.BLACK);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        touchPaint = new Paint();
        touchPaint.setColor(Color.RED);
        updateFittsPos();
        CURRENT_SCREEN_AREA = 0;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    public void finishFitts(){
        FittsStarted = false;
        Intent intent = new Intent();
        intent.putExtra("fittsMisses", missClicks);
        intent.putExtra("fittsHits", hitClicks);
        intent.setAction("com.android.activity.SEND_DATA");
        this.getContext().sendBroadcast(intent);
        Log.i(TAG, "broadcast sent");
    }

    public void updateFittsPos(){

            // get a list of all areas we can select
            Set keySet = areaCounters.keySet();
            List keyList = new ArrayList<>(keySet);
            if(keyList.size() == 0){
                // check our fittsRadius
                switch(fittsStage){
                    case FITTS_STAGE_SMALL:
                        // go to medium stage - reset hashmap
                        fittsStage = FITTS_STAGE_MEDIUM;
                        fittsRadius = FITTS_RADIUS_MEDIUM;
                        prevFittsStage = FITTS_STAGE_SMALL;
                        resetAreasFitts();
                        updateFittsPos();
                        break;
                    case FITTS_STAGE_MEDIUM:
                        fittsRadius = FITTS_RADIUS_LARGE;
                        fittsStage = FITTS_STAGE_LARGE;
                        prevFittsStage = FITTS_STAGE_MEDIUM;
                        resetAreasFitts();
                        updateFittsPos();
                        break;
                    case FITTS_STAGE_LARGE:
                        // finished
                        finishFitts();

                        break;
                    default:
                        Log.e(TAG, "How did this happen?");
                        break;
                }
            }
            else{
                int nextArea = (int) keyList.get(rand.nextInt(keyList.size()));
                while(nextArea == prevArea){
                    // if we selected same area as last time, find a new one
                    nextArea = (int) keyList.get(rand.nextInt(keyList.size()));
                }
                // get a position in nextArea
                nextPos = randomPosInArea(nextArea);
                // update the counter of this new area
                if( (int) areaCounters.get(nextArea) <fittsPartMax){
                    areaCounters.put(nextArea, (int) areaCounters.get(nextArea) + 1);
                    // did it fill? If we reached max, remove it from hashmap so it does not appear in our keyset of areas to select
                    if( (int) areaCounters.get(nextArea) == fittsPartMax){
                        areaCounters.remove(nextArea);
                    }
                }
                fittsX = (int) nextPos.get(0);
                fittsY = (int) nextPos.get(1);
            }
    }


    public ArrayList randomPosInArea(int areaPos){
        ArrayList coordinates = new ArrayList();
        int x_cord;
        int y_cord;
        switch (areaPos){
            case SCREEN_T1_L1:
                x_cord = rand.nextInt(screenWidth/4);
                y_cord = rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T1_L2:
                x_cord = screenWidth/4 + rand.nextInt(screenWidth/4);
                y_cord = rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T1_L3:
                x_cord = screenWidth/4*2 + rand.nextInt(screenWidth/4);
                y_cord = rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T1_L4:
                x_cord = screenWidth/4*3 + rand.nextInt(screenWidth/4);
                y_cord = rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T2_L1:
                x_cord = rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T2_L2:
                x_cord = screenWidth/4 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T2_L3:
                x_cord = screenWidth/4*2 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T2_L4:
                x_cord = screenWidth/4*3 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T3_L1:
                x_cord = rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*2 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T3_L2:
                x_cord = screenWidth/4 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*2 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T3_L3:
                x_cord = screenWidth/4*2 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*2 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T3_L4:
                x_cord = screenWidth/4*3 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*2 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T4_L1:
                x_cord = rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*3 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T4_L2:
                x_cord = screenWidth/4 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*3 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T4_L3:
                x_cord = screenWidth/4*2 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*3 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            case SCREEN_T4_L4:
                x_cord = screenWidth/4*3 + rand.nextInt(screenWidth/4);
                y_cord = screenHeight/4*3 + rand.nextInt(screenHeight/4);
                coordinates.add(x_cord);
                coordinates.add(y_cord);
                return coordinates;
            default:
                coordinates.add(0);
                coordinates.add(0);
                return coordinates;


        }
    }

    public int getScreenArea(int x, int y){
        if(x <= screenWidth/4){
            // L1
            if(y <= screenHeight/4){
                // T1
                return SCREEN_T1_L1;
            }
            else if (y <= screenHeight/4*2){
                // T2
                return SCREEN_T2_L1;
            }
            else if (y <= screenHeight/4*3){
                // T3
                return SCREEN_T3_L1;
            }
            else{
                // T4
                return SCREEN_T4_L1;
            }
        }
        else if (x <= screenWidth/4*2){
            // L2
            if(y <= screenHeight/4){
                // T1
                return SCREEN_T1_L2;
            }
            else if (y <= screenHeight/4*2){
                // T2
                return SCREEN_T2_L2;
            }
            else if (y <= screenHeight/4*3){
                // T3
                return SCREEN_T3_L2;
            }
            else{
                // T4
                return SCREEN_T4_L2;
            }
        }
        else if( x <= screenWidth/4*3){
            // L3
            if(y <= screenHeight/4){
                // T1
                return SCREEN_T1_L3;
            }
            else if (y <= screenHeight/4*2){
                // T2
                return SCREEN_T2_L3;

            }
            else if (y <= screenHeight/4*3){
                // T3
                return SCREEN_T3_L3;

            }
            else{
                // T4
                return SCREEN_T4_L3;

            }
        }
        else{
            // L4
            if(y <= screenHeight/4){
                // T1
                return SCREEN_T1_L4;

            }
            else if (y <= screenHeight/4*2){
                // T2
                return SCREEN_T2_L4;

            }
            else if (y <= screenHeight/4*3){
                // T3
                return SCREEN_T3_L4;

            }
            else{
                // T4
                return SCREEN_T4_L4;

            }
        }
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
                        touchRadius -= 1;
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
                    downTime + 50 ,
                    eventTime+ 50,
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

            // clear BG
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // draw Fitts
            if(FittsStarted){
                canvas.drawCircle(fittsX, fittsY, fittsRadius, fittsPaint);
            }

            // draw circle
            canvas.drawCircle(circleX, circleY, 25, paint);
            canvas.drawCircle(circleX, circleY, 5, dotPaint);


            // Send message to main UI thread to update the drawing to the main view special area.
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
        else{
            Canvas canvas = surfaceHolder.lockCanvas();

            // Draw the specify canvas background color.


            // clear BG
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);



            // draw Fitts
            if(FittsStarted){
                canvas.drawCircle(fittsX, fittsY, fittsRadius, fittsPaint);
            }

            // draw circle
            canvas.drawCircle(touchX, touchY, touchRadius, touchPaint);
            canvas.drawCircle(touchX, touchY, 5, dotPaint);


            // Send message to main UI thread to update the drawing to the main view special area.
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }



    public void setCirclePos(int x, int y, float slow){


        if(!oldValsInit){
            circleX = x;
            circleY = y;
            oldValsInit = true;
        }
        else{
            oldCircleX = circleX;
            oldCircleY = circleY;
        }
        if(slow <= 0) {
            if (x > oldCircleX) {
                    circleX = oldCircleX + 1;
                    oldCircleX = circleX;
                }
            else{
                    circleX = oldCircleX - 1;
                    oldCircleX = circleX;
                }
            if (y > oldCircleY) {
                    circleY = oldCircleY + 1;
                    oldCircleY = circleY;
                }
            else {
                    circleY = oldCircleY - 1;
                    oldCircleY = circleY;
                }

        }
        else if(slow >=1){
            circleX = x;
            circleY = y;
        }
        else if(slow <2 && slow >0){
            if (x > oldCircleX) {

                //circleX += Math.floor((x-oldCircleX)*slow);
                circleX += Math.floor(7)*slow;
                if(circleX > x){
                    circleX = x;
                }
                oldCircleX = circleX;
            }
            else{
                //circleX -= Math.floor((oldCircleX-x)*slow);
                circleX -= Math.floor(7)*slow;
                if(circleX < x){
                    circleX = x;
                }
                oldCircleX = circleX;
            }
            if (y > oldCircleY) {
                //circleY += Math.floor((y-oldCircleY)*slow);
                circleY += Math.floor(7)*slow;
                if(circleY > y){
                    circleY = y;
                }
                oldCircleY = circleY;
            }
            else {
                //circleY -= Math.floor((oldCircleY-y)*slow);
                circleY -= Math.floor(7)*slow;
                if(circleY < y){
                    circleY = y;
                }
                oldCircleY = circleY;
            }
        }

    }

    public void resetAreasFitts(){
        prevArea = 0;
        areaCounters = new HashMap();
        areaCounters.put(SCREEN_T1_L1, 0);
        areaCounters.put(SCREEN_T1_L2, 0);
        areaCounters.put(SCREEN_T1_L3, 0);
        areaCounters.put(SCREEN_T1_L4, 0);
        areaCounters.put(SCREEN_T2_L1, 0);
        areaCounters.put(SCREEN_T2_L2, 0);
        areaCounters.put(SCREEN_T2_L3, 0);
        areaCounters.put(SCREEN_T2_L4, 0);
        areaCounters.put(SCREEN_T3_L1, 0);
        areaCounters.put(SCREEN_T3_L2, 0);
        areaCounters.put(SCREEN_T3_L3, 0);
        areaCounters.put(SCREEN_T3_L4, 0);
        areaCounters.put(SCREEN_T4_L1, 0);
        areaCounters.put(SCREEN_T4_L2, 0);
        areaCounters.put(SCREEN_T4_L3, 0);
        areaCounters.put(SCREEN_T4_L4, 0);
    }

    public void startFitts(){
        FittsStarted = true;
        missClicks = new ArrayList<ArrayList>();
        hitClicks = new ArrayList<ArrayList>();
        hits = 0;
        prevArea = 0;
        prevFittsStage = 0;

        // reset counters
        T1L1C = 0;
        T1L2C = 0;
        T1L3C = 0;
        T1L4C = 0;
        T2L1C = 0;
        T2L2C = 0;
        T2L3C = 0;
        T2L4C = 0;
        T3L1C = 0;
        T3L2C = 0;
        T3L3C = 0;
        T3L4C = 0;
        T4L1C = 0;
        T4L2C = 0;
        T4L3C = 0;
        T4L4C = 0;

        CURRENT_SCREEN_AREA = 0;

        AreasList = new ArrayList();
        AreasList.add(SCREEN_T1_L1);
        AreasList.add(SCREEN_T1_L2);
        AreasList.add(SCREEN_T1_L3);
        AreasList.add(SCREEN_T1_L4);
        AreasList.add(SCREEN_T2_L1);
        AreasList.add(SCREEN_T2_L2);
        AreasList.add(SCREEN_T2_L3);
        AreasList.add(SCREEN_T2_L4);
        AreasList.add(SCREEN_T3_L1);
        AreasList.add(SCREEN_T3_L2);
        AreasList.add(SCREEN_T3_L3);
        AreasList.add(SCREEN_T3_L4);
        AreasList.add(SCREEN_T4_L1);
        AreasList.add(SCREEN_T4_L2);
        AreasList.add(SCREEN_T4_L3);
        AreasList.add(SCREEN_T4_L4);

        areaCounters = new HashMap();
        areaCounters.put(SCREEN_T1_L1, 0);
        areaCounters.put(SCREEN_T1_L2, 0);
        areaCounters.put(SCREEN_T1_L3, 0);
        areaCounters.put(SCREEN_T1_L4, 0);
        areaCounters.put(SCREEN_T2_L1, 0);
        areaCounters.put(SCREEN_T2_L2, 0);
        areaCounters.put(SCREEN_T2_L3, 0);
        areaCounters.put(SCREEN_T2_L4, 0);
        areaCounters.put(SCREEN_T3_L1, 0);
        areaCounters.put(SCREEN_T3_L2, 0);
        areaCounters.put(SCREEN_T3_L3, 0);
        areaCounters.put(SCREEN_T3_L4, 0);
        areaCounters.put(SCREEN_T4_L1, 0);
        areaCounters.put(SCREEN_T4_L2, 0);
        areaCounters.put(SCREEN_T4_L3, 0);
        areaCounters.put(SCREEN_T4_L4, 0);

        fittsStage = FITTS_STAGE_SMALL;
        lastHitTime = System.currentTimeMillis();
        updateFittsPos();






    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "Touched: x: " + event.getX() + " y: " + event.getY());
        // check if fitts was touched

        if(FittsStarted){
            int touchedX = (int) event.getX();
            int touchedY = (int) event.getY();

            Log.i(TAG, "fitts: touchedX: " + touchedX + " touchedY:" + touchedY);

            if(Math.pow((touchedX - fittsX), 2) + Math.pow((touchedY - fittsY), 2) < Math.pow(fittsRadius, 2)){
                hits += 1;
                ArrayList hitCoordinates = new ArrayList();
                // get time since last hit
                long newHitTime = System.currentTimeMillis();
                long timeToHit = newHitTime - lastHitTime;
                lastHitTime = newHitTime;

                // add x and y of target
                hitCoordinates.add(nextPos.get(0));
                hitCoordinates.add(nextPos.get(1));
                // add time to hit target and stage (to know size)
                hitCoordinates.add(timeToHit);



                if(fittsStage == FITTS_STAGE_SMALL){
                    hitCoordinates.add("SMALL");
                }
                else if(fittsStage == FITTS_STAGE_MEDIUM){
                    hitCoordinates.add("MEDIUM");
                }
                else if(fittsStage == FITTS_STAGE_LARGE){
                    hitCoordinates.add("LARGE");
                }
                else{
                    hitCoordinates.add("WHAT?");
                }

                if(prevPos == null){
                    // fitts just started, no previous position
                    hitCoordinates.add(null);
                    hitCoordinates.add(null);

                }
                else{
                    // add x and y of previous position - can be used to calculate distance
                    hitCoordinates.add(prevPos.get(0));
                    hitCoordinates.add(prevPos.get(1));
                }

                // add previous stage so we know what the radius was
                if(prevFittsStage == 0){
                    hitCoordinates.add(null);
                }
                else if(prevFittsStage == FITTS_STAGE_SMALL){
                    hitCoordinates.add("small");
                }
                else if(prevFittsStage == FITTS_STAGE_MEDIUM){
                    hitCoordinates.add("medium");
                }
                else {
                    hitCoordinates.add("what");
                }

                // format of hitcoordinates: [0] = x of hit [1] = y of hit, [2] = time to hit, [3] = currentStage, [4] = prevPos x, [5]= prevPos y, [6] = prevStage
                hitClicks.add(hitCoordinates);




                // save this position as the previous position
                prevPos = nextPos;
                updateFittsPos();
            }
            else{
                // missed the circle
                Log.i(TAG, "missed: " + "touchedX: " +touchedX + " touchedY: " + touchedY);
                ArrayList missCoordinates = new ArrayList();
                missCoordinates.add(touchedX);
                missCoordinates.add(touchedY);
                if(fittsStage == FITTS_STAGE_SMALL){
                    missCoordinates.add("SMALL");
                }
                else if (fittsStage == FITTS_STAGE_MEDIUM){
                    missCoordinates.add("MEDIUM");
                }
                else if (fittsStage == FITTS_STAGE_LARGE){
                    missCoordinates.add("LARGE");
                }
                else{
                    missCoordinates.add("ERROR-STAGE");
                }
                missClicks.add(missCoordinates);
                Log.i(TAG, "finished miss");
            }
        }

        return false;
    }
}
