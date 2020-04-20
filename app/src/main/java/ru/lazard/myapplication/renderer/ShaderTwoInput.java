package ru.lazard.myapplication.renderer;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import ru.lazard.myapplication.renderer.util.OpenGlUtils;
import ru.lazard.myapplication.renderer.util.Rotation;
import ru.lazard.myapplication.renderer.util.TextureRotationUtil;

public class ShaderTwoInput extends ShaderBase {
    private static final String VERTEX_SHADER = "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "attribute vec4 inputTextureCoordinate2;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 textureCoordinate2;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    textureCoordinate2 = inputTextureCoordinate2.xy;\n" +
            "}";

    private int shaderSecondTextureCoordinateAttribute;
    private int shaderInputTextureUniform2;
    private int shaderSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    private ByteBuffer texture2CoordinatesBuffer;
    private Bitmap bitmap;

    public ShaderTwoInput(String fragmentShader) {
        this(VERTEX_SHADER, fragmentShader);
    }

    public ShaderTwoInput(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onInit() {
        super.onInit();

        shaderSecondTextureCoordinateAttribute = GLES20.glGetAttribLocation(getProgram(), "inputTextureCoordinate2");
        shaderInputTextureUniform2 = GLES20.glGetUniformLocation(getProgram(), "inputImageTexture2"); // This does assume a name of "inputImageTexture2" for second input texture in the fragment shader
        GLES20.glEnableVertexAttribArray(shaderSecondTextureCoordinateAttribute);
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        if (bitmap != null && !bitmap.isRecycled()) {
            setBitmap(bitmap);
        }
    }

    public void setBitmap(final Bitmap bitmap) {
        if (bitmap != null && bitmap.isRecycled()) {
            return;
        }
        this.bitmap = bitmap;
        if (this.bitmap == null) {
            return;
        }
        runOnDraw(new Runnable() {
            public void run() {
                if (shaderSourceTexture2 == OpenGlUtils.NO_TEXTURE) {
                    if (bitmap == null || bitmap.isRecycled()) {
                        return;
                    }
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
                    shaderSourceTexture2 = OpenGlUtils.loadTexture(bitmap, OpenGlUtils.NO_TEXTURE, false);
                }
            }
        });
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void recycleBitmap() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(1, new int[]{
                shaderSourceTexture2
        }, 0);
        shaderSourceTexture2 = OpenGlUtils.NO_TEXTURE;
    }

    @Override
    protected void onDrawArraysPre() {
        GLES20.glEnableVertexAttribArray(shaderSecondTextureCoordinateAttribute);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shaderSourceTexture2);
        GLES20.glUniform1i(shaderInputTextureUniform2, 3);

        texture2CoordinatesBuffer.position(0);
        GLES20.glVertexAttribPointer(shaderSecondTextureCoordinateAttribute, 2, GLES20.GL_FLOAT, false, 0, texture2CoordinatesBuffer);
    }

    public void setRotation(final Rotation rotation, final boolean flipHorizontal, final boolean flipVertical) {
        float[] buffer = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);

        ByteBuffer bBuffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        FloatBuffer fBuffer = bBuffer.asFloatBuffer();
        fBuffer.put(buffer);
        fBuffer.flip();

        texture2CoordinatesBuffer = bBuffer;
    }
}
