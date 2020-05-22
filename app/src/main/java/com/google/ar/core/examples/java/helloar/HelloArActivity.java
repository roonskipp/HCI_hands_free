/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.camera2.CaptureRequest;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.common.rendering.ObjectRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.math.Vector3;


import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer{
  private static final String TAG = HelloArActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private boolean meshBtnClicked;


  // Used to calculate where to draw cursor
  private int deviceHeight;
  private int deviceWidth;

  private SurfaceHolder surfaceHolder;

  // Looper handler.
  private Handler backgroundHandler;

  // Looper handler thread.
  private HandlerThread backgroundThread;

  // Prevent any changes to camera capture session after CameraManager.openCamera() is called, but
  // before camera device becomes active.
  private boolean captureSessionChangesPossible = true;


  // used to display face orientation vectors on screen
  private TextView textView;

  private SurfaceThread surfaceThread;

  private Switch debugSwitch;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;

  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final ObjectRenderer virtualObject = new ObjectRenderer();


  // A list of CaptureRequest keys that can cause delays when switching between AR and non-AR modes.
  private List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] anchorMatrix = new float[16];

    private boolean calibrated = false;
    private boolean calibratedClosed = false;
    private boolean calibratedOpen = false;

  private ArrayList<ArrayList<Float>> faces_list;

  private ArrayList mouthValues;
  private float closedMouthVal;
  private float openMouthVal;
  private boolean calibrateClicked = false;

  private Button calibrateMouthBtn;
  private Button clickMe;

  private double sensitivitySlow = 0.4;
  private double sensitivityOpen = 1.2;

  private Button increaseSenSlowBtn;
  private Button decreaseSenSlowBtn;
  private Button increaseSenOpenBtn;
  private Button decreaseSenOpenBtn;
  private Button meshBtn;

  private TextView senText;
  private TextView touchInfo;

  private TextView yDifText;

  private boolean calibratedMouth = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);

    calibrateMouthBtn = findViewById(R.id.calibrateMouth);

    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    getDeviceMetrics();

    debugSwitch = findViewById(R.id.simpleSwitch);


    surfaceThread = new SurfaceThread(this);

    // Get text drawing LinearLayout canvas.
    LinearLayout drawCircleCanvas = (LinearLayout)findViewById(R.id.drawCircleCanvas);

    // Add surfaceview object to the LinearLayout object.
    drawCircleCanvas.addView(surfaceThread);



    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(true);

    this.meshBtn = findViewById(R.id.displayMeshBtn);

    installRequested = false;
    meshBtnClicked = true;
    textView = findViewById(R.id.vectorText);
    senText = findViewById(R.id.senText);
    touchInfo = findViewById(R.id.touchInfo);
    textView.setText("HELLO");

    yDifText = findViewById(R.id.yDifText);
    increaseSenOpenBtn = findViewById(R.id.increaseSenOpenBtn);
    decreaseSenOpenBtn = findViewById(R.id.decreaseOpenSenBtn);
    clickMe = findViewById(R.id.clickMe);

    increaseSenSlowBtn = findViewById(R.id.increaseSenSlowBtn);
    decreaseSenSlowBtn = findViewById(R.id.decreaseSlowSenBtn);

  }


  protected void onMeshBtnClick(){

  }

  @Override
  protected void onResume() {
    super.onResume();
    this.updateSenText();
    this.updateTouchInfo();



      waitUntilCameraCaptureSessionIsActive();

    //startBackgroundThread();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
          case INSTALL_REQUESTED:
            installRequested = true;
            return;
          case INSTALLED:
            break;
        }

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(/* context= */ this, EnumSet.of(Session.Feature.FRONT_CAMERA));
        Config config = new Config(session);
        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
        session.configure(config);

      } catch (UnavailableArcoreNotInstalledException
          | UnavailableUserDeclinedInstallationException e) {
        message = "Please install ARCore";
        exception = e;
      } catch (UnavailableApkTooOldException e) {
        message = "Please update ARCore";
        exception = e;
      } catch (UnavailableSdkTooOldException e) {
        message = "Please update this app";
        exception = e;
      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "This device does not support AR";
        exception = e;
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        return;
      }
    }

    // Note that order matters - see the note in onPause(), the reverse applies here.
    try {
      session.resume();
    } catch (CameraNotAvailableException e) {
      // In some cases (such as another camera app launching) the camera may be given to
      // a different app instead. Handle this properly by showing a message and recreate the
      // session at the next iteration.
      messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
      session = null;
      return;
    }

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  // Start background handler thread, used to run callbacks without blocking UI thread.
  private void startBackgroundThread() {
    backgroundThread = new HandlerThread("sharedCameraBackground");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
  }


  private synchronized void waitUntilCameraCaptureSessionIsActive() {
    while (!captureSessionChangesPossible) {
      try {
        this.wait();
      } catch (InterruptedException e) {
        Log.e(TAG, "Unable to wait for a safe time to make changes to the capture session", e);
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      // Note that the order matters - GLSurfaceView is paused first so that it does not try
      // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
      // still call session.update() and get a SessionPausedException.
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    Log.d(TAG, "onSurfaceCreated");
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
    try {
      // Create the texture and pass it to ARCore session to be filled during update().
      backgroundRenderer.createOnGlThread(/*context=*/ this);

      virtualObject.createOnGlThread(/*context=*/ this,  "models/face-texture.png");
      virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

    } catch (IOException e) {
      Log.e(TAG, "Failed to read an asset file", e);
    }
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    GLES20.glViewport(0, 0, width, height);
  }


  @Override
  public void onDrawFrame(GL10 gl) {
    // Clear screen to notify driver it should not load any pixels from previous frame.

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    if (session == null) {
      return;
    }


    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.

    displayRotationHelper.updateSessionIfNeeded(session);

    try {
      session.setCameraTextureName(backgroundRenderer.getTextureId());

      // Obtain the current frame from ARSession. When the configuration is set to
      // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
      // camera framerate.
      Frame frame = session.update();

      Camera camera = frame.getCamera();


      // If frame is ready, render camera preview image to the GL surface.
      // UNCOMMENT THE LINE BELOW TO RENDER CAMERAFEED

      backgroundRenderer.draw(frame);

      // Get projection matrix.
      float[] projmtx = new float[16];
      camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

      // Get camera matrix and draw.
      float[] viewmtx = new float[16];
      camera.getViewMatrix(viewmtx, 0);

      // Compute lighting from average intensity of the image.
      // The first three components are color scaling factors.
      // The last one is the average pixel intensity in gamma space.
      final float[] colorCorrectionRgba = new float[4];
      frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);



      for (AugmentedFace face : session.getAllTrackables(AugmentedFace.class)) {
        virtualObject.setToAugmentedFace(face);
        float[] color4f = { 1f, 1f, 1f, 1f };

        face.getCenterPose().toMatrix(anchorMatrix, 0);

        // Update and draw the model and its shadow.
        if (meshBtnClicked){
          virtualObject.updateModelMatrix(anchorMatrix, 1f);

          // UNCOMMENT THE LINE BELOW TO RENDER MESH (I think)
          virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, color4f);
          updateFaceOrientation(face);

          //checkEyeBlink(face);


        }
        else{
          return;
        }

      }

    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
  }

  public void updateTouchInfo(){
      this.touchInfo.setText("To touch yDif must be:" + (float)(this.openMouthVal-this.closedMouthVal)*sensitivityOpen + "to go slow yDic must be:" + (this.openMouthVal-this.closedMouthVal)*this.sensitivitySlow);
  }

  public void updateYDifText(float yDif){
    this.yDifText.setText(String.valueOf(yDif));
  }

  public float checkMouthOpen(AugmentedFace face){
    FloatBuffer buffer = face.getMeshVertices();
    Vector3 upperLipVec = new Vector3(buffer.get(39), buffer.get(40), buffer.get(41));
    Vector3 lowerLipVec = new Vector3(buffer.get(42), buffer.get(43), buffer.get(44));
    //Log.i(TAG, "UpperLip:" + upperLipVec);
    //Log.i(TAG, "LowerLip:" + lowerLipVec);

    float currentGap = upperLipVec.y - lowerLipVec.y;       // current gap
    float neededGap = (float) ((this.openMouthVal-this.closedMouthVal)*sensitivityOpen);  // Gap needed to click
    float slowGap = (float)((this.openMouthVal-this.closedMouthVal)*this.sensitivitySlow);
    float totalGap = (float) neededGap - slowGap; // gap between slow starts and click appears

    if(currentGap > neededGap){
      Log.i(TAG, "MOUTH OPEN:");
      surfaceThread.touchScreen();
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                updateYDifText(0);

            }
        });
        return 0;
    }
    else if( currentGap > slowGap){ // mouth is somewhat open, we want to slow down the cursor
        float partGap = currentGap - slowGap; // the amount we have moved past the slow threshold
        float slow = (totalGap - partGap)/totalGap; // this number approaches 0 as we move closer to the threshold, but stays close to 1 further from the gap

        // float slow = (float) (neededGap - currentGap)*100;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                updateYDifText(slow);

            }
        });
        return slow;

  }
    else{

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                updateYDifText(1);

            }
        });
        return 1;
    }

  }

  public void checkEyeBlink(AugmentedFace face){
    FloatBuffer buffer = face.getMeshVertices();
    Vector3 upperEyeLidVec = new Vector3(buffer.get(1158), buffer.get(1159), buffer.get(1160));
    Vector3 lowerEyeLidVec = new Vector3(buffer.get(1122), buffer.get(1123), buffer.get(1124));

    float upperEyeLidY = upperEyeLidVec.y;
    float lowerEyeLidY = lowerEyeLidVec.y;

    Log.i(TAG, "UpperEyelid:" + upperEyeLidY);
    Log.i(TAG, "LowerEyelid:" + lowerEyeLidY);

    float yDif = upperEyeLidY - lowerEyeLidY;

    Log.i(TAG, "EyelidGap:" + yDif);

  }

  public void getDeviceMetrics(){
    DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    deviceHeight = displayMetrics.heightPixels;
    deviceWidth = displayMetrics.widthPixels;
  }

  public void updateFaceOrientation(AugmentedFace face){
    float[] x = face.getCenterPose().getXAxis();
    float[] y = face.getCenterPose().getYAxis();
    float[] z = face.getCenterPose().getZAxis();
    if(faces_list == null){
        faces_list = new ArrayList<ArrayList<Float>>();
    }

    if(faces_list.size() == 10){
        float slow = 0;

        ArrayList curr_vals = new ArrayList<Float>();
        curr_vals.add(z[1]);
        curr_vals.add(z[0]);
        faces_list.remove(0);
        faces_list.add(curr_vals);


        if(calibratedMouth){
            slow = checkMouthOpen(face);
        }


        float average_z_1 = 0;
        float average_z_0 = 0;
        // get average of last 5 faces
        for(int i=0; i<faces_list.size(); i++){
            average_z_1 = (Float) average_z_1 + faces_list.get(i).get(0);
            average_z_0 = (Float) average_z_0 + faces_list.get(i).get(1);
        }
        average_z_1 = (average_z_1/10);
        average_z_0 = (average_z_0/10);

        // Calculate where to draw cursor
        int cursorHeight = deviceHeight/2;
        // System.out.println("Device: " + this.deviceWidth + " " + this.deviceHeight);
        if(z[1] < 0){
            cursorHeight = (int)Math.round((average_z_1*(-1)*deviceHeight*9 + deviceHeight)/2);
        }
        else{
            cursorHeight = (int)Math.round((average_z_1*(-1)*deviceHeight*7 + deviceHeight)/2);
        }
        int cursorLeft = (int)Math.round((average_z_0*(-1)*deviceWidth*6 + deviceWidth)/2);

        if(cursorHeight > deviceHeight){
            cursorHeight = deviceHeight;
        }
        else if(cursorHeight<0){
            cursorHeight = 0;
        }
        if(cursorLeft > deviceWidth){
            cursorLeft = deviceWidth;
        }
        else if(cursorLeft < 0){
            cursorLeft = 0;
        }

        if(this.calibrateClicked){
            this.calibrateMouth(face);
        }

        surfaceThread.setCirclePos(cursorLeft, cursorHeight, slow);
    }
    else{
        ArrayList curr_vals = new ArrayList<Float>();
        curr_vals.add(z[1]);
        curr_vals.add(z[0]);
        faces_list.add(curr_vals);

        float slow = checkMouthOpen(face);

        // Calculate where to draw cursor
        int cursorHeight = deviceHeight/2;
        // System.out.println("Device: " + this.deviceWidth + " " + this.deviceHeight);
        if(z[1] < 0){
            cursorHeight = (int)Math.round((z[1]*(-1)*deviceHeight*9 + deviceHeight)/2);
        }
        else{
            cursorHeight = (int)Math.round((z[1]*(-1)*deviceHeight*7 + deviceHeight)/2);
        }
        int cursorLeft = (int)Math.round((z[0]*(-1)*deviceWidth*6 + deviceWidth)/2);

        if(cursorHeight > deviceHeight){
            cursorHeight = deviceHeight;
        }
        else if(cursorHeight<0){
            cursorHeight = 0;
        }
        if(cursorLeft > deviceWidth){
            cursorLeft = deviceWidth;
        }
        else if(cursorLeft < 0){
            cursorLeft = 0;
        }



        surfaceThread.setCirclePos(cursorLeft, cursorHeight, slow);
    }


  }

  public boolean screenTouched(AugmentedFace face){
      FloatBuffer buffer = face.getMeshVertices();
      Vector3 upperLipVec = new Vector3(buffer.get(39), buffer.get(40), buffer.get(41));
      Vector3 lowerLipVec = new Vector3(buffer.get(42), buffer.get(43), buffer.get(44));
      float yDif = upperLipVec.y - lowerLipVec.y;
      this.updateYDifText(yDif);
      if(yDif > this.openMouthVal*0.3){
          Log.i(TAG, "MOUTH OPEN:");
          surfaceThread.touchScreen();
          return true;
      }
      else if( yDif > 0.0025){
          return true;
      }
      else{
          return false;
      }
  }

  public void calibrateMouth(AugmentedFace face){
      Log.d(TAG, "calibrate");
      // calibrate for closed mouth first
      // calibrate for open mouth after
      // Set a value slightly lower than average open as open trigger
      // Set a value slightly above closed for "about to open" value

      // create mouthValues if it is null
      if(mouthValues == null){
          mouthValues = new ArrayList();
      }

      // get values for upper and lower lips, calculate distance between them
      FloatBuffer buffer = face.getMeshVertices();
      Vector3 upperLipVec = new Vector3(buffer.get(39), buffer.get(40), buffer.get(41));
      Vector3 lowerLipVec = new Vector3(buffer.get(42), buffer.get(43), buffer.get(44));
      float yDif = upperLipVec.y - lowerLipVec.y;




      if(!calibratedClosed){
          // add to collection of mo
          mouthValues.add(yDif);
          float percent = (float)100* ((float)mouthValues.size()/(float)250);
          Log.d(TAG, "percent:" + String.valueOf(percent));
          textView.setText("Close Your mouth. Completed " + percent +"%");


          if(mouthValues.size() >= 250){
              // stop calibrating
              calibratedClosed = true;
              closedMouthVal = 0;
              // calculate average closed value
              for(int i = 0; i<mouthValues.size(); i++){
                  closedMouthVal += (float) mouthValues.get(i);
              }
              this.closedMouthVal = closedMouthVal/1000;
              mouthValues.clear();
          }
      }
      else if (!calibratedOpen){
          // add to collection of mo
          mouthValues.add(yDif);
          float percent = (float)100*(((float)mouthValues.size())/(float)250);
          Log.d(TAG, "percent:" + String.valueOf(percent));


          textView.setText("Open Your mouth. Completed " + percent +"%");

          if(mouthValues.size() >= 250){
              textView.setText("Calibration Done");
              // stop calibrating
              calibratedOpen = false;
              calibratedClosed = false;
              openMouthVal = 0;
              // calculate average closed value
              for(int i = 0; i<mouthValues.size(); i++){
                  openMouthVal += (float) mouthValues.get(i);
              }
              this.openMouthVal = openMouthVal/1000;
              this.calibrateClicked = false;
              this.calibratedMouth = true;
          }
      }
  }


  public void onClickMeshBtn(View view) throws CameraNotAvailableException {

    if(!meshBtnClicked){
      meshBtnClicked = true;
      System.out.println("MeshButtonClicked: " + meshBtnClicked);
    }
    else{
      meshBtnClicked = false;
      System.out.println("MeshButtonClicked: " + meshBtnClicked);
    }
  }


  public void onClickClickMe(View view) {
    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) findViewById(R.id.clickMe).getLayoutParams();

    int randomNumTop = ThreadLocalRandom.current().nextInt(20, (deviceHeight-25) + 1);
    int randomNumLeft = ThreadLocalRandom.current().nextInt( 20, (deviceWidth-50) + 1);


    params.leftMargin = randomNumLeft;
    params.topMargin = randomNumTop;

    Log.i(TAG, "TOP:" + randomNumTop);
    Log.i(TAG, "LEFT:" + randomNumLeft);


    findViewById(R.id.clickMe).setLayoutParams(params);
  }

    public void onClickCalibrateMouth(View view) {
      this.openMouthVal = 0;
      this.closedMouthVal = 0;
      if(this.mouthValues != null){
          this.mouthValues = new ArrayList();
      }
      this.calibrateClicked = true;
    }


    public void onClickIncreaseOpenSen(View view) {
      this.sensitivityOpen -= 0.05;
      this.updateSenText();

    }

    public void onClickDecreaseOpenSen(View view) {
        this.sensitivityOpen += 0.05;
        this.updateSenText();

    }

    public void onClickDecreaseSlowSen(View view) {
        this.sensitivitySlow += 0.05;
        this.updateSenText();

    }

    public void onClickIncreaseSlowSen(View view) {
        this.sensitivitySlow -= 0.05;
        this.updateSenText();

    }

    public void updateSenText(){
        senText.setText("sens. open:" + this.sensitivityOpen + "\t sens. slow: " + this.sensitivitySlow);

    }

    public void onDebugSwitch(View view) {
        Boolean switchState = this.debugSwitch.isChecked();
        if(switchState){
            this.calibrateMouthBtn.setEnabled(false);
            this.calibrateMouthBtn.setVisibility(View.INVISIBLE);
            this.increaseSenSlowBtn.setEnabled(false);
            this.increaseSenSlowBtn.setVisibility(View.INVISIBLE);
            this.increaseSenOpenBtn.setEnabled(false);
            this.increaseSenOpenBtn.setVisibility(View.INVISIBLE);
            this.meshBtn.setEnabled(false);
            this.meshBtn.setVisibility(View.INVISIBLE);
            this.textView.setVisibility(View.INVISIBLE);
            this.yDifText.setVisibility(View.INVISIBLE);
            this.touchInfo.setVisibility(View.INVISIBLE);
            this.senText.setVisibility(View.INVISIBLE);
            this.decreaseSenSlowBtn.setVisibility(View.INVISIBLE);
            this.decreaseSenSlowBtn.setEnabled(true);
            this.decreaseSenOpenBtn.setVisibility(View.INVISIBLE);
            this.decreaseSenOpenBtn.setEnabled(true);
        }
        else{
            this.calibrateMouthBtn.setEnabled(true);
            this.calibrateMouthBtn.setVisibility(View.VISIBLE);
            this.increaseSenSlowBtn.setEnabled(true);
            this.increaseSenSlowBtn.setVisibility(View.VISIBLE);
            this.increaseSenOpenBtn.setEnabled(true);
            this.increaseSenOpenBtn.setVisibility(View.VISIBLE);
            this.meshBtn.setEnabled(true);
            this.meshBtn.setVisibility(View.VISIBLE);
            this.textView.setVisibility(View.VISIBLE);
            this.yDifText.setVisibility(View.VISIBLE);
            this.touchInfo.setVisibility(View.VISIBLE);
            this.senText.setVisibility(View.VISIBLE);
            this.decreaseSenSlowBtn.setVisibility(View.VISIBLE);
            this.decreaseSenSlowBtn.setEnabled(false);
            this.decreaseSenOpenBtn.setVisibility(View.VISIBLE);
            this.decreaseSenOpenBtn.setEnabled(false);
        }
    }
}
