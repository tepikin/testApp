package ru.lazard.myapplication

import android.opengl.GLES20

class MainSprite :
    ru.lazard.myapplication.renderer.ShaderTwoInput(SHADER) {
    private var xLocation = 0
    private var yLocation = 0
    private var x = 0.5f
    private var y = 0.5f
    override fun onInit() {
        super.onInit()
        xLocation = GLES20.glGetUniformLocation(program, "x")
        yLocation = GLES20.glGetUniformLocation(program, "y")
    }

    override fun onInitialized() {
        super.onInitialized()
        setX(x)
        setY(y)
    }

    fun setX(x: Float) {
        this.x = x
        setFloat(xLocation, this.x)
    }

    fun setY(y: Float) {
        this.y = y
        setFloat(yLocation, this.y)
    }

    fun setPosition(array: FloatArray) {
        setX(array[0])
        setY(array[1])
    }

    companion object {
        private const val SHADER = """
            varying highp vec2 textureCoordinate;
            varying highp vec2 textureCoordinate2;
            
            uniform sampler2D inputImageTexture;
            uniform sampler2D inputImageTexture2;
                                
            uniform lowp float x;
            uniform lowp float y;
            
            void main(){
                highp vec2 scaledPoint = vec2(textureCoordinate2);
                scaledPoint=scaledPoint-vec2(x,y);
                scaledPoint.x=scaledPoint.x*4.0/5.0;
                scaledPoint=scaledPoint*10.0;
                
                if (scaledPoint.x<0.0||scaledPoint.x>1.0||scaledPoint.y<0.0||scaledPoint.y>1.0){
                    gl_FragColor = texture2D(inputImageTexture,  textureCoordinate);
                }else{
                    gl_FragColor = texture2D(inputImageTexture2,  scaledPoint);
                }
            }
            """
    }

}