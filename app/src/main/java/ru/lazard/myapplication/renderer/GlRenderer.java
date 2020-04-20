package ru.lazard.myapplication.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.lazard.myapplication.renderer.util.OpenGlUtils;
import ru.lazard.myapplication.renderer.util.Rotation;
import ru.lazard.myapplication.renderer.util.TextureRotationUtil;

import static ru.lazard.myapplication.renderer.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

public class GlRenderer implements GLSurfaceView.Renderer {
    private static final int NO_IMAGE = -1;
    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private ShaderBase shader;

    public final Object surfaceChangedWaiter = new Object();

    private int glTextureId = NO_IMAGE;
    private SurfaceTexture surfaceTexture = null;
    private final FloatBuffer glCubeBuffer;
    private final FloatBuffer glTextureBuffer;


    private int outputWidth;
    private int outputHeight;
    private int imageWidth;
    private int imageHeight;

    private final Queue<Runnable> runOnDraw;
    private final Queue<Runnable> runOnDrawEnd;
    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GlHelper.ScaleType scaleType = GlHelper.ScaleType.CENTER_INSIDE;

    private float backgroundRed = 1f;
    private float backgroundGreen = 1f;
    private float backgroundBlue = 1f;

    public GlRenderer(final ShaderBase shader) {
        this.shader = shader;
        runOnDraw = new LinkedList<>();
        runOnDrawEnd = new LinkedList<>();

        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        if (onSurfaceChangeListener != null) {
            onSurfaceChangeListener.run();
        }
        GLES20.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        shader.ifNeedInit();

    }

    @Override
    public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
        if (onSurfaceChangeListener != null) {
            onSurfaceChangeListener.run();
        }
        outputWidth = width;
        outputHeight = height;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUseProgram(shader.getProgram());
        shader.onOutputSizeChanged(width, height);
        adjustImageScaling();
        synchronized (surfaceChangedWaiter) {
            surfaceChangedWaiter.notifyAll();
            if (onSurfaceChangeListener != null) {
                onSurfaceChangeListener.run();
            }
        }

    }


    @Override
    public void onDrawFrame(final GL10 gl) {
        if (onPreDrawFrame != null) {
            onPreDrawFrame.run();
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(runOnDraw);
        shader.onDraw(glTextureId, glCubeBuffer, glTextureBuffer);
        runAll(runOnDrawEnd);
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }
    }

    private Runnable onPreDrawFrame = null;
    private Runnable onSurfaceChangeListener = null;

    public void setOnPreDrawFrame(Runnable onPreDrawFrame) {
        this.onPreDrawFrame = onPreDrawFrame;
    }

    public void setOnSurfaceChangeListener(Runnable onSurfaceChangeListener) {
        this.onSurfaceChangeListener = onSurfaceChangeListener;
    }


    public void setBackgroundColor(float red, float green, float blue) {
        backgroundRed = red;
        backgroundGreen = green;
        backgroundBlue = blue;
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }


    public void setShader(final ShaderBase shader) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                final ShaderBase oldShader = GlRenderer.this.shader;
                GlRenderer.this.shader = shader;
                if (oldShader != null) {
                    oldShader.destroy();
                }
                GlRenderer.this.shader.ifNeedInit();
                GLES20.glUseProgram(GlRenderer.this.shader.getProgram());
                GlRenderer.this.shader.onOutputSizeChanged(outputWidth, outputHeight);
            }
        });
    }

    public void deleteImage() {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glDeleteTextures(1, new int[]{
                        glTextureId
                }, 0);
                glTextureId = NO_IMAGE;
            }
        });
    }

    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, true);
    }

    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        runOnDraw(new Runnable() {

            @Override
            public void run() {
                Bitmap resizedBitmap = null;
                if (bitmap.getWidth() % 2 == 1) {
                    resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1, bitmap.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas can = new Canvas(resizedBitmap);
                    can.drawARGB(0x00, 0x00, 0x00, 0x00);
                    can.drawBitmap(bitmap, 0, 0, null);
                }

                glTextureId = OpenGlUtils.loadTexture(
                        resizedBitmap != null ? resizedBitmap : bitmap, glTextureId, recycle);
                if (resizedBitmap != null) {
                    resizedBitmap.recycle();
                }
                imageWidth = bitmap.getWidth();
                imageHeight = bitmap.getHeight();
                adjustImageScaling();
            }
        });
    }

    public void setScaleType(GlHelper.ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    private void adjustImageScaling() {
        float outputWidth = this.outputWidth;
        float outputHeight = this.outputHeight;
        if (rotation == Rotation.ROTATION_270 || rotation == Rotation.ROTATION_90) {
            outputWidth = this.outputHeight;
            outputHeight = this.outputWidth;
        }

        float ratio1 = outputWidth / imageWidth;
        float ratio2 = outputHeight / imageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
        if (scaleType == GlHelper.ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        glCubeBuffer.clear();
        glCubeBuffer.put(cube).position(0);
        glTextureBuffer.clear();
        glTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    public void setRotation(final Rotation rotation) {
        this.rotation = rotation;
        adjustImageScaling();
    }

    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        setRotation(rotation);
    }


    protected void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (runOnDrawEnd) {
            runOnDrawEnd.add(runnable);
        }
    }
}
