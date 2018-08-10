/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image;

import java.io.IOException;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.util.Constants;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Texture</code> defines a texture object to be used to display an image on a piece of geometry. The image to be
 * displayed is defined by the <code>Image</code> class. All attributes required for texture mapping are contained
 * within this class. This includes mipmapping if desired, magnificationFilter options, apply options and correction
 * options. Default values are as follows: minificationFilter - NearestNeighborNoMipMaps, magnificationFilter -
 * NearestNeighbor, wrap - EdgeClamp on S,T and R, apply - Modulate, environment - None.
 *
 * @see com.ardor3d.image.Image
 */
public abstract class Texture implements Savable {

    public static boolean DEFAULT_STORE_IMAGE = Constants.storeSavableImages;

    public enum Type {
        /**
         * One dimensional texture. (basically a line)
         */
        OneDimensional,
        /**
         * Two dimensional texture (default). A rectangle.
         */
        TwoDimensional,
        /**
         * Three dimensional texture. (A cube)
         */
        ThreeDimensional,
        /**
         * A set of 6 TwoDimensional textures arranged as faces of a cube facing inwards.
         */
        CubeMap
    }

    public enum MinificationFilter {

        /**
         * Nearest neighbor interpolation is the fastest and crudest filtering method - it simply uses the color of the
         * texel closest to the pixel center for the pixel color. While fast, this results in aliasing and shimmering
         * during minification. (GL equivalent: GL_NEAREST)
         */
        NearestNeighborNoMipMaps(false),

        /**
         * In this method the four nearest texels to the pixel center are sampled (at texture level 0), and their colors
         * are combined by weighted averages. Though smoother, without mipmaps it suffers the same aliasing and
         * shimmering problems as nearest NearestNeighborNoMipMaps. (GL equivalent: GL_LINEAR)
         */
        BilinearNoMipMaps(false),

        /**
         * Same as NearestNeighborNoMipMaps except that instead of using samples from texture level 0, the closest
         * mipmap level is chosen based on distance. This reduces the aliasing and shimmering significantly, but does
         * not help with blockiness. (GL equivalent: GL_NEAREST_MIPMAP_NEAREST)
         */
        NearestNeighborNearestMipMap(true),

        /**
         * Same as BilinearNoMipMaps except that instead of using samples from texture level 0, the closest mipmap level
         * is chosen based on distance. By using mipmapping we avoid the aliasing and shimmering problems of
         * BilinearNoMipMaps. (GL equivalent: GL_LINEAR_MIPMAP_NEAREST)
         */
        BilinearNearestMipMap(true),

        /**
         * Similar to NearestNeighborNoMipMaps except that instead of using samples from texture level 0, a sample is
         * chosen from each of the closest (by distance) two mipmap levels. A weighted average of these two samples is
         * returned. (GL equivalent: GL_NEAREST_MIPMAP_LINEAR)
         */
        NearestNeighborLinearMipMap(true),

        /**
         * Trilinear filtering is a remedy to a common artifact seen in mipmapped bilinearly filtered images: an abrupt
         * and very noticeable change in quality at boundaries where the renderer switches from one mipmap level to the
         * next. Trilinear filtering solves this by doing a texture lookup and bilinear filtering on the two closest
         * mipmap levels (one higher and one lower quality), and then linearly interpolating the results. This results
         * in a smooth degradation of texture quality as distance from the viewer increases, rather than a series of
         * sudden drops. Of course, closer than Level 0 there is only one mipmap level available, and the algorithm
         * reverts to bilinear filtering (GL equivalent: GL_LINEAR_MIPMAP_LINEAR)
         */
        Trilinear(true);

        private boolean _usesMipMapLevels;

        private MinificationFilter(final boolean usesMipMapLevels) {
            _usesMipMapLevels = usesMipMapLevels;
        }

        public boolean usesMipMapLevels() {
            return _usesMipMapLevels;
        }
    }

    public enum MagnificationFilter {

        /**
         * Nearest neighbor interpolation is the fastest and crudest filtering mode - it simply uses the color of the
         * texel closest to the pixel center for the pixel color. While fast, this results in texture 'blockiness'
         * during magnification. (GL equivalent: GL_NEAREST)
         */
        NearestNeighbor,

        /**
         * In this mode the four nearest texels to the pixel center are sampled (at the closest mipmap level), and their
         * colors are combined by weighted average according to distance. This removes the 'blockiness' seen during
         * magnification, as there is now a smooth gradient of color change from one texel to the next, instead of an
         * abrupt jump as the pixel center crosses the texel boundary. (GL equivalent: GL_LINEAR)
         */
        Bilinear;

    }

    public enum WrapMode {
        /**
         * Only the fractional portion of the coordinate is considered.
         */
        Repeat,
        /**
         * Only the fractional portion of the coordinate is considered, but if the integer portion is odd, we'll use 1 -
         * the fractional portion. (Introduced around OpenGL1.4) Falls back on Repeat if not supported.
         */
        MirroredRepeat,
        /**
         * coordinate will be clamped to the range [-1/(2N), 1 + 1/(2N)] where N is the size of the texture in the
         * direction of clamping. Falls back on Clamp if not supported.
         */
        BorderClamp,
        /**
         * coordinate will be clamped to the range [1/(2N), 1 - 1/(2N)] where N is the size of the texture in the
         * direction of clamping. Falls back on Clamp if not supported.
         */
        EdgeClamp,
        /**
         * mirrors and clamps to edge the texture coordinate, where mirroring and clamping to edge a value f computes:
         * <code>mirrorClampToEdge(f) = min(1-1/(2*N), max(1/(2*N), abs(f)))</code> where N is the size of the one-,
         * two-, or three-dimensional texture image in the direction of wrapping. (Introduced after OpenGL1.4) Falls
         * back on EdgeClamp if not supported.
         */
        MirrorEdgeClamp;
    }

    public enum WrapAxis {
        /**
         * S wrapping (u or "horizontal" wrap)
         */
        S,
        /**
         * T wrapping (v or "vertical" wrap)
         */
        T,
        /**
         * R wrapping (w or "depth" wrap)
         */
        R;
    }

    /**
     * The shadowing texture compare mode
     */
    public enum DepthTextureCompareMode {
        /** Perform no shadow based comparsion */
        None,
        /** Perform a comparison between source depth and texture depth */
        RtoTexture,
    }

    /**
     * The shadowing texture compare function
     */
    public enum DepthTextureCompareFunc {
        /** Outputs if the source depth is less than the texture depth */
        LessThanEqual,
        /** Outputs if the source depth is greater than the texture depth */
        GreaterThanEqual
    }

    // texture attributes.
    private Image _image = null;
    private final ColorRGBA _borderColor = new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA);

    private final Matrix4 _texMatrix = new Matrix4();

    private float _anisotropicFilterPercent = 0.0f;
    private float _lodBias = 0.0f;

    private MinificationFilter _minificationFilter = MinificationFilter.NearestNeighborNoMipMaps;
    private MagnificationFilter _magnificationFilter = MagnificationFilter.Bilinear;

    private boolean _hasBorder = false;

    private TextureKey _key = null;
    private TextureStoreFormat _storeFormat = TextureStoreFormat.RGBA8;
    private PixelDataType _rttPixelDataType = PixelDataType.UnsignedByte;
    private transient boolean _storeImage = DEFAULT_STORE_IMAGE;

    private DepthTextureCompareMode _depthCompareMode = DepthTextureCompareMode.None;
    private DepthTextureCompareFunc _depthCompareFunc = DepthTextureCompareFunc.GreaterThanEqual;

    private int _textureBaseLevel = 0;
    private int _textureMaxLevel = -1;

    /**
     * Constructor instantiates a new <code>Texture</code> object with default attributes.
     */
    public Texture() {}

    /**
     * sets the color used when texture operations encounter the border of a texture.
     *
     * @param color
     *            the new border color (the default is {@link ColorRGBA#BLACK_NO_ALPHA})
     */
    public void setBorderColor(final ReadOnlyColorRGBA color) {
        _borderColor.set(color);
    }

    /**
     * sets the color used when texture operations encounter the border of a texture.
     *
     * @param red
     * @param green
     * @param blue
     * @param alpha
     */
    public void setBorderColor(final float red, final float green, final float blue, final float alpha) {
        _borderColor.set(red, green, blue, alpha);
    }

    /**
     * @return the MinificationFilterMode of this texture.
     */
    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }

    /**
     * @param minificationFilter
     *            the new MinificationFilterMode for this texture.
     * @throws IllegalArgumentException
     *             if minificationFilter is null
     */
    public void setMinificationFilter(final MinificationFilter minificationFilter) {
        if (minificationFilter == null) {
            throw new IllegalArgumentException("minificationFilter can not be null.");
        }
        _minificationFilter = minificationFilter;
    }

    /**
     * @return the MagnificationFilterMode of this texture.
     */
    public MagnificationFilter getMagnificationFilter() {
        return _magnificationFilter;
    }

    /**
     * @param magnificationFilter
     *            the new MagnificationFilter for this texture.
     * @throws IllegalArgumentException
     *             if magnificationFilter is null
     */
    public void setMagnificationFilter(final MagnificationFilter magnificationFilter) {
        if (magnificationFilter == null) {
            throw new IllegalArgumentException("magnificationFilter can not be null.");
        }
        _magnificationFilter = magnificationFilter;
    }

    /**
     * <code>setImage</code> sets the image object that defines the texture.
     *
     * @param image
     *            the image that defines the texture.
     */
    public void setImage(final Image image) {
        _image = image;
        setDirty();
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context this texture belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the texture id of this texture in the given context. If the texture is not found in the given context, 0
     *         is returned.
     */
    public int getTextureIdForContext(final Object glContext) {
        return _key.getTextureIdForContext(glContext);
    }

    /**
     * @param glContext
     *            the object representing the OpenGL context this texture belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @return the texture id of this texture in the given context as an Integer object. If the texture is not found in
     *         the given context, a 0 integer is returned.
     */
    public Integer getTextureIdForContextAsInteger(final Object glContext) {
        return _key.getTextureIdForContext(glContext);
    }

    /**
     * Sets the id for this texture in regards to the given OpenGL context.
     *
     * @param glContext
     *            the object representing the OpenGL context this texture belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     * @param textureId
     *            the texture id of this texture. To be valid, this must be greater than 0.
     * @throws IllegalArgumentException
     *             if textureId is less than or equal to 0.
     */
    public void setTextureIdForContext(final Object glContext, final int textureId) {
        _key.setTextureIdForContext(glContext, textureId);
    }

    /**
     * <p>
     * Removes any texture id for this texture for the given OpenGL context.
     * </p>
     * <p>
     * Note: This does not remove the texture from the card and is provided for use by code that does remove textures
     * from the card.
     * </p>
     *
     * @param glContext
     *            the object representing the OpenGL context this texture belongs to. See
     *            {@link RenderContext#getGlContextRep()}
     */
    public void removeFromIdCache(final Object glContext) {
        _key.removeFromIdCache(glContext);
    }

    /**
     * @return the image data that makes up this texture. If no image data has been set, this will return null.
     */
    public Image getImage() {
        return _image;
    }

    /**
     * @return the color to be used for border operations. (the default is {@link ColorRGBA#BLACK_NO_ALPHA})
     */
    public ReadOnlyColorRGBA getBorderColor() {
        return _borderColor;
    }

    /**
     * Sets the wrap mode of this texture for a particular axis.
     *
     * @param axis
     *            the texture axis to define a wrapmode on.
     * @param mode
     *            the wrap mode for the given axis of the texture.
     * @throws IllegalArgumentException
     *             if axis or mode are null or invalid for this type of texture
     */
    public abstract void setWrap(WrapAxis axis, WrapMode mode);

    /**
     * Sets the wrap mode of this texture for all axis.
     *
     * @param mode
     *            the wrap mode for the given axis of the texture.
     * @throws IllegalArgumentException
     *             if mode is null or invalid for this type of texture
     */
    public abstract void setWrap(WrapMode mode);

    /**
     * @param axis
     *            the axis to return for
     * @return the wrap mode for the given coordinate axis on this texture.
     * @throws IllegalArgumentException
     *             if axis is null or invalid for this type of texture
     */
    public abstract WrapMode getWrap(WrapAxis axis);

    /**
     * @return the {@link Type} enum value of this Texture object.
     */
    public abstract Type getType();

    /**
     * @return the anisotropic filtering level for this texture as a percentage (0.0 - 1.0)
     */
    public float getAnisotropicFilterPercent() {
        return _anisotropicFilterPercent;
    }

    /**
     * @param percent
     *            the anisotropic filtering level for this texture as a percentage (0.0 - 1.0)
     */
    public void setAnisotropicFilterPercent(float percent) {
        if (percent > 1.0f) {
            percent = 1.0f;
        } else if (percent < 0.0f) {
            percent = 0.0f;
        }
        _anisotropicFilterPercent = percent;
    }

    /**
     * @return the lodBias for this texture
     */
    public float getLodBias() {
        return _lodBias;
    }

    /**
     * @param bias
     *            the lod bias for this texture. The default is 0.
     */
    public void setLodBias(final float bias) {
        _lodBias = bias;
    }

    public void setTextureKey(final TextureKey tkey) {
        _key = tkey;
    }

    public TextureKey getTextureKey() {
        return _key;
    }

    public void setTextureStoreFormat(final TextureStoreFormat storeFormat) {
        _storeFormat = storeFormat;
    }

    public void setRenderedTexturePixelDataType(final PixelDataType type) {
        _rttPixelDataType = type;
    }

    public PixelDataType getRenderedTexturePixelDataType() {
        return _rttPixelDataType;
    }

    public TextureStoreFormat getTextureStoreFormat() {
        return _storeFormat;
    }

    public boolean isStoreImage() {
        return _storeImage;
    }

    public void setStoreImage(final boolean store) {
        _storeImage = store;
    }

    public boolean hasBorder() {
        return _hasBorder;
    }

    public void setHasBorder(final boolean hasBorder) {
        _hasBorder = hasBorder;
    }

    /**
     * Get the depth texture compare function
     *
     * @return The depth texture compare function
     */
    public DepthTextureCompareFunc getDepthCompareFunc() {
        return _depthCompareFunc;
    }

    /**
     * Set the depth texture compare function
     *
     * param depthCompareFunc The depth texture compare function
     */
    public void setDepthCompareFunc(final DepthTextureCompareFunc depthCompareFunc) {
        _depthCompareFunc = depthCompareFunc;
    }

    /**
     * Get the depth texture compare mode
     *
     * @return The depth texture compare mode
     */
    public DepthTextureCompareMode getDepthCompareMode() {
        return _depthCompareMode;
    }

    /**
     * Set the depth texture compare mode
     *
     * @param depthCompareMode
     *            The depth texture compare mode
     */
    public void setDepthCompareMode(final DepthTextureCompareMode depthCompareMode) {
        _depthCompareMode = depthCompareMode;
    }

    public void setDirty() {
        if (_key != null) {
            _key.setDirty();
        }
    }

    public boolean isDirty(final Object glContext) {
        return _key.isDirty(glContext);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Texture)) {
            return false;
        }

        final Texture that = (Texture) other;
        if (getImage() != null && !getImage().equals(that.getImage())) {
            return false;
        }
        if (getImage() == null && that.getImage() != null) {
            return false;
        }
        if (getAnisotropicFilterPercent() != that.getAnisotropicFilterPercent()) {
            return false;
        }
        if (getMagnificationFilter() != that.getMagnificationFilter()) {
            return false;
        }
        if (getMinificationFilter() != that.getMinificationFilter()) {
            return false;
        }
        if (!_borderColor.equals(that._borderColor)) {
            return false;
        }

        return true;
    }

    public abstract Texture createSimpleClone();

    /**
     * Retrieve a basic clone of this Texture (ie, clone everything but the image data, which is shared)
     *
     * @return Texture
     */
    public Texture createSimpleClone(final Texture rVal) {
        rVal.setAnisotropicFilterPercent(_anisotropicFilterPercent);
        rVal.setBorderColor(_borderColor);
        rVal.setDepthCompareFunc(_depthCompareFunc);
        rVal.setDepthCompareMode(_depthCompareMode);
        rVal.setHasBorder(_hasBorder);
        rVal.setTextureStoreFormat(_storeFormat);
        rVal.setRenderedTexturePixelDataType(_rttPixelDataType);
        rVal.setImage(_image); // NOT CLONED.
        rVal.setLodBias(_lodBias);
        rVal.setMinificationFilter(_minificationFilter);
        rVal.setMagnificationFilter(_magnificationFilter);
        rVal.setStoreImage(_storeImage);
        rVal.setTextureMatrix(_texMatrix);
        if (getTextureKey() != null) {
            rVal.setTextureKey(getTextureKey());
        }
        return rVal;
    }

    public ReadOnlyMatrix4 getTextureMatrix() {
        return _texMatrix;
    }

    public void setTextureMatrix(final ReadOnlyMatrix4 matrix) {
        _texMatrix.set(matrix);
    }

    public void write(final OutputCapsule capsule) throws IOException {
        if (_storeImage) {
            capsule.write(_image, "image", null);
        }
        capsule.write(_borderColor, "borderColor", new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA));
        capsule.write(_texMatrix, "texMatrix", new Matrix4(Matrix4.IDENTITY));
        capsule.write(_hasBorder, "hasBorder", false);
        capsule.write(_anisotropicFilterPercent, "anisotropicFilterPercent", 0.0f);
        capsule.write(_lodBias, "lodBias", 0.0f);
        capsule.write(_minificationFilter, "minificationFilter", MinificationFilter.NearestNeighborNoMipMaps);
        capsule.write(_magnificationFilter, "magnificationFilter", MagnificationFilter.Bilinear);
        capsule.write(_storeFormat, "storeFormat", TextureStoreFormat.RGBA8);
        capsule.write(_rttPixelDataType, "rttPixelDataType", PixelDataType.UnsignedByte);
        capsule.write(_key, "textureKey", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        _minificationFilter = capsule.readEnum("minificationFilter", MinificationFilter.class,
                MinificationFilter.NearestNeighborNoMipMaps);
        _image = (Image) capsule.readSavable("image", null);

        // pull our key, if exists
        final TextureKey key = (TextureKey) capsule.readSavable("textureKey", null);
        if (key != null) {
            _key = TextureKey.getKey(key.getSource(), key.isFlipped(), key.getFormat(), key.getId(),
                    key.getMinificationFilter());
        } else {
            // none set, so pop in a generated key
            _key = TextureKey.getRTTKey(_minificationFilter);
        }

        // pull texture image from resource, if possible.
        if (_image == null && _key != null && _key.getSource() != null) {
            TextureManager.loadFromKey(_key, null, this);
        }

        _borderColor.set((ColorRGBA) capsule.readSavable("borderColor", new ColorRGBA(ColorRGBA.BLACK_NO_ALPHA)));
        _texMatrix.set((Matrix4) capsule.readSavable("texMatrix", new Matrix4(Matrix4.IDENTITY)));
        _hasBorder = capsule.readBoolean("hasBorder", false);
        _anisotropicFilterPercent = capsule.readFloat("anisotropicFilterPercent", 0.0f);
        _lodBias = capsule.readFloat("lodBias", 0.0f);
        _magnificationFilter = capsule.readEnum("magnificationFilter", MagnificationFilter.class,
                MagnificationFilter.Bilinear);
        _storeFormat = capsule.readEnum("storeFormat", TextureStoreFormat.class, TextureStoreFormat.RGBA8);
        _rttPixelDataType = capsule.readEnum("rttPixelDataType", PixelDataType.class, PixelDataType.UnsignedByte);
    }

    public Class<? extends Texture> getClassTag() {
        return this.getClass();
    }

    public int getTextureBaseLevel() {
        return _textureBaseLevel;
    }

    public void setTextureBaseLevel(final int textureBaseLevel) {
        _textureBaseLevel = textureBaseLevel;
    }

    public int getTextureMaxLevel() {
        return _textureMaxLevel;
    }

    public void setTextureMaxLevel(final int textureMaxLevel) {
        _textureMaxLevel = textureMaxLevel;
    }
}
