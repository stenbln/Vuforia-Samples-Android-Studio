/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.qualcomm.vuforia.samples.VuforiaSamples.app.CylinderTargets;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.qualcomm.vuforia.CylinderTargetResult;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vuforia;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.qualcomm.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.qualcomm.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleApplication3DModel;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.qualcomm.vuforia.samples.SampleApplication.utils.Texture;


// The renderer class for the ImageTargets sample.
public class CylinderTargetRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "CylinderTargetRenderer";
    
    // Reference to main activity
    private CylinderTargets mActivity;
    
    private SampleApplicationSession vuforiaAppSession;
    
    private Vector<Texture> mTextures;
    
    private int shaderProgramID;
    
    private int vertexHandle;
    
    private int normalHandle;
    
    private int textureCoordHandle;
    
    private int mvpMatrixHandle;
    
    private int texSampler2DHandle;
    
    private Renderer mRenderer;
    
    CylinderModel mCylinderModel;
    
    // dimensions of the cylinder (as set in the TMS tool)
    private float kCylinderHeight = 95.0f;
    private float kCylinderTopDiameter = 65.0f;
    private float kCylinderBottomDiameter = 65.0f;
    
    // ratio between top and bottom diameter
    // used to generate the model of the cylinder
    private float kCylinderTopRadiusRatio = kCylinderTopDiameter
        / kCylinderBottomDiameter;
    
    // the height of the tea pot
    private float kObjectHeight = 1.0f;
    
    // we want the object to be the 1/3 of the height of the cylinder
    private float kRatioCylinderObjectHeight = 3.0f;
    
    // Scaling of the object to match the ratio we want
    private float kObjectScale = kCylinderHeight
        / (kRatioCylinderObjectHeight * kObjectHeight);
    
    // scaling of the cylinder model to fit the actual cylinder
    private float kCylinderScaleX = kCylinderBottomDiameter / 2.0f;
    private float kCylinderScaleY = kCylinderBottomDiameter / 2.0f;
    private float kCylinderScaleZ = kCylinderHeight;
    
    private SampleApplication3DModel mSphereModel;
    
    private double prevTime;
    private float rotateBallAngle;
    
    public boolean mIsActive = false;
    
    
    public CylinderTargetRenderer(CylinderTargets activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
    }
    
    
    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        
        // Call function to initialize rendering:
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
    }
    
    
    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        
        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }
    
    
    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;
        
        // Call our function to render content
        renderFrame();
    }
    
    
    private void initRendering()
    {
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
        mRenderer = Renderer.getInstance();
        
        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        SampleUtils.checkGLError("CylinderTargets GLInitRendering");
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        SampleUtils.checkGLError("GLInitRendering");
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");
        SampleUtils.checkGLError("GLInitRendering due");
        SampleUtils
            .checkGLError("CylinderTargets GLInitRendering getting location att and unif");
        
        try
        {
            mSphereModel = new SampleApplication3DModel();
            mSphereModel.loadModel(mActivity.getResources().getAssets(),
                "CylinderTargets/Sphere.txt");
        } catch (IOException e)
        {
            Log.e(LOGTAG, "Unable to load soccer ball");
        }
        
        prevTime = System.currentTimeMillis();
        rotateBallAngle = 0;
        
        mCylinderModel = new CylinderModel(kCylinderTopRadiusRatio);
        
        // Hide the Loading Dialog
        mActivity.loadingDialogHandler
            .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        
    }
    
    
    private void renderFrame()
    {
        
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        State state = mRenderer.begin();
        mRenderer.drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        SampleUtils.checkGLError("CylinderTargets drawVideoBackground");
        
        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW);  // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW);   // Back camera
            
        // did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            
            TrackableResult result = state.getTrackableResult(tIdx);
            if (!result.isOfType(CylinderTargetResult.getClassType()))
                continue;
            
            Matrix44F modelViewMatrix_Vuforia;
            float[] modelViewProjection = new float[16];
            
            // prepare the cylinder
            modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(result
                .getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            Matrix.scaleM(modelViewMatrix, 0, kCylinderScaleX, kCylinderScaleY,
                kCylinderScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            SampleUtils.checkGLError("CylinderTargets prepareCylinder");
            
            GLES20.glUseProgram(shaderProgramID);
            
            // Draw the cylinder:
            
            // We must detect if background reflection is active and adjust the
            // culling direction.
            // If the reflection is active, this means the post matrix has been
            // reflected as well,
            // therefore standard counter clockwise face culling will result in
            // "inside out" models.
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            if (Renderer.getInstance().getVideoBackgroundConfig()
                .getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
                GLES20.glFrontFace(GLES20.GL_CW);  // Front camera
            else
                GLES20.glFrontFace(GLES20.GL_CCW);   // Back camera
                
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, mCylinderModel.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                false, 0, mCylinderModel.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mCylinderModel.getTexCoords());
            
            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(0).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                mCylinderModel.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                mCylinderModel.getIndices());
            
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            SampleUtils.checkGLError("CylinderTargets drawCylinder");
            
            // prepare the object
            modelViewMatrix = Tool.convertPose2GLMatrix(result.getPose())
                .getData();
            
            // draw the anchored object
            animateObject(modelViewMatrix);
            
            // we move away the object from the target
            Matrix.translateM(modelViewMatrix, 0, 1.0f * kCylinderTopDiameter,
                0.0f, kObjectScale);
            Matrix.scaleM(modelViewMatrix, 0, kObjectScale, kObjectScale,
                kObjectScale);
            
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            
            GLES20.glUseProgram(shaderProgramID);
            
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, mSphereModel.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                false, 0, mSphereModel.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, mSphereModel.getTexCoords());
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(1).mTextureID[0]);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
                mSphereModel.getNumObjectVertex());
            
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);
            
            SampleUtils.checkGLError("CylinderTargets renderFrame");
        }
        
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        mRenderer.end();
        
    }
    
    
    private void animateObject(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis();             // Get real time difference
        float dt = (float) (time - prevTime) / 1000;          // from frame to frame
        
        rotateBallAngle += dt * 180.0f / 3.1415f;     // Animate angle based on time
        rotateBallAngle %= 360;
        
        Matrix.rotateM(modelViewMatrix, 0, rotateBallAngle, 0.0f, 0.0f, 1.0f);
        
        prevTime = time;
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }
    
}
