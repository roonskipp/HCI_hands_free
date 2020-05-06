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
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
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


import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer, SurfaceHolder.Callback {
  private static final String TAG = HelloArActivity.class.getSimpleName();

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private boolean meshBtnClicked;

  private CircleView circleView;

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
  private TextView faceOrientationText;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;

  private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
  private final ObjectRenderer virtualObject = new ObjectRenderer();


  // A list of CaptureRequest keys that can cause delays when switching between AR and non-AR modes.
  private List<CaptureRequest.Key<?>> keysThatCanCauseCaptureDelaysWhenModified;

  private <T> boolean checkIfKeyCanCauseDelay(CaptureRequest.Key<T> key) {
    if (Build.VERSION.SDK_INT >= 28) {
      // On Android P and later, return true if key is difficult to apply per-frame.
      return keysThatCanCauseCaptureDelaysWhenModified.contains(key);
    } else {
      // On earlier Android versions, log a warning since there is no API to determine whether
      // the key is difficult to apply per-frame. Certain keys such as CONTROL_AE_TARGET_FPS_RANGE
      // are known to cause a noticeable delay on certain devices.
      // If avoiding unexpected capture delays when switching between non-AR and AR modes is
      // important, verify the runtime behavior on each pre-Android P device on which the app will
      // be distributed. Note that this device-specific runtime behavior may change when the
      // device's operating system is updated.
      Log.w(
              TAG,
              "Changing "
                      + key
                      + " may cause a noticeable capture delay. Please verify actual runtime behavior on"
                      + " specific pre-Android P devices that this app will be distributed on.");
      // Allow the change since we're unable to determine whether it can cause unexpected delays.
      return false;
    }
  }

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] anchorMatrix = new float[16];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);

    displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

    getDeviceMetrics();

    circleView = findViewById(R.id.circleView);
    surfaceHolder = circleView.getHolder();
    surfaceHolder.addCallback(this);
    surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);




    // Set up renderer.
    surfaceView.setPreserveEGLContextOnPause(true);
    surfaceView.setEGLContextClientVersion(2);
    surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
    surfaceView.setRenderer(this);
    surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    surfaceView.setWillNotDraw(true);

    installRequested = false;
    meshBtnClicked = true;
    faceOrientationText = findViewById(R.id.vectorText);
  }


  protected void onMeshBtnClick(){

  }

  @Override
  protected void onResume() {
    super.onResume();

    waitUntilCameraCaptureSessionIsActive();

    startBackgroundThread();

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

      /*
      System.out.println(camera.toString());
      System.out.println(frame.toString());
      System.out.println(frame.getTimestamp());
      System.out.println(frame.getAndroidCameraTimestamp());
      System.out.println("TAG: "+ TAG); */

      // If frame is ready, render camera preview image to the GL surface.
      // UNCOMMENT THE LINE BELOW TO RENDER CAMERAFEED

      //backgroundRenderer.draw(frame);

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
          //virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, color4f);
          updateFaceOrientation(face);


        }
        else{
          displayErrorFace(face);
          return;
        }

      }

    } catch (Throwable t) {
      // Avoid crashing the application due to unhandled exceptions.
      Log.e(TAG, "Exception on the OpenGL thread", t);
    }
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

    // System.out.println("cursorHeight: " + cursorHeight);
    // System.out.println("cursorWidth: " + cursorLeft);

    circleView.updatePos(cursorLeft, cursorHeight, 50);

    // System.out.println("CenterPose X: " + x[0]);
    // System.out.println("CenterPose Y: " + y[0]);
    // System.out.println("CenterPose Z: " + z[0]);
    // faceOrientationText.setText("CenterPose Z: \n" + z[0]  + "\n" + z[1] + "\n" + z[2] );
  }

  public void displayErrorFace(AugmentedFace face){

    // faceOrientationText.setText("There is a bug. Reopen the app without closing it.");
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

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
      Log.d(TAG, "circleView createed");
      circleView.setWillNotDraw(false);
      Paint paint = new Paint();
      paint.setColor(Color.YELLOW);
      System.out.println("Drawing circle on:" + " " + 50 + " " + 50);
      Canvas canvas = holder.lockCanvas();
      holder.unlockCanvasAndPost(canvas);

  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    Paint paint = new Paint();
    paint.setColor(Color.YELLOW);
    System.out.println("Drawing circle on:" + " " + 50 + " " + 50);
    Canvas canvas = holder.lockCanvas();
    holder.unlockCanvasAndPost(canvas);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {

  }
}
