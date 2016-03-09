package com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils;

/**
 * Created by Gotti on 2/3/16.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */




        import java.nio.FloatBuffer;
        import java.util.Collection;
        import java.util.Iterator;

        import com.jme3.asset.AssetManager;
        import com.jme3.math.ColorRGBA;
        import com.jme3.math.Vector3f;
        import com.jme3.scene.Node;
        import com.jme3.util.BufferUtils;


/**
 * Abstract superclass, unifies the input to use FloatBuffers.<br>
 * This is an implementation of the { @link PointCloudJme3GraphGenerator}.<br>
 * Override the generator methods in a subclass.<br>
 * <br>
 * Licensing: Use at will and "As is", credits would be nice, but aren't necessary...
 *
 * @author Ogli
 */
public abstract class AbstractPointCloudGraphGenerator implements PointCloudJme3GraphGenerator  {

    protected AssetManager assetManager;

    public AbstractPointCloudGraphGenerator(AssetManager assetManager)
    {
        this.assetManager = assetManager;
    }

    /**
     * Implement this in non-abstract subclasses, to generate the scene graph.
     * This is the version where the color buffer is null (only coordinates given).
     * @param pointCoordinates3d the coordinates of the points (mustn't be null)
     * @return the root node of the scene graph
     */
    protected abstract Node generatePointCloudGraphFrom(
            FloatBuffer pointCoordinates3d);

    /**
     * Implement this in non-abstract subclasses, to generate the scene graph.
     * This is the version for an attributed point cloud (coordinates and colors given).
     * @param pointCoordinates3d the coordinates of the points (mustn't be null)
     * @param colorsRGBA the colors for each point (mustn't be null)
     * @return the root node of the scene graph
     */
    protected abstract Node generatePointCloudGraphFrom(
            FloatBuffer pointCoordinates3d,
            FloatBuffer colorsRGBA);


    //interface PointCloudJme3GraphGenerator:


    public Node generatePointCloudGraph(
            float[] pointCoordinates3d) throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");

        if((pointCoordinates3d.length % 3) != 0)
            throw new Exception("number of point coordinates must be a multiple of 3!");

        FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

        return generatePointCloudGraph(coords, null);
    }


    public Node generatePointCloudGraph(
            FloatBuffer pointCoordinates3d) throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");

        FloatBuffer coords = pointCoordinates3d;

        return generatePointCloudGraph(coords, null);
    }

    @Override
    public Node generatePointCloudGraph(
            Vector3f[] pointCoordinates3d) throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");

        FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

        return generatePointCloudGraph(coords, null);
    }


    public Node generatePointCloudGraph(
            Collection<Vector3f> pointCoordinates3d) throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");

        FloatBuffer coords = BufferUtils.createFloatBuffer( 3 * pointCoordinates3d.size() );
        Iterator<Vector3f> it = pointCoordinates3d.iterator();
        Vector3f current;
        while(it.hasNext())
        {
            current = it.next();
            coords.put(current.x).put(current.y).put(current.z);
        }
        coords.rewind();

        return generatePointCloudGraph(coords, null);
    }

    @Override
    public Node generatePointCloudGraph(
            float[] pointCoordinates3d,
            float[] colorsRGBA) throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");
        if(colorsRGBA == null) return generatePointCloudGraph(pointCoordinates3d);

        if((pointCoordinates3d.length % 3) != 0)
            throw new NumberFormatException("number of point coordinates must be a multiple of 3!");

        if((colorsRGBA.length % 4) != 0)
            throw new NumberFormatException("number of color values must be a multiple of 4!");

        if(pointCoordinates3d.length/3 != colorsRGBA.length/4)
            throw new Exception("There should be a color value for each point, if colors are used!");

        FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

        FloatBuffer colors = BufferUtils.createFloatBuffer(colorsRGBA);

        return generatePointCloudGraph(coords, colors);
    }

    @Override
    public Node generatePointCloudGraph(
            FloatBuffer pointCoordinates3d,
            FloatBuffer colorsRGBA)  throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");

        //now - this method calls the main generator function:
        if(colorsRGBA == null)
            return generatePointCloudGraphFrom(pointCoordinates3d);
        else
            return generatePointCloudGraphFrom(pointCoordinates3d, colorsRGBA);
    }

    @Override
    public Node generatePointCloudGraph(
            Vector3f[] pointCoordinates3d,
            ColorRGBA[] colorsRGBA)  throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");
        if(colorsRGBA == null) return generatePointCloudGraph(pointCoordinates3d);

        if(pointCoordinates3d.length != colorsRGBA.length)
            throw new Exception("There should be a color value for each point, if colors are used!");

        FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

        FloatBuffer colors = BufferUtils.createFloatBuffer( 4 * colorsRGBA.length );
        for(int i = 0; i < colorsRGBA.length; i++)
        {
            colors.put(colorsRGBA[i].r).put(colorsRGBA[i].g).put(colorsRGBA[i].b).put(colorsRGBA[i].a);
        }
        colors.rewind();

        return generatePointCloudGraph(coords, colors);
    }

    @Override
    public Node generatePointCloudGraph(
            Collection<Vector3f> pointCoordinates3d,
            Collection<ColorRGBA> colorsRGBA) throws Exception
    {
        if(pointCoordinates3d == null) throw new Exception("point cloud mustn'nt be null!");
        if(colorsRGBA == null) return generatePointCloudGraph(pointCoordinates3d);

        if(pointCoordinates3d.size() != colorsRGBA.size())
            throw new Exception("There should be a color value for each point, if colors are used!");

        FloatBuffer coords = BufferUtils.createFloatBuffer( 3 * pointCoordinates3d.size() );
        Iterator<Vector3f> it = pointCoordinates3d.iterator();
        Vector3f current;
        while(it.hasNext())
        {
            current = it.next();
            coords.put(current.x).put(current.y).put(current.z);
        }
        coords.rewind();

        FloatBuffer colors = BufferUtils.createFloatBuffer( 4 * colorsRGBA.size() );
        Iterator<ColorRGBA> it2 = colorsRGBA.iterator();
        ColorRGBA current2;
        while(it2.hasNext())
        {
            current2 = it2.next();
            colors.put(current2.r).put(current2.g).put(current2.b).put(current2.a);
        }
        colors.rewind();

        return generatePointCloudGraph(coords, colors);
    }

}