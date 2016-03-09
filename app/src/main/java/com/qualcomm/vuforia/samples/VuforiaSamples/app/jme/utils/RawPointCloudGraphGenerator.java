package com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils;



        import java.nio.FloatBuffer;

        import com.jme3.asset.AssetManager;
        import com.jme3.material.Material;
        import com.jme3.material.RenderState.BlendMode;
        import com.jme3.math.ColorRGBA;
        import com.jme3.renderer.queue.RenderQueue.Bucket;
        import com.jme3.renderer.queue.RenderQueue.ShadowMode;
        import com.jme3.scene.Geometry;
        import com.jme3.scene.Mesh;
        import com.jme3.scene.Node;
        import com.jme3.scene.Mesh.Mode;
        import com.jme3.scene.VertexBuffer;
        import com.jme3.util.BufferUtils;


/**
 * This is a very basic point cloud generator for jME3.<br>
 * It will output a Node, which is the root node of the scene graph.<br>
 * There is only one big geometry object in a single bounding volume.<br>
 * But there is still very high performance possible due to the use of point sprites.<br>
 * <br>
 * Licensing: Use at will and "As is", credits would be nice, but aren't necessary...
 *
 * @author Ogli
 */
public class RawPointCloudGraphGenerator extends
        AbstractPointCloudGraphGenerator {

    public RawPointCloudGraphGenerator(AssetManager assetManager) {
        super(assetManager);
    }

    @Override
    protected Node generatePointCloudGraphFrom(
            FloatBuffer pointCoordinates3d)
    {
        FloatBuffer colors = createColorBuffer(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f), pointCoordinates3d);
        return generatePointCloudGraphFrom(pointCoordinates3d, colors);
    }

    @Override
    protected Node generatePointCloudGraphFrom(
            FloatBuffer pointCoordinates3d,
            FloatBuffer colorsRGBA)
    {
        FloatBuffer sizes = createSizeBuffer(1.0f, pointCoordinates3d);
        return generatePointCloudGraphFrom(pointCoordinates3d, colorsRGBA, sizes);
    }

    /**
     * This is the core function (jME3 configuration and outputting a Node).
     *
     * @param pointCoordinates3d - the buffer with (x,y,z) coordinates
     * @param colorsRGBA - the buffer with (R,G,B,A) colors
     * @param sizes - a size buffer (size factor for point sprites size)
     * @return
     */
    protected Node generatePointCloudGraphFrom(
            FloatBuffer pointCoordinates3d,
            FloatBuffer colorsRGBA,
            FloatBuffer sizes)
    {
        Node result = new Node();

        Material mat = new Material(
                assetManager,
                "Common/MatDefs/Misc/Particle.j3md");
        mat.getAdditionalRenderState().setPointSprite(true);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Off);
        mat.setBoolean("PointSprite", true);
        mat.setFloat("Quadratic", 0.25f);

        Mesh m = new Mesh();
        m.setMode(Mode.Points);
        m.setBuffer(VertexBuffer.Type.Position, 3, pointCoordinates3d);
        m.setBuffer(VertexBuffer.Type.Color, 4, colorsRGBA);
        m.setBuffer(VertexBuffer.Type.Size, 1, sizes);
        m.setStatic();
        m.updateBound();

        Geometry g = new Geometry("Point Cloud", m);
        g.setShadowMode(ShadowMode.Off);
        g.setQueueBucket(Bucket.Opaque);
        g.setMaterial(mat);
        g.updateModelBound();

        result.attachChild(g);
        result.updateModelBound();
        return result;
    }

    protected FloatBuffer createColorBuffer(ColorRGBA color, FloatBuffer points)
    {
        int bufferSize = ( points.limit() / 3) * 4;
        FloatBuffer result = BufferUtils.createFloatBuffer(bufferSize);
        for(int i = 0; i < (bufferSize/4); i++)
        {
            result.put(color.r).put(color.g).put(color.b).put(color.a);
        }
        return result;
    }

    protected FloatBuffer createSizeBuffer(float pointSize, FloatBuffer points)
    {
        int bufferSize = points.limit() / 3;
        FloatBuffer result = BufferUtils.createFloatBuffer( bufferSize );
        for(int i = 0; i < bufferSize; i++)
        {
            result.put(pointSize);
        }
        return result;
    }
}