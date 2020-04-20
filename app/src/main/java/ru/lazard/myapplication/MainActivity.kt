package ru.lazard.myapplication

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.lazard.myapplication.renderer.GlHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val model: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!GlHelper.supportsOpenGLES2(this)) {
            Toast.makeText(this,getString(R.string.message_gl_not_supported),Toast.LENGTH_LONG).show()
            return
        }

        setContentView(R.layout.activity_main)

        startButton.setOnClickListener { model.switch() }

        GlHelper().apply {
            setGLSurfaceView(glSurfaceView)
            setImage(R.drawable.bg.load)
            val sprite = MainSprite().apply { bitmap = R.drawable.sprite.load;setShader(this) }
            renderer.setOnPreDrawFrame {
                model.doStep()
                sprite.setX(model.position[0])
                sprite.setY(model.position[1])
            }
            renderer.setOnSurfaceChangeListener( model::onSurfaceChanged)
        }

    }

    override fun onResume() {
        super.onResume()
    }

    val Int.load get()= BitmapFactory.decodeResource(resources, this)

}


