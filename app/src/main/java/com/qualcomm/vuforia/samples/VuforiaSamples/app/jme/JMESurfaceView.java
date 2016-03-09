package com.qualcomm.vuforia.samples.VuforiaSamples.app.jme;

/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/



        import com.jme3.asset.TextureKey;
        import com.jme3.material.RenderState;
        import com.jme3.scene.shape.Box;
        import com.jme3.terrain.geomipmap.TerrainQuad;
        import com.jme3.terrain.heightmap.AbstractHeightMap;
        import com.jme3.terrain.heightmap.ImageBasedHeightMap;
        import com.jme3.texture.Texture;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils.Constants;

        import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleMath;
        import com.jme3.animation.AnimChannel;
        import com.jme3.animation.AnimControl;
        import com.jme3.animation.AnimEventListener;
        import com.jme3.animation.LoopMode;
        import com.jme3.app.SimpleApplication;
        import com.jme3.light.DirectionalLight;
        import com.jme3.material.Material;
        import com.jme3.math.ColorRGBA;
        import com.jme3.math.Vector3f;
        import com.jme3.renderer.Camera;
        import com.jme3.renderer.ViewPort;
        import com.jme3.scene.Geometry;
        import com.jme3.scene.Node;
        import com.jme3.scene.Spatial;
        import com.jme3.scene.shape.Quad;
        import com.jme3.texture.Image;
        import com.jme3.texture.Texture2D;
        import com.qualcomm.vuforia.Marker;
        import com.qualcomm.vuforia.MarkerResult;
        import com.qualcomm.vuforia.MarkerTracker;
        import com.qualcomm.vuforia.Matrix44F;
        import com.qualcomm.vuforia.Renderer;
        import com.qualcomm.vuforia.State;
        import com.qualcomm.vuforia.Tool;
        import com.qualcomm.vuforia.TrackableResult;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils.CustomAbstractHeightMap;
        import com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils.CustomImageBasedHeightMap;

        import java.util.ArrayList;



public class JMESurfaceView extends SimpleApplication implements AnimEventListener {
    private static final String LOGTAG = "JMESurfaceView";

    // The geometry which will represent the video background
    private Geometry mVideoBGGeom;
    // The material which will be applied to the video background geometry.
    private Material mvideoBGMat;
    // The texture displaying the Android camera preview frames.
    private Texture2D mCameraTexture;
    // the JME image which serves as intermediate storage place for the Android
    // camera frame before the pixels get uploaded into the texture.
    private Image mCameraImage;
    // A flag indicating if the scene has been already initialized.
    private boolean mSceneInitialized = false;
    // A flag indicating if the JME Image has been already initialized.
    private boolean mVideoImageInitialized = false;
    // A flag indicating if a new Android camera image is available.
    boolean mNewCameraFrameAvailable = false;

    //	private float mForegroundCamFOVY = 30;
    private float mForegroundCamFOVY = 50f; // for a Samsung Galaxy SII

    Camera videoBGCam;
    Camera foregroundCamera;

    // for animation
    // The controller allows access to the animation sequences of the model
    private ArrayList<AnimControl> mAnimControls = new ArrayList<AnimControl>();
    // the channel is used to run one animation sequence at a time


    @Override
    public void simpleInitApp() {

        // Do not display statistics or frames per second
        setDisplayStatView(false);
        setDisplayFps(false);

        // We use custom viewports - so the main viewport does not need to contain the rootNode
        viewPort.detachScene(rootNode);
        initVideoBackground(settings.getWidth(), settings.getHeight());

        initForegroundScene();

        initBackgroundCamera();

        initForegroundCamera(mForegroundCamFOVY);

    }


    public static void main(String[] args) {
        JMESurfaceView app = new JMESurfaceView();
        app.start();
    }


    public void initForegroundCamera(float fovY) {

        foregroundCamera = new Camera(settings.getWidth(), settings.getHeight());

        foregroundCamera.setViewPort(0, 1.0f, 0.f, 1.0f);
        foregroundCamera.setLocation(new Vector3f(0f, 0f, 0f));
        foregroundCamera.setAxes(new Vector3f(-1f, 0f, 0f), new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, -1f));
        foregroundCamera.setFrustumPerspective(fovY, (float) cam.getWidth() / cam.getHeight(), 1f, 1000f);

        ViewPort fgVP = renderManager.createMainView("ForegroundView", foregroundCamera);
        fgVP.attachScene(rootNode);
        //color,depth,stencil
        fgVP.setClearFlags(false, true, false);
        fgVP.setBackgroundColor(new ColorRGBA(0, 0, 0, 1));
    }


    public void initForegroundScene() {
        // Load a model from  OgreXML + material + texture
                // Spatial ninja = assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
                // ninja.setName(Constants.MARKER_0); //Set name to identify which 3d model to show
                // ninja.scale(2f, 2f, 2f);
                // ninja.rotate(0.0f, 3.14f, 0.0f); //Rotate ninja 3.14 radians, 180 degrees
                // ninja.setLocalTranslation(0.0f, -2.5f, -50.0f);

       // Box b = new Box(30f,30f,30f);
       // Geometry ninja = new Geometry("Geometrija", b);
       // Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
       // Texture textura = assetManager.loadTexture("Textures/Jasmine.jpg");
       // //mat.setColor("Color", ColorRGBA.Blue);
       // mat.setTexture("ColorMap", textura);
       // ninja.setMaterial(mat);
       // ninja.setName(Constants.MARKER_0);
//
       // ninja.setLocalTranslation(0,0,0);
//
//
       // rootNode.attachChild(ninja); //Atach node to rootNode






        Material mat_terrain;
        mat_terrain = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");

        mat_terrain.setTexture("ColorMap", assetManager.loadTexture("Textures/AlgeriaBlack.png"));
        mat_terrain.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        /** 2. Create the height map */
        CustomAbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/AlgeriaBlack.jpg");
        heightmap = new CustomImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        heightmap.smooth(1);
        //heightmap.smooth(1, 2);
        int patchSize = 65;
        TerrainQuad terrain;
        terrain = new TerrainQuad("my terrain", patchSize, 513, heightmap.getHeightMap());
        terrain.scale(0.3f);
        terrain.rotate(1.57f,0,0);

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(mat_terrain);
//        terrain.setLocalScale(2f, 1f, 2f);
        terrain.setName(Constants.MARKER_0);
        rootNode.attachChild(terrain);


























        // You must add a light to make the model visible
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White.mult(1f));
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        rootNode.addLight(sun);

        // Init animations
        mAnimControls.clear();
        //findAnimControls(ninja); //Find the AnimControlers
        loadAnimation(); //Load Animation
    }


    public void initBackgroundCamera() {
        // Create a custom virtual camera with orthographic projection
        videoBGCam = new Camera(settings.getWidth(), settings.getHeight());
        videoBGCam.setViewPort(0.0f, 1.0f, 0.f, 1.0f);
        videoBGCam.setLocation(new Vector3f(0f, 0f, 1.f));
        videoBGCam.setAxes(new Vector3f(-1f, 0f, 0f), new Vector3f(0f, 1f, 0f), new Vector3f(0f, 0f, -1f));
        videoBGCam.setParallelProjection(true);

        // Also create a custom viewport.
        ViewPort videoBGVP = renderManager.createMainView("VideoBGView",
                videoBGCam);
        // Attach the geometry representing the video background to the viewport.
        videoBGVP.attachScene(mVideoBGGeom);
    }


    // This method retrieves the preview images from the Android world and puts them into a JME image.
    public void setVideoBGTexture(final Image image) {
        if (!mSceneInitialized) {
            return;
        }
        mCameraImage = image;

        mNewCameraFrameAvailable = true;
    }


    // This function creates the geometry, the viewport and the virtual camera
    // needed for rendering the incoming Android camera frames in the scene graph
    public void initVideoBackground(int screenWidth, int screenHeight) {
        // Create a Quad shape.

        Quad videoBGQuad = new Quad(1, 1, true);
        // Create a Geometry with the Quad shape
        mVideoBGGeom = new Geometry("quad", videoBGQuad);

        float newWidth = 1.f * screenWidth / screenHeight;

        // Center the Geometry in the middle of the screen.
        mVideoBGGeom.setLocalTranslation(-0.5f * newWidth, -0.5f, 0.f);
        // Scale (stretch) the width of the Geometry to cover the whole screen
        // width.
        mVideoBGGeom.setLocalScale(newWidth, 1.f, 1.f);

        // Apply a unshaded material which we will use for texturing.
        mvideoBGMat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mVideoBGGeom.setMaterial(mvideoBGMat);
        // Create a new texture which will hold the Android camera preview frame
        // pixels.
        mCameraTexture = new Texture2D();

        mSceneInitialized = true;
    }


    @Override
    public void simpleUpdate(float tpf) {


        if (mNewCameraFrameAvailable) {
            mCameraTexture.setImage(mCameraImage);
            mvideoBGMat.setTexture("ColorMap", mCameraTexture);
        }

        mVideoBGGeom.updateLogicalState(tpf);
        mVideoBGGeom.updateGeometricState();

        updateVuforiaTracking();
    }


    private void updateVuforiaTracking() {
        // Get the state from vuforia and update the scene rendering
        State state = Renderer.getInstance().begin();

        // Hide all objects
        for (Spatial s : rootNode.getChildren()) {
            s.setCullHint(Spatial.CullHint.Always);
        }

        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            // Get the trackable
            TrackableResult trackableResult = state.getTrackableResult(tIdx);

            // Check the type of the trackable
            assert (trackableResult.getType() == MarkerTracker.getClassType());
            MarkerResult markerResult = (MarkerResult) (trackableResult);
            Marker marker = (Marker) markerResult.getTrackable();

            // Identify marker 0 to show 3d model. You can customize this to your own markers
            if (marker.getMarkerId() == 0) {
                processTrackable(trackableResult);
            }
        }

        Renderer.getInstance().end();
    }


    private void processTrackable(TrackableResult trackableScene) {

        Spatial fight = rootNode.getChild(Constants.MARKER_0); // This can me changed according to the number of models loaded. In this case I am showing only one model for marker 0.
        fight.setCullHint(Spatial.CullHint.Dynamic); // Show 3d model

        Matrix44F modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(trackableScene.getPose());
        Matrix44F inverseMatrix_Vuforia = SampleMath.Matrix44FInverse(modelViewMatrix_Vuforia);
        Matrix44F inverseTrasposedMatrix_Vuforia = SampleMath.Matrix44FTranspose(inverseMatrix_Vuforia);

        float[] modelViewMatrix = inverseTrasposedMatrix_Vuforia.getData();

        //Get Camera Position
        float cam_x = modelViewMatrix[12];
        float cam_y = modelViewMatrix[13];
        float cam_z = modelViewMatrix[14];

        float cam_right_x = modelViewMatrix[0];
        float cam_right_y = modelViewMatrix[1];
        float cam_right_z = modelViewMatrix[2];
        float cam_up_x = modelViewMatrix[4];
        float cam_up_y = modelViewMatrix[5];
        float cam_up_z = modelViewMatrix[6];
        float cam_dir_x = modelViewMatrix[8];
        float cam_dir_y = modelViewMatrix[9];
        float cam_dir_z = modelViewMatrix[10];

        setCameraPoseFromVuforia(cam_x, cam_y, cam_z);
        setCameraOrientationFromVuforia(cam_right_x, cam_right_y, cam_right_z, cam_up_x, cam_up_y, cam_up_z, cam_dir_x, cam_dir_y, cam_dir_z);
    }


    public void setCameraPoseFromVuforia(float cam_x, float cam_y, float cam_z) {
        foregroundCamera.setLocation(new Vector3f(cam_x, cam_y, cam_z));
    }


    public void setCameraOrientationFromVuforia(float cam_right_x, float cam_right_y, float cam_right_z,
                                                float cam_up_x, float cam_up_y, float cam_up_z, float cam_dir_x, float cam_dir_y, float cam_dir_z) {
        foregroundCamera.setAxes(
                new Vector3f(-cam_right_x, -cam_right_y, -cam_right_z),
                new Vector3f(-cam_up_x, -cam_up_y, -cam_up_z),
                new Vector3f(cam_dir_x, cam_dir_y, cam_dir_z));
    }


    /**
     * Method to find the animation control, because it is not on the models root
     * node.
     *
     * @param parent The spatial to search.
     * @return The {@link AnimControl} or null if it does not exist.
     */
    private AnimControl findAnimControls(final Spatial parent) {

        final AnimControl animControl = parent.getControl(AnimControl.class);
        if (animControl != null) {
            mAnimControls.add(animControl);
            return animControl;
        }

        if (parent instanceof Node) {
            for (final Spatial s : ((Node) parent).getChildren()) {
                AnimControl animControl2 = findAnimControls(s);
                if (animControl2 != null) {
                    mAnimControls.add(animControl2);
                }
            }
        }
        return null;
    }

    private void loadAnimation() {
        for (AnimControl animControl : mAnimControls) {
            if (animControl.getAnimationNames().size() > 0) {
                //animControl.addListener(this);
                AnimChannel mAniChannel = animControl.createChannel();
                for (String name : animControl.getAnimationNames()) { //Check de animation names and load them
                    //show animation from beginning
                    mAniChannel.setAnim(name);
                    mAniChannel.setLoopMode(LoopMode.Loop);
                    mAniChannel.setSpeed(1f);
                }
            }
        }
    }

    @Override
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

    }

    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

    }
}