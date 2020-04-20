package ru.lazard.myapplication.renderer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;


public class GlHelper {

    public enum ScaleType {CENTER_INSIDE, CENTER_CROP}

    private final GlRenderer renderer;
    private GLSurfaceView glSurfaceView;
    private ShaderBase shader;

    public GlHelper() {
        shader = new ShaderBase();
        renderer = new GlRenderer(shader);
    }

    public GlRenderer getRenderer() {
        return renderer;
    }


    public static boolean supportsOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }


    public void setGLSurfaceView(final GLSurfaceView view) {
        glSurfaceView = view;
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glSurfaceView.requestRender();
    }


    public void setBackgroundColor(float red, float green, float blue) {
        renderer.setBackgroundColor(red, green, blue);
    }


    public void requestRender() {
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }


    public void setShader(final ShaderBase shader) {
        this.shader = shader;
        renderer.setShader(this.shader);
        requestRender();
    }


    public void setImage(final Bitmap bitmap) {
        renderer.setImageBitmap(bitmap, false);
        requestRender();
    }


    public void setScaleType(ScaleType scaleType) {
        renderer.setScaleType(scaleType);
        renderer.deleteImage();
        requestRender();
    }


}
