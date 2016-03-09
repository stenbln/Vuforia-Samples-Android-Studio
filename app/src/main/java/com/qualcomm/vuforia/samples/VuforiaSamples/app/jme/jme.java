package com.qualcomm.vuforia.samples.VuforiaSamples.app.jme;

/**
 * Created by Gotti on 1/31/16.
 */
/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/


        import android.app.AlertDialog;
        import android.content.DialogInterface;
        import android.content.pm.ActivityInfo;
        import android.content.res.Configuration;
        import android.graphics.Color;
        import android.os.Bundle;
        import android.os.Handler;
        import android.util.Log;
        import android.view.GestureDetector;
        import android.view.LayoutInflater;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.ViewGroup.LayoutParams;
        import android.widget.RelativeLayout;


        //import android.R;
        import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationControl;
        import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationSession;
        import com.qualcomm.vuforia.samples.VuforiaSamples.R;


        import com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils.Constants;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils.LoadingDialogHandler;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.vuforia.VuforiaApplicationControl;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.vuforia.VuforiaApplicationException;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.vuforia.VuforiaApplicationSession;
        import com.jme3.app.AndroidHarness;
        import com.jme3.input.event.TouchEvent;
        import com.jme3.system.android.AndroidConfigChooser.ConfigType;
        import com.jme3.texture.Image;
        import com.qualcomm.vuforia.CameraDevice;
        import com.qualcomm.vuforia.Frame;
        import com.qualcomm.vuforia.Marker;
        import com.qualcomm.vuforia.MarkerTracker;
        import com.qualcomm.vuforia.PIXEL_FORMAT;
        import com.qualcomm.vuforia.State;
        import com.qualcomm.vuforia.Tracker;
        import com.qualcomm.vuforia.TrackerManager;
        import com.qualcomm.vuforia.Vec2F;
        import com.qualcomm.vuforia.Vuforia;


        import java.lang.reflect.Array;
        import java.nio.ByteBuffer;
        import java.util.Arrays;


// The main activity for the MainActivity sample.
public class jme extends AndroidHarness implements VuforiaApplicationControl {
    private static final String LOGTAG = "MainActivity";

    VuforiaApplicationSession vuforiaAppSession;
    private RelativeLayout mUILayout;
    private RelativeLayout mUILayoutCamera;
    private View mCameraButton;
    private Marker dataSet[];
    private GestureDetector mGestureDetector;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(
            this);

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    boolean mIsDroidDevice = false;

    Image cameraJMEImageRGB565;
    private byte[] mPreviewBufferRGB656;
    java.nio.ByteBuffer mPreviewByteBufferRGB565;
    static boolean firstTimeGetImage = true;

    public jme() {
        // Set the application class to run
        appClass = "com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.JMESurfaceView";
        // Try ConfigType.FASTEST; or ConfigType.LEGACY if you have problems
        eglConfigType = ConfigType.BEST;
        // Exit Dialog title & message
        exitDialogTitle = "Exit?";
        exitDialogMessage = "Press Yes";
        // Enable verbose logging
        eglConfigVerboseLogging = false;
        // Choose screen orientation
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        // Invert the MouseEvents X (default = true)
        mouseEventsInvertX = true;
        // Invert the MouseEvents Y (default = true)
        mouseEventsInvertY = true;
    }

    public void setRGB565CameraImage(byte[] buffer, int width, int height) {

        if (firstTimeGetImage) {
            initializeImageBuffer(width, height);
            firstTimeGetImage = false;
        }


        mPreviewByteBufferRGB565.clear();
        mPreviewByteBufferRGB565.put(buffer);

        cameraJMEImageRGB565.setData(mPreviewByteBufferRGB565);
        Log.d(LOGTAG, "Ovo " + Integer.toString(cameraJMEImageRGB565.getHeight()) + " Width ===== " + Integer.toString(cameraJMEImageRGB565.getWidth()));


        if ((com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.JMESurfaceView) app != null) {
            ((com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.JMESurfaceView) app)
                    .setVideoBGTexture(cameraJMEImageRGB565);
        }

    }


    public void initializeImageBuffer(int width, int height) {
        int bufferSizeRGB565 = width * height * 2 + 4096;

        mPreviewBufferRGB656 = null;

        mPreviewBufferRGB656 = new byte[bufferSizeRGB565];

        mPreviewByteBufferRGB565 = ByteBuffer.allocateDirect(mPreviewBufferRGB656.length);
        cameraJMEImageRGB565 = new Image(Image.Format.RGB565, width,
                height, mPreviewByteBufferRGB565);

    }

    // Called when the activity first starts or the user navigates back to an activity.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);


        startLoadingAnimation();

        vuforiaAppSession = new VuforiaApplicationSession(this);
        vuforiaAppSession
                .initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mGestureDetector = new GestureDetector(this, new GestureListener());
    }

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();


        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(LOGTAG, "FOCUS!!!!");
            // Generates a Handler to trigger autofocus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable() {
                public void run() {
                    boolean result = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);

                    if (!result)
                        Log.e("SingleTapUp", "Unable to trigger focus");
                }
            }, 1000L);

            return true;
        }
    }


    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        try {
            vuforiaAppSession.resumeAR();
        } catch (VuforiaApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }


    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }


    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        try {
            vuforiaAppSession.pauseAR();
        } catch (VuforiaApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
    }


    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (VuforiaApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
        System.gc();
    }

    @Override
    public void onTouch(String name, TouchEvent evt, float tpf) {
        super.onTouch(name, evt, tpf);
        Log.d(LOGTAG, "TOUCH!!!");
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(LOGTAG, event.toString());
        return mGestureDetector.onTouchEvent(event);
    }


    private void startLoadingAnimation() {
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,
                null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);

        // Shows the loading indicator at start
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }


    // Initializes AR application components.
    private void initApplicationAR() {
        // Create OpenGL ES view:
        boolean translucent = Vuforia.requiresAlpha();
    }


    @Override
    public boolean doInitTrackers() {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // Initialize the marker tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker trackerBase = trackerManager.initTracker(MarkerTracker
                .getClassType());
        MarkerTracker markerTracker = (MarkerTracker) (trackerBase);

        if (markerTracker == null) {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        return result;

    }


    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager
                .getTracker(MarkerTracker.getClassType());
        if (markerTracker == null)
            return false;

        dataSet = new Marker[4];

        dataSet[0] = markerTracker.createFrameMarker(0, Constants.MARKER_0, new Vec2F(
                50, 50));
        if (dataSet[0] == null) {
            Log.e(LOGTAG, "Failed to create frame marker Q.");
            return false;
        }

        dataSet[1] = markerTracker.createFrameMarker(1, "MarkerC", new Vec2F(
                50, 50));
        if (dataSet[1] == null) {
            Log.e(LOGTAG, "Failed to create frame marker C.");
            return false;
        }

        dataSet[2] = markerTracker.createFrameMarker(2, "MarkerA", new Vec2F(
                50, 50));
        if (dataSet[2] == null) {
            Log.e(LOGTAG, "Failed to create frame marker A.");
            return false;
        }

        dataSet[3] = markerTracker.createFrameMarker(3, "MarkerR", new Vec2F(
                50, 50));
        if (dataSet[3] == null) {
            Log.e(LOGTAG, "Failed to create frame marker R.");
            return false;
        }

        Log.i(LOGTAG, "Successfully initialized MarkerTracker.");

        return true;
    }


    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager
                .getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.start();

        return result;
    }


    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager
                .getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.stop();

        return result;
    }


    @Override
    public boolean doUnloadTrackersData() {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        return result;
    }


    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(MarkerTracker.getClassType());

        return result;
    }


    @Override
    public void onInitARDone(VuforiaApplicationException exception) {

        if (exception == null) {
            initApplicationAR();
            // Now add the GL surface view. It is important that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video background is configured.

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();
            addOverlayView(true);

            // Hides the Loading Dialog
            loadingDialogHandler
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try {
                vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
            } catch (VuforiaApplicationException e) {
                Log.e(LOGTAG, e.getString());
            }

            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

            if (!result)
                Log.e(LOGTAG, "Unable to enable continuous autofocus");


        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }


    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        jme.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }


    @Override
    public void onQCARUpdate(State state) {
        com.qualcomm.vuforia.Image imageRGB565 = null;
        Frame frame = state.getFrame();

        for (int i = 0; i < frame.getNumImages(); ++i) {
            com.qualcomm.vuforia.Image image = frame.getImage(i);
            if (image.getFormat() == PIXEL_FORMAT.RGB565) {
                imageRGB565 = image;
                break;
            }
        }

        if (imageRGB565 != null) {
            ByteBuffer pixels = imageRGB565.getPixels();
            byte[] pixelArray = new byte[pixels.remaining()];
            pixels.get(pixelArray, 0, pixelArray.length);

            this.setRGB565CameraImage(pixelArray, imageRGB565.getWidth(), imageRGB565.getHeight());
            Log.d(LOGTAG, "Ovo " + Integer.toString(pixelArray.length ) + Integer.toString(imageRGB565.getWidth() ) + Integer.toString(imageRGB565.getStride()));
        }
    }

    private void addOverlayView(boolean initLayout)
    {
        // Inflates the Overlay Layout to be displayed above the Camera View
        LayoutInflater inflater = LayoutInflater.from(this);
        mUILayoutCamera = (RelativeLayout) inflater.inflate(
                R.layout.camera_overlay_udt, null, false);

        mUILayoutCamera.setVisibility(View.VISIBLE);

        // If this is the first time that the application runs then the
        // uiLayout background is set to BLACK color, will be set to
        // transparent once the SDK is initialized and camera ready to draw

            mUILayoutCamera.setBackgroundColor(Color.TRANSPARENT);


        // Adds the inflated layout to the view
        addContentView(mUILayoutCamera, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // Gets a reference to the bottom navigation bar


        // Gets a reference to the Camera button
        mCameraButton = mUILayoutCamera.findViewById(R.id.camera_button);

        // Gets a reference to the loading dialog container
        //loadingDialogHandler.mLoadingDialogContainer = mUILayoutCamera
         //       .findViewById(R.id.loading_layout);



        mUILayoutCamera.bringToFront();
    }

    public void onCameraClick(View v)
    {

        Log.d(LOGTAG, "AA");

    }

}