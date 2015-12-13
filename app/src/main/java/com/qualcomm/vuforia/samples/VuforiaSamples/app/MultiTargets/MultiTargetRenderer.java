/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.qualcomm.vuforia.samples.VuforiaSamples.app.MultiTargets;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.MultiTargetResult;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.TrackableResult;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vuforia;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.qualcomm.vuforia.samples.SampleApplication.utils.CubeObject;
import com.qualcomm.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.qualcomm.vuforia.samples.SampleApplication.utils.Texture;


// The renderer class for the MultiTargets sample. 
public class MultiTargetRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "MultiTargetRenderer";
    
    private SampleApplicationSession vuforiaAppSession;
    
    boolean mIsActive = false;
    
    private int shaderProgramID;
    
    private int vertexHandle;
    private int normalHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    
    private Vector<Texture> mTextures;
    
    private double prevTime;
    private float rotateAngle;
    
    private CubeObject cubeObject = new CubeObject();
    private BowlAndSpoonObject bowlAndSpoonObject = new BowlAndSpoonObject();
    
    // Constants:
    final static float kCubeScaleX = 120.0f * 0.75f / 2.0f;
    final static float kCubeScaleY = 120.0f * 1.00f / 2.0f;
    final static float kCubeScaleZ = 120.0f * 0.50f / 2.0f;
    
    final static float kBowlScaleX = 120.0f * 0.15f;
    final static float kBowlScaleY = 120.0f * 0.15f;
    final static float kBowlScaleZ = 120.0f * 0.15f;
    
    
    public MultiTargetRenderer(SampleApplicationSession session)
    {
        vuforiaAppSession = session;
    }
    
    
    // Called when the surface is created or recreated.
    @Override
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
    @Override
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
        
        // Call our function to render content:
        renderFrame();
    }
    
    
    private void initRendering()
    {
        Log.d(LOGTAG, "MultiTargetsRenderer.initRendering");
        
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);
        
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
        
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        
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
    }
    
    
    private void renderFrame()
    {
        SampleUtils.checkGLError("Check gl errors prior render Frame");
        
        // Clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // Get the state from Vuforia and mark the beginning of a rendering
        // section
        State state = Renderer.getInstance().begin();
        
        // Explicitly render the Video Background
        Renderer.getInstance().drawVideoBackground();
        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        
        // Did we find any trackables this frame?
        if (state.getNumTrackableResults() != 0)
        {
            // Get the trackable:
            TrackableResult result = null;
            int numResults = state.getNumTrackableResults();
            
            // Browse results searching for the MultiTarget
            for (int j = 0; j < numResults; j++)
            {
                result = state.getTrackableResult(j);
                if (result.isOfType(MultiTargetResult.getClassType()))
                    break;
                result = null;
            }
            
            // If it was not found exit
            if (result == null)
            {
                // Clean up and leave
                GLES20.glDisable(GLES20.GL_BLEND);
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                
                Renderer.getInstance().end();
                return;
            }
            
            Matrix44F modelViewMatrix_Vuforia = Tool
                .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            float[] modelViewProjection = new float[16];
            Matrix.scaleM(modelViewMatrix, 0, kCubeScaleX, kCubeScaleY,
                kCubeScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            
            GLES20.glUseProgram(shaderProgramID);
            
            // Draw the cube:
            
            // We must detect if background reflection is active and adjust the
            // culling direction.
            // If the reflection is active, this means the post matrix has been
            // reflected as well, therefore standard counter clockwise face
            // culling will result in "inside out" models.
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);
            if (Renderer.getInstance().getVideoBackgroundConfig()
                .getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
                GLES20.glFrontFace(GLES20.GL_CW); // Front camera
            else
                GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
                
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, cubeObject.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                false, 0, cubeObject.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, cubeObject.getTexCoords());
            
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
                cubeObject.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                cubeObject.getIndices());
            
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            
            // Draw the bowl:
            modelViewMatrix = modelViewMatrix_Vuforia.getData();
            
            // Remove the following line to make the bowl stop spinning:
            animateBowl(modelViewMatrix);
            
            Matrix.translateM(modelViewMatrix, 0, 0.0f, -0.50f * 120.0f,
                1.35f * 120.0f);
            Matrix.rotateM(modelViewMatrix, 0, -90.0f, 1.0f, 0, 0);
            
            Matrix.scaleM(modelViewMatrix, 0, kBowlScaleX, kBowlScaleY,
                kBowlScaleZ);
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, bowlAndSpoonObject.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                false, 0, bowlAndSpoonObject.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                GLES20.GL_FLOAT, false, 0, bowlAndSpoonObject.getTexCoords());
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(1).mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                modelViewProjection, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                bowlAndSpoonObject.getNumObjectIndex(),
                GLES20.GL_UNSIGNED_SHORT, bowlAndSpoonObject.getIndices());
            
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);
            
            SampleUtils.checkGLError("MultiTargets renderFrame");
            
        }
        
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        Renderer.getInstance().end();
        
    }
    
    
    private void animateBowl(float[] modelViewMatrix)
    {
        double time = System.currentTimeMillis(); // Get real time difference
        float dt = (float) (time - prevTime) / 1000; // from frame to frame
        
        rotateAngle += dt * 180.0f / 3.1415f; // Animate angle based on time
        rotateAngle %= 360;
        Log.d(LOGTAG, "Delta animation time: " + rotateAngle);
        
        Matrix.rotateM(modelViewMatrix, 0, rotateAngle, 0.0f, 1.0f, 0.0f);
        
        prevTime = time;
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
        
    }
    
}
