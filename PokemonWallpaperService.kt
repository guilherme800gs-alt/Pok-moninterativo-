package com.guilherme.pokemoninterativo

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.MotionEvent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class PokemonWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = PokemonEngine()

    inner class PokemonEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var bitmap: Bitmap? = null
        private var visible = false
        private var width = 1
        private var height = 1
        private val particles = mutableListOf<Particle>()
        private var lastFrame = 0L

        private data class Particle(
            var x: Float, var y: Float, var vx: Float, var vy: Float,
            var life: Float, val maxLife: Float, val radius: Float
        )

        private val drawFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                draw()
                handler.postDelayed(this, 33L)
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(true)
            bitmap = BitmapFactory.decodeResource(resources, R.drawable.pokemon_wallpaper)
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(drawFrame)
            if (visible) handler.post(drawFrame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w.coerceAtLeast(1)
            height = h.coerceAtLeast(1)
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawFrame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            handler.removeCallbacks(drawFrame)
            bitmap?.recycle()
            bitmap = null
            super.onDestroy()
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                val count = if (event.action == MotionEvent.ACTION_DOWN) 26 else 8
                repeat(count) {
                    val angle = Random.nextFloat() * (Math.PI * 2).toFloat()
                    val speed = 1.0f + Random.nextFloat() * 4.2f
                    particles.add(
                        Particle(
                            event.x, event.y,
                            kotlin.math.cos(angle) * speed,
                            kotlin.math.sin(angle) * speed,
                            1f, 1f, 2f + Random.nextFloat() * 3.5f
                        )
                    )
                }
                if (particles.size > 240) particles.subList(0, particles.size - 240).clear()
                draw()
            }
            super.onTouchEvent(event)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.rgb(24, 19, 39))
                    bitmap?.let { image ->
                        val scale = max(width.toFloat() / image.width, height.toFloat() / image.height)
                        val dw = image.width * scale
                        val dh = image.height * scale
                        val left = (width - dw) / 2f
                        val top = (height - dh) / 2f
                        canvas.drawBitmap(image, null, RectF(left, top, left + dw, top + dh), paint)
                    }

                    val now = SystemClock.uptimeMillis()
                    val dt = if (lastFrame == 0L) 1f else ((now - lastFrame) / 33f).coerceIn(0.4f, 2.5f)
                    lastFrame = now

                    val iterator = particles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.vx *= 0.985f
                        p.vy = p.vy * 0.985f + 0.025f * dt
                        p.life -= 0.035f * dt
                        if (p.life <= 0f) {
                            iterator.remove()
                        } else {
                            paint.color = Color.argb((p.life * 255).toInt().coerceIn(0, 255), 255, 246, 170)
                            paint.setShadowLayer(12f, 0f, 0f, Color.rgb(180, 150, 255))
                            canvas.drawCircle(p.x, p.y, p.radius * p.life, paint)
                            paint.clearShadowLayer()
                            paint.color = Color.argb((p.life * 180).toInt().coerceIn(0, 180), 255, 255, 255)
                            canvas.drawRect(p.x - 5f, p.y - 0.7f, p.x + 5f, p.y + 0.7f, paint)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }
    }
}
