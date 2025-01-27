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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.MediaRouteButton;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

  // used to determine how many frames we use to average the movement
  private final static int AMOUNT_FACES_FOR_AVG = 5;

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
    private boolean calibratedOpen = false;

  private ArrayList<ArrayList<Float>> faces_list;

  private ArrayList mouthValues;
  private float closedMouthVal;
  private float openMouthVal;
  private boolean calibrateClicked = false;

  private Button calibrateMouthBtn;
  private Button calibrateFaceBtn;
  private Button continueCalibrationBtn;
  private Button clickMe;

  private float sensitivitySlow = (float) 0.4;
  private float sensitivityOpen = (float) 1.2;

  private Button increaseSenSlowBtn;
  private Button decreaseSenSlowBtn;
  private Button increaseSenOpenBtn;
  private Button decreaseSenOpenBtn;
  private Button meshBtn;

  private TextView senText;
  private TextView touchInfo;

  private TextView yDifText;

  // codes used to determine which part of facecalibartion we are in
  public final static int FaceTop = 5555550;
  public final static int FaceTopRight = 5555551;
  public final static int FaceRight = 5555552;
  public final static int FaceBottomRight = 5555553;
  public final static int FaceBottom = 5555554;
  public final static int FaceBottomLeft = 5555555;
  public final static int FaceLeft = 5555556;
  public final static int FaceTopLeft = 5555557;
  public final static int FaceCompleted = 5555558;

  // fields to hold the calibrated facevalues
  private float FaceTopCalibrated;
  private float FaceTopRightCalibrated;
  private float FaceRightCalibrated;
  private float FaceBottomRightCalibrated;
  private float FaceBottomCalibrated;
  private float FaceBottomLeftCalibrated;
  private float FaceLeftCalibrated = 0;
  private float FaceTopLeftCalibrated;
  private float midpoint = 0;
  private float distTop = (float) 0.3;
  private float distBot = (float) 0.3;

  // field to determine how many values to use to calculate the facecalibration
  public final static int faceCalibrationSize = 100;
    public final static int mouthCalibrationSize = 100;

  private static final int REQUEST_EXTERNAL_STORAGE = 1;
  private static String[] PERMISSIONS_STORAGE = {
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
  };



  // boolean fields to progress through face calibration
  private boolean faceCalTextUpdated = false;
  private boolean CalibratingFace = false;
  private boolean ContinueCalibration = false;
  private ArrayList<ArrayList> faceValues;
  private int currentFaceCalibration = 0;

  private boolean calibratedMouth = false;

  public static final String SHARED_PREFS = "sharedPrefs";
  public static final String FACE_TOP = "face_top";
  public static final String FACE_RIGHT ="face_right";
  public static final String FACE_BOTTOM = "face_bottom";
  public static final String FACE_LEFT = "face_left";
  public static final String OPEN_MOUTH = "open_mouth";
  public static final String CLOSED_MOUTH = "closed_mouth";
  public static final String SENSITIVITY_SLOW = "sensitivity_slow";
  public static final String SENSITIVITY_OPEN = "sensitivity_open";
    private boolean calibratingMouth;
    private boolean startedCalibratingClosed;
    private boolean startedCalibratingOpen;
    private boolean mouthCalibrationContinueButtonVisible;
    private boolean ContinueMouthCalibration;
    private Button continueMouthCalibrationBtn;
    private int currentMouthCalibration;

    public final static int MOUTH_CLOSED = 666660;
    public final static int MOUTH_CLOSED_DONE = 666663;
    public final static int MOUTH_OPEN = 666661;
    public final static int MOUTH_OPEN_DONE = 666664;
    public final static int MOUTH_NOT_STARTED = 666662;

    public final static String MIDPOINT = "midpoint";
    public final static String DIST_TOP = "dist_top";
    public final static String DIST_BOT = "dist_bot";
    private boolean mouthCalibrationSetText;
    private boolean touchFlag;

    private TextView lipGapText;
    private TextView calibratedMouthText;

    // fitts law
    private Button fittsBtn;

    // used to check if file exists
    private String fileName = "fittsResult.txt";

    // to receive broadcasts from other threads
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received broadcastmessage");
        ArrayList<ArrayList> missClicks = (ArrayList<ArrayList>) intent.getSerializableExtra("fittsMisses");
        for(int i = 0; i< missClicks.size(); i++){
          Log.i(TAG, "missclick:" + i + "x: " + missClicks.get(i).get(0).toString() + " y:" + missClicks.get(i).get(1).toString());
        }

        ArrayList<ArrayList> hitClicks = (ArrayList<ArrayList>) intent.getSerializableExtra("fittsHits");
        // in here the [0] = x coordinate, [1] = y coordinate, [2] = time to hit target, [3] = code of stage, small, medium or large target size, [4] = prevPos x, [5] = prevpos y (use to calc distance), [6] = prevStage
        for(int i = 0; i< hitClicks.size(); i++){
          Log.i(TAG, "hit target:" + i + "x: " + hitClicks.get(i).get(0).toString() + " y:" + hitClicks.get(i).get(1).toString() + " timeToHit: "+ hitClicks.get(i).get(2).toString() + " stage: " + hitClicks.get(i).get(3).toString() + " prev x: " + hitClicks.get(i).get(4) + " prev y: " + hitClicks.get(i).get(5) + " prevStage: " + hitClicks.get(i).get(6));
        }

        // create jsons for misses and hits to be saved to internal storage
        JSONObject json = new JSONObject();
        JSONObject missJson = new JSONObject();
        JSONObject hitJson = new JSONObject();
        for(int i = 0; i< missClicks.size(); i++){
          JSONArray jsonArray = new JSONArray(missClicks.get(i));
          try {
            missJson.put("miss" + String.valueOf(i), jsonArray);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
        for(int i = 0; i< hitClicks.size(); i++){
          JSONArray jsonArray = new JSONArray(hitClicks.get(i));
          try {
            hitJson.put("hit" + i, jsonArray);
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        try {
          json.put("misses", missJson);
          json.put("hits", hitJson);
          json.put("faceTop", FaceTopCalibrated);
          json.put("faceRight", FaceRightCalibrated);
          json.put("faceBot", FaceBottomCalibrated);
          json.put("faceLeft", FaceLeftCalibrated);
          json.put("openMouth", openMouthVal);
          json.put("closedMouth", closedMouthVal);



        } catch (JSONException e) {
          e.printStackTrace();
        }

        saveJson(json);
        fittsBtn.setVisibility(View.VISIBLE);
        fittsBtn.setEnabled(true);

      }

    };
  private boolean storageRequested;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);

    calibrateMouthBtn = findViewById(R.id.calibrateMouth);
    calibrateFaceBtn = findViewById(R.id.calibrateFace);

    continueMouthCalibrationBtn = findViewById(R.id.continueCalibrateMouth);
    continueMouthCalibrationBtn.setVisibility(View.INVISIBLE);

    continueCalibrationBtn = findViewById(R.id.continueCalibrateFace);

      continueCalibrationBtn.setVisibility(View.INVISIBLE);
      continueCalibrationBtn.setEnabled(false);

    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    getDeviceMetrics();

    debugSwitch = findViewById(R.id.simpleSwitch);

    lipGapText = findViewById(R.id.lipGapText);
    calibratedMouthText = findViewById(R.id.calibratedMouthText);


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
    clickMe.setEnabled(false);
    clickMe.setVisibility(View.INVISIBLE);

    increaseSenSlowBtn = findViewById(R.id.increaseSenSlowBtn);
    decreaseSenSlowBtn = findViewById(R.id.decreaseSlowSenBtn);

    fittsBtn = findViewById(R.id.fittsTest);

      loadCalibration();

  }


  protected void onMeshBtnClick(){

  }

  @Override
  protected void onResume() {
    super.onResume();
    this.updateSenText();
    this.updateTouchInfo();

    this.registerReceiver(broadcastReceiver, new IntentFilter("com.android.activity.SEND_DATA"));





      waitUntilCameraCaptureSessionIsActive();

    //startBackgroundThread();

    if(!storageRequested){
      requestStoragePermission();

    }

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

  private void requestStoragePermission() {
      storageRequested = true;
    ActivityCompat.requestPermissions(this,
            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
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
    this.unregisterReceiver(broadcastReceiver);
    //LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {

      switch (requestCode){
        case 0:
          if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
              // Permission denied with checking "Do not ask again".
              CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
          }
        case REQUEST_EXTERNAL_STORAGE:
          Log.i(TAG, "permission:" + (results[0] == PackageManager.PERMISSION_GRANTED));
          break;
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
    /*
    float neededGap = (float) ((this.openMouthVal*sensitivityOpen-this.closedMouthVal));  // Gap needed to click
    float slowGap = (float)((this.openMouthVal-this.closedMouthVal*this.sensitivitySlow)); */
    float neededGap = (float) openMouthVal*sensitivityOpen;
    float slowGap = (float) closedMouthVal *sensitivitySlow;
    float totalGap = (float) neededGap - slowGap; // gap between slow starts and click appears

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        lipGapText.setText("LipGap: " + currentGap);
      }
    });


    if(currentGap > neededGap){
        if(!touchFlag){
            touchFlag = true;
            Log.i(TAG, "MOUTH OPEN:");
            surfaceThread.touchScreen();
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    updateYDifText(0);

                }
            });
            return 1;
        }
        else{
            Log.i(TAG, "MOUTH STILL:");
            return 1;
        }

    }
    else if( currentGap > slowGap){ // mouth is somewhat open, we want to slow down the cursor
        if(touchFlag){
            touchFlag = false;;
        }
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
        if(touchFlag){
            touchFlag = false;
        }

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

    if(faces_list.size() == AMOUNT_FACES_FOR_AVG){
        float slow = 0;

        ArrayList curr_vals = new ArrayList<Float>();
        curr_vals.add(z[1]);
        curr_vals.add(z[0]);
        faces_list.remove(0);
        faces_list.add(curr_vals);

        if(!calibratingMouth){
            if(calibratedMouth){
                slow = checkMouthOpen(face);
            }
        }





        float average_z_1 = 0;
        float average_z_0 = 0;
        // get average of last 10 faces
        for(int i=0; i<faces_list.size(); i++){
            average_z_1 = (Float) average_z_1 + faces_list.get(i).get(0);
            average_z_0 = (Float) average_z_0 + faces_list.get(i).get(1);
        }
        average_z_1 = (average_z_1/AMOUNT_FACES_FOR_AVG);
        average_z_0 = (average_z_0/AMOUNT_FACES_FOR_AVG);

        if(CalibratingFace){
            calibrateFace(average_z_0, average_z_1);
        }

        if(this.FaceTopCalibrated != 0){
            int cursorHeight = deviceHeight/2;
            // System.out.println("Device: " + this.deviceWidth + " " + this.deviceHeight);
            System.out.println("z: " + average_z_1);
            System.out.println("go to: " + Math.round(deviceHeight/2 + ((average_z_1/this.FaceBottomCalibrated)*deviceHeight/2)));

            // for better performance, move this logic to callibration, so it is not caluclated on each frame

            Log.d(TAG, "midpoint:" + midpoint);

            Log.d(TAG, "distTop: " + distTop);
            Log.d(TAG, "distBot: " + distBot);
            float currDist = (float) Math.sqrt(Math.pow((double) midpoint - (double) average_z_1, 2));
            Log.d(TAG, "currDist: " + currDist);
            if(z[1] < midpoint){
                cursorHeight = (int)Math.round(deviceHeight/2 + ((currDist/distBot)*deviceHeight/2));

            }
            else{
                cursorHeight =  (int)Math.round(deviceHeight/2 - ((currDist/distTop)*deviceHeight/2));
            }
            int cursorLeft = 0;
            if(z[0] <=0){
                cursorLeft = (int)Math.round(deviceWidth/2 + (average_z_0/this.FaceRightCalibrated)*deviceWidth/2);
            }
            else{
                cursorLeft = (int)Math.round(deviceWidth/2 - (average_z_0/this.FaceLeftCalibrated)*deviceWidth/2);
            }

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

      switch(currentMouthCalibration){
          case MOUTH_NOT_STARTED:
              if(!mouthCalibrationSetText) {
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          textView.setText("Close your mouth, press continue and keep it closed until finished");
                          mouthCalibrationSetText = true;
                      }
                  });
              }
              break;

          case MOUTH_CLOSED:
              FloatBuffer buffer = face.getMeshVertices();
              Vector3 upperLipVec = new Vector3(buffer.get(39), buffer.get(40), buffer.get(41));
              Vector3 lowerLipVec = new Vector3(buffer.get(42), buffer.get(43), buffer.get(44));
              float yDif = upperLipVec.y - lowerLipVec.y;
              Log.i(TAG, "yDIF: " + yDif);

              mouthValues.add(yDif);
              float percent = (float)100* ((float)mouthValues.size()/(float)mouthCalibrationSize);
              Log.d(TAG, "percent:" + String.valueOf(percent));
              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      textView.setText("Close Your mouth. Completed " + percent +"%");
                  }
              });

              if(mouthValues.size() >= mouthCalibrationSize){
                  // stop calibrating
                  closedMouthVal = 0;
                  // calculate average closed value
                  for(int i = 0; i<mouthValues.size(); i++){
                      closedMouthVal += (float) mouthValues.get(i);
                  }
                  this.closedMouthVal = closedMouthVal/mouthCalibrationSize;
                  mouthValues.clear();

                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          textView.setText("Open your mouth widely, press continue and keep it open until finished");
                      }
                  });
                  currentMouthCalibration = MOUTH_CLOSED_DONE;
              }
              break;
          case MOUTH_CLOSED_DONE:
              if(!mouthCalibrationContinueButtonVisible){
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          continueMouthCalibrationBtn.setEnabled(true);
                          continueMouthCalibrationBtn.setVisibility(View.VISIBLE);
                      }
                  });
                  mouthCalibrationContinueButtonVisible = true;
              }

              break;

          case MOUTH_OPEN:
              FloatBuffer Open_buffer = face.getMeshVertices();
              Vector3 Open_upperLipVec = new Vector3(Open_buffer.get(39), Open_buffer.get(40), Open_buffer.get(41));
              Vector3 Open_lowerLipVec = new Vector3(Open_buffer.get(42), Open_buffer.get(43), Open_buffer.get(44));
              float OpenyDif = Open_upperLipVec.y - Open_lowerLipVec.y;

              mouthValues.add(OpenyDif);
              float Open_percent = (float)100*(((float)mouthValues.size())/(float)mouthCalibrationSize);
              Log.d(TAG, "percent:" + String.valueOf(Open_percent));

              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      textView.setText("Open Your mouth. Completed " + Open_percent +"%");
                  }
              });

              if(mouthValues.size() >= mouthCalibrationSize){
                  runOnUiThread(new Runnable() {
                      @Override
                      public void run() {
                          textView.setText("Mouth calibration done.");
                          calibrateMouthBtn.setEnabled(true);
                          calibrateMouthBtn.setVisibility(View.VISIBLE);
                          currentMouthCalibration = MOUTH_NOT_STARTED;
                      }
                  });
                  // stop calibrating
                  calibratedOpen = false;
                  openMouthVal = 0;
                  // calculate average closed value
                  for(int i = 0; i<mouthValues.size(); i++){
                      openMouthVal += (float) mouthValues.get(i);
                  }
                  this.openMouthVal = openMouthVal/mouthCalibrationSize;
                  this.calibrateClicked = false;
                  this.calibratedMouth = true;
                  calibratingMouth = false;
                  runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      calibratedMouthText.setText("mouth open gap: " + openMouthVal + "\nmouth closed gap: " + closedMouthVal);
                    }
                  });
                  saveCalibration();
              }

              break;
          default:
              break;
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

  // takes in x and y values of the z vector through the face
  public void calibrateFace(float x, float y){

      if(faceValues == null){
          faceValues = new ArrayList<ArrayList>();
      }

      // get the z vector


      switch (this.currentFaceCalibration){
          case 0:
              // Code
              this.currentFaceCalibration = this.FaceTop;
              faceValues = new ArrayList<ArrayList>();
              break;
          case FaceTop:
              if(faceValues.size() == 0) {
                  if (!faceCalTextUpdated) {
                      textView.setText("Face upwards to comfortable position and press\n continue to start calibrating");
                      faceCalTextUpdated = true;
                  }
              }
              if(this.ContinueCalibration){
                  if(faceValues.size() < faceCalibrationSize){
                      ArrayList<Float> vals = new ArrayList<>();
                      vals.add(x);
                      vals.add(y);
                      faceValues.add(vals);
                  }
                  else if(faceValues.size() >= faceCalibrationSize){
                      // pause calibration
                      ContinueCalibration = false;
                      // calculate average of the calibrationvalues
                      float averagedCalibrationValue = 0;
                      for(int i = 0; i<faceCalibrationSize; i++){
                            averagedCalibrationValue += (float) faceValues.get(i).get(1);
                      }
                      FaceTopCalibrated = averagedCalibrationValue/faceCalibrationSize;
                      this.currentFaceCalibration = FaceRight;
                      this.ContinueCalibration = false;
                      runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              continueCalibrationBtn.setVisibility(View.VISIBLE);
                              continueCalibrationBtn.setEnabled(true);

                          }
                      });

                      faceValues.clear();
                      faceCalTextUpdated = false;
                  }
              }
              break;

          /*case FaceTopRight:
              if(faceValues.size() == 0) {
                  if (!faceCalTextUpdated) {
                      textView.setText("Face upwards and press\n to start calibrating");
                      faceCalTextUpdated = true;
                  }
              }
              if(this.ContinueCalibration){
                  if(faceValues.size() < faceCalibrationSize){
                      ArrayList<Float> vals = new ArrayList<>();
                      vals.add(x);
                      vals.add(y);
                      faceValues.add(vals);
                  }
                  else if(faceValues.size() >= faceCalibrationSize){
                      // pause calibration
                      ContinueCalibration = false;
                      // calculate average of the calibrationvalues
                      float averagedCalibrationValue = 0;
                      for(int i = 0; i<faceCalibrationSize; i++){
                          averagedCalibrationValue += (float) faceValues.get(i).get(0);
                      }
                      FaceTopCalibrated = averagedCalibrationValue/faceCalibrationSize;
                      this.currentFaceCalibration = FaceTopRight;
                      this.ContinueCalibration = false;
                      faceValues.clear();
                      faceCalTextUpdated = false;
              break; */

          case FaceRight:
              if(faceValues.size() == 0) {
                  if (!faceCalTextUpdated) {
                      textView.setText("Face to the right to comfortable position and press\n continue to start calibrating");
                      faceCalTextUpdated = true;
                  }
              }
              if(this.ContinueCalibration) {
                  if (faceValues.size() < faceCalibrationSize) {
                      ArrayList<Float> vals = new ArrayList<>();
                      vals.add(x);
                      vals.add(y);
                      faceValues.add(vals);
                  } else if (faceValues.size() >= faceCalibrationSize) {
                      // pause calibration
                      ContinueCalibration = false;
                      // calculate average of the calibrationvalues
                      float averagedCalibrationValue = 0;
                      for (int i = 0; i < faceCalibrationSize; i++) {
                          averagedCalibrationValue += (float) faceValues.get(i).get(0);
                      }
                      FaceRightCalibrated = averagedCalibrationValue / faceCalibrationSize;
                      this.currentFaceCalibration = FaceBottom;
                      this.ContinueCalibration = false;
                      runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              continueCalibrationBtn.setVisibility(View.VISIBLE);
                              continueCalibrationBtn.setEnabled(true);

                          }
                      });

                      faceValues.clear();
                      faceCalTextUpdated = false;
                  }
              }
              break;

          /*case FaceBottomRight:
              break; */

          case FaceBottom:
              if(faceValues.size() == 0) {
                  if (!faceCalTextUpdated) {
                      textView.setText("Face downwards to comfortable position and press\n continue to start calibrating");
                      faceCalTextUpdated = true;
                  }
              }
              if(this.ContinueCalibration) {
                  if (faceValues.size() < faceCalibrationSize) {
                      ArrayList<Float> vals = new ArrayList<>();
                      vals.add(x);
                      vals.add(y);
                      faceValues.add(vals);
                  } else if (faceValues.size() >= faceCalibrationSize) {
                      // pause calibration
                      ContinueCalibration = false;
                      // calculate average of the calibrationvalues
                      float averagedCalibrationValue = 0;
                      for (int i = 0; i < faceCalibrationSize; i++) {
                          averagedCalibrationValue += (float) faceValues.get(i).get(1);
                      }
                      FaceBottomCalibrated = averagedCalibrationValue / faceCalibrationSize;
                      this.currentFaceCalibration = FaceLeft;
                      this.ContinueCalibration = false;
                      runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              continueCalibrationBtn.setVisibility(View.VISIBLE);
                              continueCalibrationBtn.setEnabled(true);

                          }
                      });

                      faceValues.clear();
                      faceCalTextUpdated = false;
                  }
              }
              break;

          /*case FaceBottomLeft:
              break;*/

          case FaceLeft:
              if(faceValues.size() == 0) {
                  if (!faceCalTextUpdated) {
                      textView.setText("Face left to comfortable position and press\n continue to start calibrating");
                      faceCalTextUpdated = true;
                  }
              }
              if(this.ContinueCalibration) {
                  if (faceValues.size() < faceCalibrationSize) {
                      ArrayList<Float> vals = new ArrayList<>();
                      vals.add(x);
                      vals.add(y);
                      faceValues.add(vals);
                  } else if (faceValues.size() >= faceCalibrationSize) {
                      // pause calibration
                      ContinueCalibration = false;
                      // calculate average of the calibrationvalues
                      float averagedCalibrationValue = 0;
                      for (int i = 0; i < faceCalibrationSize; i++) {
                          averagedCalibrationValue += (float) faceValues.get(i).get(0);
                      }
                      FaceLeftCalibrated = averagedCalibrationValue / faceCalibrationSize;
                      this.currentFaceCalibration = 0;
                      this.CalibratingFace = false;
                      this.ContinueCalibration = false;
                      midpoint = (FaceTopCalibrated+(FaceBottomCalibrated))/(float)2;
                      distTop = FaceTopCalibrated - midpoint;
                      distBot = midpoint - FaceBottomCalibrated;

                      textView.setText("Calibration completed\n" + this.FaceTopCalibrated + " " + this.FaceRightCalibrated + "\n" + this.FaceBottomCalibrated + " " + this.FaceLeftCalibrated);
                      runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              continueCalibrationBtn.setVisibility(View.INVISIBLE);
                              continueCalibrationBtn.setEnabled(false);
                              calibrateFaceBtn.setVisibility(View.VISIBLE);
                              calibrateFaceBtn.setEnabled(true);

                          }
                      });

                      faceValues.clear();
                      faceCalTextUpdated = false;
                      saveCalibration();
                  }
              }
              break;

          /*case FaceTopLeft:
              break;*/

          default:
              break;

      }
      // if nothing is calibrated yet display look up

      // look right corner

      // after calibrating look up change display text to look right

      // look downright corner

      // --- look down

      // look downleft corner

      // --- look left

      // look up left corner
      //
  }





    public void onClickIncreaseOpenSen(View view) {
      this.sensitivityOpen -= 0.05;
      this.updateSenText();
      saveSlowOpenSensitvity();

    }

    public void onClickDecreaseOpenSen(View view) {
        this.sensitivityOpen += 0.05;
        this.updateSenText();
        saveSlowOpenSensitvity();

    }

    public void onClickDecreaseSlowSen(View view) {
        this.sensitivitySlow += 0.05;
        this.updateSenText();
        saveSlowOpenSensitvity();

    }

    public void onClickIncreaseSlowSen(View view) {
        this.sensitivitySlow -= 0.05;
        this.updateSenText();
        saveSlowOpenSensitvity();

    }

    public void updateSenText(){
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          senText.setText("sens. open:" + sensitivityOpen + "\t sens. slow: " +sensitivitySlow);

        }
      });

    }

    public void onDebugSwitch(View view) {
        Boolean switchState = this.debugSwitch.isChecked();
        if(!switchState){
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
            this.decreaseSenSlowBtn.setEnabled(false);
            this.decreaseSenOpenBtn.setVisibility(View.INVISIBLE);
            this.decreaseSenOpenBtn.setEnabled(false);
            //this.fittsBtn.setEnabled(false);
            //this.fittsBtn.setVisibility(View.INVISIBLE);
            this.calibrateFaceBtn.setVisibility(View.INVISIBLE);
            this.calibrateFaceBtn.setEnabled(false);
            //this.fittsBtn.setVisibility(View.INVISIBLE);
            //this.fittsBtn.setEnabled(false);
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
            this.decreaseSenSlowBtn.setEnabled(true);
            this.decreaseSenOpenBtn.setVisibility(View.VISIBLE);
            this.decreaseSenOpenBtn.setEnabled(true);

          // this introduces bug, because if the calibration is going on, then you will be able to start a calibration even when one is going on...
            this.calibrateFaceBtn.setEnabled(true);
            this.calibrateFaceBtn.setVisibility(View.VISIBLE);
            // this introduces a bug because if fitts test is already going on, then you can restart it.... might be handled in surfacethread?
            // this.fittsBtn.setEnabled(true);
            // this.fittsBtn.setVisibility(View.VISIBLE);
        }
    }

    public void onClickCalibrateFace(View view) {

      // flip a boolean that makes a call to drawframe flow through a cralibrateFace logic run
        this.CalibratingFace = true;
        this.calibrateFaceBtn.setEnabled(false);
        this.calibrateFaceBtn.setVisibility(View.INVISIBLE);
        this.continueCalibrationBtn.setVisibility(View.VISIBLE);
        this.continueCalibrationBtn.setEnabled(true);

    }

    public void onClickContinueCalibrateFace(View view) {
      this.ContinueCalibration = true;
        this.continueCalibrationBtn.setVisibility(View.INVISIBLE);
        this.continueCalibrationBtn.setEnabled(false);
    }

    public void saveCalibration(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(sensitivitySlow == 0){
            sensitivitySlow = (float) 0.55;
        }
        if(sensitivityOpen == 0){
            sensitivityOpen = (float) 0.90;
        }
        editor.putFloat(FACE_TOP, FaceTopCalibrated);
        editor.putFloat(FACE_RIGHT, FaceRightCalibrated);
        editor.putFloat(FACE_BOTTOM, FaceBottomCalibrated);
        editor.putFloat(FACE_LEFT, FaceLeftCalibrated);
        editor.putFloat(OPEN_MOUTH, openMouthVal);
        editor.putFloat(CLOSED_MOUTH, closedMouthVal);
        editor.putFloat(SENSITIVITY_OPEN, sensitivityOpen);
        editor.putFloat(SENSITIVITY_SLOW, sensitivitySlow);
        editor.putFloat(MIDPOINT, midpoint);
        editor.putFloat(DIST_TOP, distTop);
        editor.putFloat(DIST_BOT, distBot);
        editor.apply();
        Toast.makeText(this, "Calibration saved", Toast.LENGTH_SHORT);
    }

    public void saveSlowOpenSensitvity(){
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putFloat(SENSITIVITY_OPEN, sensitivityOpen);
        editor.putFloat(SENSITIVITY_SLOW, sensitivitySlow);
        editor.apply();
        Toast.makeText(this, "Sensitivty saved", Toast.LENGTH_SHORT);
    }



    public void loadCalibration(){
      SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
      FaceTopCalibrated = sharedPreferences.getFloat(FACE_TOP, 0);
      FaceRightCalibrated = sharedPreferences.getFloat(FACE_RIGHT, 0);
      FaceBottomCalibrated = sharedPreferences.getFloat(FACE_BOTTOM, 0);
      FaceLeftCalibrated = sharedPreferences.getFloat(FACE_LEFT, 0);
      openMouthVal = sharedPreferences.getFloat(OPEN_MOUTH, 0);
      closedMouthVal = sharedPreferences.getFloat(CLOSED_MOUTH, 0);
      sensitivityOpen = sharedPreferences.getFloat(SENSITIVITY_OPEN, 0);
      sensitivitySlow = sharedPreferences.getFloat(SENSITIVITY_SLOW, 0);
      midpoint = sharedPreferences.getFloat(MIDPOINT, 0);
      distTop = sharedPreferences.getFloat(DIST_TOP, 0);
      distBot = sharedPreferences.getFloat(DIST_BOT, 0);

      if(closedMouthVal == 0 || openMouthVal == 0){
          if(FaceTopCalibrated == 0){
              runOnUiThread(new Runnable() {

                  @Override
                  public void run() {

                      textView.setText("Calibrate mouth and face");

                  }
              });
          }
          else{
              runOnUiThread(new Runnable() {

                  @Override
                  public void run() {

                      textView.setText("Calibrate mouth");

                  }
              });
          }

      }
      else if(FaceTopCalibrated == 0){
          calibratedMouth = true;
          runOnUiThread(new Runnable() {

              @Override
              public void run() {

                  textView.setText("Calibrate face");

              }
          });
      }
      else{
          calibratedMouth = true;
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              calibratedMouthText.setText("mouth open gap: " + openMouthVal + "\nmouth closed gap: " + closedMouthVal);
            }
          });
      }
    }

    public void onClickContinueCalibrateMouth(View view) {
        if(currentMouthCalibration == MOUTH_NOT_STARTED){
            currentMouthCalibration = MOUTH_CLOSED;
        }
        else if(currentMouthCalibration == MOUTH_CLOSED_DONE){
            currentMouthCalibration = MOUTH_OPEN;
        }
        this.continueMouthCalibrationBtn.setVisibility(View.INVISIBLE);
        this.continueMouthCalibrationBtn.setEnabled(false);
        mouthCalibrationContinueButtonVisible = false;

    }

    public void onClickCalibrateMouth(View view) {
        this.calibratingMouth = true;
        currentMouthCalibration = MOUTH_NOT_STARTED;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                continueMouthCalibrationBtn.setVisibility(View.VISIBLE);
                continueMouthCalibrationBtn.setEnabled(true);
            }
        });
        calibrateMouthBtn.setEnabled(false);
        this.openMouthVal = 0;
        this.closedMouthVal = 0;
        if(this.mouthValues != null){
            this.mouthValues = new ArrayList();
        }
        this.calibrateClicked = true;
    }

  public void onClickFitts(View view) {

      // hide this button
    fittsBtn.setEnabled(false);
    fittsBtn.setVisibility(View.INVISIBLE);
    surfaceThread.startFitts();
    // use a broadcast receiver to handle the result of fitts?

  }
  public boolean fileExist(String fileName){
    File file = getBaseContext().getFileStreamPath(fileName);
    return file.exists();
  }

  private boolean isExternalStorageWritable(){
    if(Environment.MEDIA_MOUNTED.equals((Environment.getExternalStorageState()))){
      Log.i(TAG, "Media is mounted");
      return true;
    }
    else{
      Log.i(TAG, "Media is unmounted");
      return false;
    }
  }

  public boolean checkPermission(String permission){
    int check = ContextCompat.checkSelfPermission(this, permission);
    return (check == PackageManager.PERMISSION_GRANTED);
  }

  public void saveJson(JSONObject json){
    if(isExternalStorageWritable() && checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
      int counter = 1;
      File textFile = new File(Environment.getExternalStorageDirectory(), "fittsResult.txt");
      while(textFile.exists()){
        textFile = new File(Environment.getExternalStorageDirectory(), "fittsResult" + counter +".txt");
        counter += 1;
      }

      try{
        FileOutputStream fos = new FileOutputStream(textFile);
        fos.write(json.toString().getBytes());
        fos.close();
        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(textFile));
        sendBroadcast(intent);
      } catch (IOException e){
        Log.e(TAG, "Error with file output stream");
        e.printStackTrace();
      }
    }



  }
}
