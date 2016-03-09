package com.qualcomm.vuforia.samples.VuforiaSamples.app.vuforia;

/*===============================================================================
Copyright (c) 2012-2015 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/


        import android.app.Activity;
        import android.content.pm.ActivityInfo;
        import android.content.res.Configuration;
        import android.os.AsyncTask;
        import android.os.Build;
        import android.util.DisplayMetrics;
        import android.util.Log;
        import android.view.WindowManager;

        import android.R;
        import com.qualcomm.vuforia.CameraCalibration;
        import com.qualcomm.vuforia.CameraDevice;
        import com.qualcomm.vuforia.HINT;
        import com.qualcomm.vuforia.Matrix44F;
        import com.qualcomm.vuforia.PIXEL_FORMAT;
        import com.qualcomm.vuforia.Renderer;
        import com.qualcomm.vuforia.State;
        import com.qualcomm.vuforia.Tool;
        import com.qualcomm.vuforia.Vec2I;
        import com.qualcomm.vuforia.VideoBackgroundConfig;
        import com.qualcomm.vuforia.VideoMode;
        import com.qualcomm.vuforia.Vuforia;
        import com.qualcomm.vuforia.Vuforia.UpdateCallbackInterface;


public class VuforiaApplicationSession implements UpdateCallbackInterface {

    private static final String LOGTAG = "Vuforia_Sample_Applications";

    // Reference to the current activity
    private Activity mActivity;
    private VuforiaApplicationControl mSessionControl;

    // Flags
    private boolean mStarted = false;
    private boolean mCameraRunning = false;

    // Display size of the device:
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    // The async tasks to initialize the Vuforia SDK:
    private InitVuforiaTask mInitVuforiaTask;
    private LoadTrackerTask mLoadTrackerTask;

    /**
     * An object used for synchronizing Vuforia initialization, dataset loading
     * and the Android onDestroy() life cycle event. If the application is
     * destroyed while a data set is still being loaded, then we wait for the
     * loading operation to finish before shutting down Vuforia:
     **/
    private Object mShutdownLock = new Object();

    // Vuforia initialization flags:
    private int mVuforiaFlags = 0;

    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA.CAMERA_DEFAULT;

    // Stores the projection matrix to use for rendering purposes
    private Matrix44F mProjectionMatrix;

    // Stores orientation
    private boolean mIsPortrait = false;


    public VuforiaApplicationSession(VuforiaApplicationControl sessionControl) {
        mSessionControl = sessionControl;
    }


    // Initializes Vuforia and sets up preferences.
    public void initAR(Activity activity, int screenOrientation) {
        VuforiaApplicationException vuforiaException = null;
        mActivity = activity;

        if ((screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
                && (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

        updateActivityOrientation();

        // Query display dimensions:
        storeScreenDimensions();

        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright:
        mActivity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVuforiaFlags = Vuforia.GL_20;

        /** Initialize Vuforia SDK asynchronously to avoid blocking the
         main (UI) thread.
         NOTE: This task instance must be created and invoked on the
         UI thread and it can be executed only once! **/
        if (mInitVuforiaTask != null) {
            String logMessage = "Cannot initialize SDK twice";
            vuforiaException = new VuforiaApplicationException(
                    VuforiaApplicationException.VUFORIA_ALREADY_INITIALIZATED,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException == null) {
            try {
                mInitVuforiaTask = new InitVuforiaTask();
                mInitVuforiaTask.execute();
            } catch (Exception e) {
                String logMessage = "Initializing Vuforia SDK failed";
                vuforiaException = new VuforiaApplicationException(
                        VuforiaApplicationException.INITIALIZATION_FAILURE,
                        logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (vuforiaException != null)
            mSessionControl.onInitARDone(vuforiaException);
    }


    // Starts Vuforia, initialize and starts the camera and start the trackers
    public void startAR(int camera) throws VuforiaApplicationException {
        Log.d(LOGTAG, "STARTING AR");
        String error;
        if (mCameraRunning) {
            error = "Camera already running, unable to open again";
            Log.e(LOGTAG, error);
            throw new VuforiaApplicationException(
                    VuforiaApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        mCamera = camera;
        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaApplicationException(
                    VuforiaApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().selectVideoMode(
                CameraDevice.MODE.MODE_DEFAULT)) {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new VuforiaApplicationException(
                    VuforiaApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        // Configure the rendering of the video background
        configureVideoBackground();

        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new VuforiaApplicationException(
                    VuforiaApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        setProjectionMatrix();

        mSessionControl.doStartTrackers();

        mCameraRunning = true;

        if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {

            Log.e(LOGTAG, "CONTNUOUS FOCUS FAILS");
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
        }

        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);
    }


    // Stops any ongoing initialization, stops Vuforia
    public void stopAR() throws VuforiaApplicationException {
        Log.e(LOGTAG, "STOP");
        // Cancel potentially running tasks
        if (mInitVuforiaTask != null
                && mInitVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED) {
            mInitVuforiaTask.cancel(true);
            mInitVuforiaTask = null;
        }

        if (mLoadTrackerTask != null
                && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED) {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        mInitVuforiaTask = null;
        mLoadTrackerTask = null;

        mStarted = false;

        stopCamera();

        // Ensure that all asynchronous operations to initialize Vuforia
        // and loading the tracker datasets do not overlap:
        synchronized (mShutdownLock) {

            boolean unloadTrackersResult;
            boolean deinitTrackersResult;

            // Destroy the tracking data set:
            unloadTrackersResult = mSessionControl.doUnloadTrackersData();

            // Deinitialize the trackers:
            deinitTrackersResult = mSessionControl.doDeinitTrackers();

            // Deinitialize Vuforia SDK:
            Vuforia.deinit();

            if (!unloadTrackersResult)
                throw new VuforiaApplicationException(
                        VuforiaApplicationException.UNLOADING_TRACKERS_FAILURE,
                        "Failed to unload trackers\' data");

            if (!deinitTrackersResult)
                throw new VuforiaApplicationException(
                        VuforiaApplicationException.TRACKERS_DEINITIALIZATION_FAILURE,
                        "Failed to deinitialize trackers");

        }
    }


    // Resumes Vuforia, restarts the trackers and the camera
    public void resumeAR() throws VuforiaApplicationException {
        // Vuforia-specific resume operation
        Vuforia.onResume();

        if (mStarted) {
            startAR(mCamera);
        }
    }


    // Pauses Vuforia and stops the camera
    public void pauseAR() throws VuforiaApplicationException {
        if (mStarted) {
            stopCamera();
        }

        Vuforia.onPause();
    }


    // Gets the projection matrix to be used for rendering
    public Matrix44F getProjectionMatrix() {
        return mProjectionMatrix;
    }


    // Callback called every cycle
    @Override
    public void QCAR_onUpdate(State s) {
        mSessionControl.onQCARUpdate(s);
    }


    // Manages the configuration changes
    public void onConfigurationChanged() {
        updateActivityOrientation();

        storeScreenDimensions();

        if (isARRunning()) {
            // configure video background
            configureVideoBackground();

            // Update projection matrix:
            setProjectionMatrix();
        }

    }


    // Methods to be called to handle lifecycle
    public void onResume() {
        Vuforia.onResume();
    }


    public void onPause() {
        Vuforia.onPause();
    }


    public void onSurfaceChanged(int width, int height) {
        Vuforia.onSurfaceChanged(width, height);
    }


    public void onSurfaceCreated() {
        Vuforia.onSurfaceCreated();
    }

    // An async task to initialize Vuforia asynchronously.
    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean> {
        // Initialize with invalid value:
        private int mProgressValue = -1;


        protected Boolean doInBackground(Void... params) {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mShutdownLock) {
                Vuforia.setInitParameters(mActivity, mVuforiaFlags, "AZuC5L7/////AAAAAZjx+ui9nkT1ru/MsjjLLiuC+31Ef/QZOyvzWfWWuujGXe2werdeEoilDPBB7I31sZvMROEbG33s77LdxezhGBimxr4BzfEEaplGIr4TzUY561UzdbHrwJ5eJFJYYsbTz9bkrt6/gcp0Ht/pX8EN7bDKzXyrfYzNp+Q6PosX9Jh+Puo9uqFNjx4iyKKvNsgEJv5BHzWARJtMFXz10jRO3q/gvMBCgrq94SIcuRGn218V5Z9e4ubzqTXbom6TcuuGeK3dCo030fP1Z97wC7q9QOw6pjKNHsz8hvvLsy8C71eW+3lVCiII5FKeU0U/fxoHG4Vr+86DbWpl8E6CikB1c6MMsKohNY+R+iGQHakR7XO0");
                //Vuforia allow multiple targets. In this example two
                //Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_OBJECT_TARGETS, 2);
                do {
                    /** Vuforia.init() blocks until an initialization step is
                     complete, then it proceeds to the next step and reports
                     progress in percents (0 ... 100%).
                     If Vuforia.init() returns -1, it indicates an error.
                     Initialization is done when progress has reached 100%. **/
                    mProgressValue = Vuforia.init();

                    // Publish the progress value:
                    publishProgress(mProgressValue);

                    /**We check whether the task has been canceled in the
                     meantime (by calling AsyncTask.cancel(true)).
                     and bail out if it has, thus stopping this thread.
                     This is necessary as the AsyncTask will run to completion
                     regardless of the status of the component that
                     started is. **/
                } while (!isCancelled() && mProgressValue >= 0
                        && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }


        protected void onProgressUpdate(Integer... values) {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }


        protected void onPostExecute(Boolean result) {
            // Done initializing Vuforia, proceed to next application
            // initialization status:

            VuforiaApplicationException vuforiaException = null;

            if (result) {
                Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: Vuforia "
                        + "initialization successful");

                boolean initTrackersResult;
                initTrackersResult = mSessionControl.doInitTrackers();

                if (initTrackersResult) {
                    try {
                        mLoadTrackerTask = new LoadTrackerTask();
                        mLoadTrackerTask.execute();
                    } catch (Exception e) {
                        String logMessage = "Loading tracking data set failed";
                        vuforiaException = new VuforiaApplicationException(
                                VuforiaApplicationException.LOADING_TRACKERS_FAILURE,
                                logMessage);
                        Log.e(LOGTAG, logMessage);
                        mSessionControl.onInitARDone(vuforiaException);
                    }

                } else {
                    vuforiaException = new VuforiaApplicationException(
                            VuforiaApplicationException.TRACKERS_INITIALIZATION_FAILURE,
                            "Failed to initialize trackers");
                    mSessionControl.onInitARDone(vuforiaException);
                }
            } else {
                String logMessage;

                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                logMessage = getInitializationErrorString(mProgressValue);

                // Log error:
                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: " + logMessage
                        + " Exiting.");

                // Send Vuforia Exception to the application and call initDone
                // to stop initialization process
                vuforiaException = new VuforiaApplicationException(
                        VuforiaApplicationException.INITIALIZATION_FAILURE,
                        logMessage);
                mSessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    // An async task to load the tracker data asynchronously.
    private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean> {
        protected Boolean doInBackground(Void... params) {
            // Prevent the onDestroy() method to overlap:
            synchronized (mShutdownLock) {
                // Load the tracker data set:
                return mSessionControl.doLoadTrackersData();
            }
        }


        protected void onPostExecute(Boolean result) {

            VuforiaApplicationException vuforiaException = null;

            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            if (!result) {
                String logMessage = "Failed to load tracker data.";
                // Error loading dataset
                Log.e(LOGTAG, logMessage);
                vuforiaException = new VuforiaApplicationException(
                        VuforiaApplicationException.LOADING_TRACKERS_FAILURE,
                        logMessage);
            } else {
                /** Hint to the virtual machine that it would be a good time to
                 run the garbage collector:

                 NOTE: This is only a hint. There is no guarantee that the
                 garbage collector will actually be run. **/
                System.gc();

                Vuforia.registerCallback(VuforiaApplicationSession.this);

                mStarted = true;
            }

            // Done loading the tracker, update application status, send the
            // exception to check errors
            mSessionControl.onInitARDone(vuforiaException);
        }
    }


    // Returns the error message for each error code
    private String getInitializationErrorString(int code) {
        if (code == Vuforia.INIT_DEVICE_NOT_SUPPORTED)
            return "INIT_ERROR_DEVICE_NOT_SUPPORTED)";
        if (code == Vuforia.INIT_NO_CAMERA_ACCESS)
            return "INIT_ERROR_NO_CAMERA_ACCESS)";
        if (code == Vuforia.INIT_LICENSE_ERROR_MISSING_KEY)
            return "INIT_LICENSE_ERROR_MISSING_KEY)";
        if (code == Vuforia.INIT_LICENSE_ERROR_INVALID_KEY)
            return "INIT_LICENSE_ERROR_INVALID_KEY)";
        if (code == Vuforia.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
            return "INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)";
        if (code == Vuforia.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
            return "INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)";
        if (code == Vuforia.INIT_LICENSE_ERROR_CANCELED_KEY)
            return "INIT_LICENSE_ERROR_CANCELED_KEY)";
        if (code == Vuforia.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
            return "INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)";
        else {
            return "INIT_LICENSE_ERROR_UNKNOWN_ERROR)";
        }
    }


    // Stores screen dimensions
    private void storeScreenDimensions() {
        // Query display dimensions:
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
    }


    // Stores the orientation depending on the current resources configuration
    private void updateActivityOrientation() {
        Configuration config = mActivity.getResources().getConfiguration();

        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                mIsPortrait = true;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                mIsPortrait = false;
                break;
            case Configuration.ORIENTATION_UNDEFINED:
            default:
                break;
        }

        Log.i(LOGTAG, "Activity is in "
                + (mIsPortrait ? "PORTRAIT" : "LANDSCAPE"));
    }


    // Method for setting / updating the projection matrix for AR content
    // rendering
    public void setProjectionMatrix() {
        CameraCalibration camCal = CameraDevice.getInstance()
                .getCameraCalibration();
        mProjectionMatrix = Tool.getProjectionGL(camCal, 10.0f, 5000.0f);
    }


    public void stopCamera() {
        if (mCameraRunning) {
            mSessionControl.doStopTrackers();
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
            mCameraRunning = false;
        }
    }


    // Configures the video mode and sets offsets for the camera's image
    private void configureVideoBackground() {
        CameraDevice cameraDevice = CameraDevice.getInstance();
        VideoMode vm = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT);

        VideoBackgroundConfig config = new VideoBackgroundConfig();
        config.setEnabled(true);
        config.setPosition(new Vec2I(0, 0));

        int xSize = 0, ySize = 0;
        if (mIsPortrait) {
            xSize = (int) (vm.getHeight() * (mScreenHeight / (float) vm
                    .getWidth()));
            ySize = mScreenHeight;

            if (xSize < mScreenWidth) {
                xSize = mScreenWidth;
                ySize = (int) (mScreenWidth * (vm.getWidth() / (float) vm
                        .getHeight()));
            }
        } else {
            xSize = mScreenWidth;
            ySize = (int) (vm.getHeight() * (mScreenWidth / (float) vm
                    .getWidth()));

            if (ySize < mScreenHeight) {
                xSize = (int) (mScreenHeight * (vm.getWidth() / (float) vm
                        .getHeight()));
                ySize = mScreenHeight;
            }
        }

        config.setSize(new Vec2I(xSize, ySize));

        Log.i(LOGTAG, "As Configure Video Background : Video (" + vm.getWidth()
                + " , " + vm.getHeight() + "), Screen (" + mScreenWidth + " , "
                + mScreenHeight + "), mSize (" + xSize + " , " + ySize + ")");

        Renderer.getInstance().setVideoBackgroundConfig(config);

    }

    // Returns true if Vuforia is initialized, the trackers started and the
    // tracker data loaded
    private boolean isARRunning() {
        return mStarted;
    }

}