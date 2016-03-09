package com.qualcomm.vuforia.samples.VuforiaSamples.app.jme.utils;



import android.util.Log;

import com.jme3.math.ColorRGBA;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageHeightmap;
import com.jme3.texture.Image;
import com.jme3.texture.image.ImageRaster;


import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.image.ImageRaster;

/**
 * <code>ImageBasedHeightMap</code> is a height map created from the grayscale
 * conversion of an image. The image used currently must have an equal height
 * and width, although future work could scale an incoming image to a specific
 * height and width.
 *
 * @author Mike Kienenberger
 * @version $id$
 */
public class CustomImageBasedHeightMap extends CustomAbstractHeightMap implements ImageHeightmap {


    protected Image colorImage;
    private float backwardsCompScale = 255f;


    public void setImage(Image image) {
        this.colorImage = image;
    }

    /**
     * Creates a HeightMap from an Image. The image will be converted to
     * grayscale, and the grayscale values will be used to generate the height
     * map. White is highest point while black is lowest point.
     *
     * Currently, the Image used must be square (width == height), but future
     * work could rescale the image.
     *
     * @param colorImage
     *            Image to map to the height map.
     */
    public CustomImageBasedHeightMap(Image colorImage) {
        this.colorImage = colorImage;
    }

    public CustomImageBasedHeightMap(Image colorImage, float heightScale) {
        this.colorImage = colorImage;
        this.heightScale = heightScale;
    }

    /**
     * Loads the image data from top left to bottom right
     */
    public boolean load() {
        return load(false, false);
    }

    /**
     * Get the grayscale value, or override in your own sub-classes
     */
    protected float calculateHeight(float red, float green, float blue) {
        return (float) (0.299 * red + 0.587 * green + 0.114 * blue);
    }

    protected float calculateHeight(ColorRGBA color) {
        return (float) (0.299 * color.r + 0.587 * color.g + 0.114 * color.b);
    }

    protected ImageRaster getImageRaster() {
        return ImageRaster.create(colorImage);
    }

    public boolean load(boolean flipX, boolean flipY) {

        int imageWidth = colorImage.getWidth();
        int imageHeight = colorImage.getHeight();
        Log.d("TEST", "CC CCC Widdddsdsdsdsadasfgasgasgasgasgasgasgasgasgsagasgasgasgasgasgasgttj    " + imageWidth + "     Hegihjasgasgasgasgasgttttttt" + imageHeight);


        size = imageWidth;
        ImageRaster raster = getImageRaster();

        heightData = new float[(imageWidth * imageHeight)];

        ColorRGBA colorStore = new ColorRGBA();

        int index = 0;

        for (int h = imageHeight - 1; h >= 0; --h) {
                    for (int w = 0; w < imageWidth; ++w) {
                        //int baseIndex = (h * imageWidth)+ w;
                        //heightData[index++] = getHeightAtPostion(raster, baseIndex, colorStore)*heightScale;
                        heightData[index++] = calculateHeight(raster.getPixel(w, h, colorStore))*heightScale*backwardsCompScale;
                    }
        }




            return true;
    }

    /*protected float getHeightAtPostion(ImageRaster image, int position, ColorRGBA store) {
        switch (image.getFormat()){
            case RGBA8:
                buf.position( position * 4 );
                store.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()));
                return calculateHeight(store.r, store.g, store.b);
            case ABGR8:
                buf.position( position * 4 );
                float a = byte2float(buf.get());
                float b = byte2float(buf.get());
                float g = byte2float(buf.get());
                float r = byte2float(buf.get());
                store.set(r,g,b,a);
                return calculateHeight(store.r, store.g, store.b);
            case RGB8:
                buf.position( position * 3 );
                store.set(byte2float(buf.get()), byte2float(buf.get()), byte2float(buf.get()), 1);
                return calculateHeight(store.r, store.g, store.b);
            case Luminance8:
                buf.position( position );
                return byte2float(buf.get())*255*heightScale;
            case Luminance16:
                ShortBuffer sbuf = buf.asShortBuffer();
                sbuf.position( position );
                return (sbuf.get() & 0xFFFF) / 65535f * 255f * heightScale;
            default:
                throw new UnsupportedOperationException("Image format: "+image.getFormat());
        }
    }

    private float byte2float(byte b){
        return ((float)(b & 0xFF)) / 255f;
    }*/
}