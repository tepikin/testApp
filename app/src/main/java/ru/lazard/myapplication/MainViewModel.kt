package ru.lazard.myapplication

import android.graphics.Path
import android.graphics.PathMeasure
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel


class MainViewModel : ViewModel() {
    companion object {
        private val ANIMATION_DURATION = 3000
    }


    val position = floatArrayOf(0.06f, 0.07f)

    private var isAliveAnimation = false
    private var lasDrawTime: Long = -1;
    private var progress = 0f

    //Path for move sprite
    private val pathMeasure = PathMeasure(Path().apply {
        moveTo(0.06f, 0.07f)
        lineTo(0.84f, 0.06f)
        lineTo(0.07f, 0.87f)
        lineTo(0.77f, 0.87f)
    }, false)


    fun switch() {
        synchronized(this) {
            isAliveAnimation = !isAliveAnimation
        }
    }

    fun onSurfaceChanged() {
        synchronized(this) {
            lasDrawTime = -1

            //For skip rotation animation - temporary stop.
            if (isAliveAnimation) {isAliveAnimation = false;Handler(Looper.getMainLooper()).postDelayed(Runnable {isAliveAnimation = true}, 300)}
        }
    }

    fun doStep() {
        synchronized(this) {
            val currentTime = System.currentTimeMillis()
            if (lasDrawTime < 0) lasDrawTime = currentTime
            val timeDiff = currentTime - lasDrawTime
            if (isAliveAnimation) {
                progress += timeDiff.toFloat() / ANIMATION_DURATION;
                if (progress > 1f) progress = 0f;
                pathMeasure.getPosTan(pathMeasure.length * progress, position, null)
            }
            lasDrawTime = currentTime;

        }
    }


}