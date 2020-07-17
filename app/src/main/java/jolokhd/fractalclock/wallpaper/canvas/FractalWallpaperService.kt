/*
    Copyright (C) 2020  Jannes Althoff

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>
*/
@file:Suppress("MayBeConstant")

package jolokhd.fractalclock.wallpaper.canvas

import android.app.WallpaperColors
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import jolokhd.fractalclock.wallpaper.ClockType
import jolokhd.fractalclock.wallpaper.Vector2f
import jolokhd.fractalclock.wallpaper.Vertex
import java.util.*
import kotlin.math.*


val ratioH: Float = (1.0f / 2.0f)
val ratioM: Float = sqrt(1.0f / 2.0f)
val ratioS: Float = sqrt(1.0f / 2.0f)
val ratio: Float = max(max(
    ratioH,
    ratioM
), ratioS
)
var rotH: Vector2f =
    Vector2f(0.0f, 0.0f)
var rotM: Vector2f =
    Vector2f(0.0f, 0.0f)
var rotS: Vector2f =
    Vector2f(0.0f, 0.0f)

class FractalWallpaperService: WallpaperService() {
    override fun onCreateEngine(): Engine {
        return FractalWallpaperEngine()
    }
    private inner class FractalWallpaperEngine : Engine() {
        //Settings
        private var useTick: Boolean = false
        private var drawClock: Boolean = true
        private var drawBranches: Boolean = true
        private var antiAliasing: Boolean = true
        private var everySecond: Boolean = false
        private var fps: Float = 60.0f
        private var clockType: ClockType = ClockType.HMS
        private var maxIters: Int = 12 // Should be 16
        private var clockFaceColor: Int = Color.argb(192, 255, 255, 255)
        private var backgroundColor: Int = Color.rgb(16, 16, 16)
        private var scalingFactor: Float = 0.25f
        //Internal Values
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunner = Runnable { draw() }
        private var visible: Boolean = true
        private var width: Int = 0
        private var height: Int = 0
        private var format: Int = 0
        private var fractalColor: Int = 0
        private val clockFaceArray1: Vector<Vertex> = Vector()
        private val clockFaceArray2: Vector<Vertex> = Vector()
        private var base = Vector2f(0f, 0f)
        private var hour = Vector2f(0f, 0f)
        private var minute = Vector2f(0f, 0f)
        private var second = Vector2f(0f, 0f)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ ->
                updatePreference(prefs)
            }

        init {
            updatePreference(PreferenceManager.getDefaultSharedPreferences(this@FractalWallpaperService))
            PreferenceManager.getDefaultSharedPreferences(this@FractalWallpaperService).registerOnSharedPreferenceChangeListener(listener)
        }

        //Called if Service Created
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setOffsetNotificationsEnabled(false)
            }
            handler.post(drawRunner)
        }

        //Called if Surface Created
        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.post(drawRunner)
            }
        }

        //Called if Surface Destroyed
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            this.visible = false
            handler.removeCallbacks(drawRunner)
        }

        //Called if Visibility Changed
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            handler.removeCallbacks(drawRunner)
            if (visible) {
                handler.post(drawRunner)
            }
        }



        private fun updatePreference(preference: SharedPreferences) {
            drawClock = preference.getBoolean("draw_clock", true)
            useTick = preference.getBoolean("use_tick", false)
            drawBranches = preference.getBoolean("draw_branches", true)
            antiAliasing = preference.getBoolean("use_antialiasing", true)
            everySecond = preference.getBoolean("every_second", false)
            maxIters = preference.getInt("max_iters", 12)
            fps = preference.getInt("fps", 60).toFloat()
            scalingFactor = preference.getInt("scaling_factor", 50).toFloat() / 100f
            clockFaceColor = preference.getInt("color_clock", 0)
            backgroundColor = preference.getInt("color_background", 0)
            clockType = when(preference.getString("clock_type", "1")){
                "1" -> {
                    ClockType.HMS
                }
                "2" -> {
                    ClockType.HM
                }
                "3" -> {
                    ClockType.MS
                }
                else -> {
                    ClockType.HMS
                }
            }
            //Update the clock face
            clockFaceArray1.clear()
            clockFaceArray2.clear()
            for (i in 0 until 60) {
                val pt = Vector2f(
                    this.width.toFloat() * scalingFactor * 0.5f,
                    this.height.toFloat() * scalingFactor * 0.5f
                )
                val startMag: Float =
                    min(this.width, this.height) * scalingFactor * 0.5f * (1.0f - ratio) / ratio
                val ang: Float = i.toFloat() * 2.0f * PI.toFloat() / 60.0f
                val v =
                    Vector2f(cos(ang), sin(ang))
                val isHour: Boolean = (i % 5 == 0)
                val innerRad: Float = if (isHour) 0.9f else 0.95f
                if (isHour) {
                    clockFaceArray1.addElement(
                        Vertex(
                            pt + v * startMag * innerRad,
                            clockFaceColor
                        )
                    )
                    clockFaceArray1.addElement(
                        Vertex(
                            pt + v * startMag * 1.0f,
                            clockFaceColor
                        )
                    )
                } else {
                    clockFaceArray2.addElement(
                        Vertex(
                            pt + v * startMag * innerRad,
                            clockFaceColor
                        )
                    )
                    clockFaceArray2.addElement(
                        Vertex(
                            pt + v * startMag * 1.0f,
                            clockFaceColor
                        )
                    )
                }
            }
        }

        //Called if Surface Changed
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int){
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            this.format = format
            //Update the clock face
            clockFaceArray1.clear()
            clockFaceArray2.clear()
            for (i in 0 until 60) {
                val pt = Vector2f(
                    this.width.toFloat() * scalingFactor * 0.5f,
                    this.height.toFloat() * scalingFactor * 0.5f
                )
                val startMag: Float = min(this.width, this.height) * scalingFactor * 0.5f * (1.0f - ratio) / ratio
                val ang: Float = i.toFloat() * 2.0f * PI.toFloat() / 60.0f
                val v =
                    Vector2f(cos(ang), sin(ang))
                val isHour: Boolean = (i % 5 == 0)
                val innerRad: Float = if (isHour) 0.9f else 0.95f
                if(isHour) {
                    clockFaceArray1.addElement(
                        Vertex(
                            pt + v * startMag * innerRad,
                            clockFaceColor
                        )
                    )
                    clockFaceArray1.addElement(
                        Vertex(
                            pt + v * startMag * 1.0f,
                            clockFaceColor
                        )
                    )
                } else {
                    clockFaceArray2.addElement(
                        Vertex(
                            pt + v * startMag * innerRad,
                            clockFaceColor
                        )
                    )
                    clockFaceArray2.addElement(
                        Vertex(
                            pt + v * startMag * 1.0f,
                            clockFaceColor
                        )
                    )
                }
            }

            handler.removeCallbacks(drawRunner)
            handler.post(drawRunner)
        }

        //Called if Wallpaper Colors Wanted
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            return WallpaperColors(Color.valueOf(fractalColor), Color.valueOf(clockFaceColor), Color.valueOf(backgroundColor))
        }

        //Draws the Wallpaper
        private fun draw(){
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                 canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch(e: Exception){}
            val cal = Calendar.getInstance()
            val lastTime: Long = cal.timeInMillis
            if (canvas != null) {
                //Calculate maximum iterations
                var iters: Int = maxIters
                if(clockType == ClockType.HMS){
                    iters = maxIters - 3
                }

                //Get the time
                if(everySecond){
                    cal.set(Calendar.MILLISECOND, 0)
                }
                val now: Long = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val passedL: Long = now - cal.timeInMillis
                var passedF: Float = passedL/1000.0f
                if(useTick){
                    val a = 30.0f
                    val b = 14.0f
                    val x : Float = passedF % 1.0f
                    val y : Float = 1.0f - cos(a * x) * exp(-b*x)
                    passedF = passedF - x + y
                }
                val seconds: Float = ((passedF % 60.0f) * 2.0f * PI.toFloat()) / 60.0f
                val minutes: Float = ((passedF % 3600.0f) * 2.0f * PI.toFloat()) / 3600.0f
                val hours: Float = ((passedF % 43200.0f) * 2.0f * PI.toFloat()) / 43200.0f
                val startMag: Float = min(this.width, this.height) * scalingFactor * 0.5f * (1.0f - ratio) / ratio

                //Update the clock
                rotH =
                    Vector2f(
                        cos(hours),
                        sin(hours)
                    )
                rotM =
                    Vector2f(
                        cos(minutes),
                        sin(minutes)
                    )
                rotS =
                    Vector2f(
                        cos(seconds),
                        sin(seconds)
                    )
                val pt = Vector2f(
                    this.width.toFloat() * scalingFactor * 0.5f,
                    this.height.toFloat() * scalingFactor * 0.5f
                )
                val dir =
                    Vector2f(0.0f, -startMag)

                //Update the colors
                val r1: Float = sin(passedF * 0.017f) * 0.5f + 0.5f
                val r2: Float = sin(passedF * 0.011f) * 0.5f + 0.5f
                val r3: Float = sin(passedF * 0.003f) * 0.5f + 0.5f
                val colorScheme = IntArray(maxIters)
                for (i in 0 until iters) {
                    val a: Float = i.toFloat() / (iters - 1).toFloat()
                    val h: Float = (r2 + 0.5f * a) % 1.0f
                    val s: Float = 0.5f + 0.5f * r3 - 0.5f*(1.0f - a)
                    val v: Float = 0.3f + 0.5f * r1
                    if (i == 0) {
                        val color: Int = hsvToColor(h, 1.0f, 1.0f)

                        colorScheme[i] = Color.argb(128, Color.red(color), Color.green(color), Color.blue(color))
                    } else if (i == iters - 1 && drawClock) {
                        colorScheme[i] = clockFaceColor
                    } else {
                        val color: Int =  hsvToColor(h, s, v)
                        colorScheme[i] = Color.argb(255, Color.red(color), Color.green(color), Color.blue(color))
                    }
                }

                //Clear the screen
                canvas.drawColor(backgroundColor, PorterDuff.Mode.SRC)
                //Update the fractal
                val lineArray: Vector<Vertex> = Vector()
                val pointArray: Vector<Vertex> = Vector()
                when (clockType) {
                    ClockType.HM -> {
                        fractalIterHM(pt, dir, iters - 1, lineArray, pointArray, colorScheme, true, canvas)
                    }
                    ClockType.HMS -> {
                        fractalIterHMS(pt, dir, iters - 1, lineArray, pointArray, colorScheme, true, canvas)
                    }
                    ClockType.MS -> {
                        fractalIterMS(pt, dir, iters - 1, lineArray, pointArray, colorScheme, true, canvas)
                    }
                }



                //Draw the final fractal in a brighter color
                val pointPaint = Paint(if (antiAliasing) Paint.ANTI_ALIAS_FLAG else 0)
                pointPaint.strokeWidth = 1.0f / scalingFactor
                for(i in pointArray){
                    pointPaint.color = i.color
                    canvas.drawPoint(i.position.x / scalingFactor, i.position.y / scalingFactor, pointPaint)
                }
                fractalColor = colorScheme[0]
                //Draw the clock
                if (drawClock) {
                    //Draw Lines
                    val clockPaint = Paint(if (antiAliasing) Paint.ANTI_ALIAS_FLAG else 0)
                    clockPaint.color = clockFaceColor
                    when(clockType){
                        ClockType.HMS -> {
                            clockPaint.strokeWidth = 5.0f
                            canvas.drawLine(base.x, base.y, hour.x, hour.y, clockPaint)
                            clockPaint.strokeWidth = 4.0f
                            canvas.drawLine(base.x, base.y, minute.x, minute.y, clockPaint)
                            clockPaint.strokeWidth = 2.0f
                            canvas.drawLine(base.x, base.y, second.x, second.y, clockPaint)
                        }
                        ClockType.MS -> {
                            clockPaint.strokeWidth = 4.0f
                            canvas.drawLine(base.x, base.y, minute.x, minute.y, clockPaint)
                            clockPaint.strokeWidth = 2.0f
                            canvas.drawLine(base.x, base.y, second.x, second.y, clockPaint)
                        }
                        ClockType.HM -> {
                            clockPaint.strokeWidth = 5.0f
                            canvas.drawLine(base.x, base.y, hour.x, hour.y, clockPaint)
                            clockPaint.strokeWidth = 4.0f
                            canvas.drawLine(base.x, base.y, minute.x, minute.y, clockPaint)
                        }
                    }

                    //Draw the clock face lines
                    clockPaint.color = clockFaceColor
                    clockPaint.strokeWidth = 4.0f
                    for(i in 0 until clockFaceArray1.size step 2){
                        canvas.drawLine(clockFaceArray1[i].position.x / scalingFactor, clockFaceArray1[i].position.y / scalingFactor, clockFaceArray1[i+1].position.x / scalingFactor, clockFaceArray1[i+1].position.y / scalingFactor, clockPaint)
                    }
                    clockPaint.strokeWidth = 2.0f
                    for(i in 0 until clockFaceArray2.size step 2){
                        canvas.drawLine(clockFaceArray2[i].position.x / scalingFactor, clockFaceArray2[i].position.y / scalingFactor, clockFaceArray2[i+1].position.x / scalingFactor, clockFaceArray2[i+1].position.y / scalingFactor, clockPaint)
                    }
                }
            }
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch(e: Exception){}
            }
            handler.removeCallbacks(drawRunner)
            if (visible) {
                val cal2 = Calendar.getInstance()
                if(fps <= 0) {
                    handler.post(drawRunner)
                } else {
                    if(everySecond){
                        handler.postDelayed(
                            drawRunner,
                            (1000.0f - (cal2.timeInMillis - lastTime)).toLong()
                        )
                    } else {
                        handler.postDelayed(
                            drawRunner,
                            (1000.0f / fps - (cal2.timeInMillis - lastTime)).toLong()
                        )
                    }
                }
            }
        }

        //Transforms HSV Color to Int
        private fun hsvToColor(h: Float, s: Float, v: Float): Int {
            val i: Int = (h * 6).toInt()
            val f: Float = h * 6 - i
            val p: Float = v * (1 - s)
            val q: Float = v * (1 - f * s)
            val t: Float = v * (1 - (1 - f) * s)

            //Special cases
            var r = 0.0f
            var g = 0.0f
            var b = 0.0f
            when(i % 6){
                0 -> {
                    r = v; g = t; b = p
                }
                1 -> {
                    r = q; g = v; b = p
                }
                2 -> {
                    r = p; g = v; b = t
                }
                3 -> {
                    r = p; g = q; b = v
                }
                4 -> {
                    r = t; g = p; b = v
                }
                5 -> {
                    r = v; g = p; b = q
                }
            }

            //Convert color range from 0-1 to 0-255

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Color.rgb(r, g, b)
            } else {
                Color.rgb((r*255).toInt(), (g*255).toInt(), (b*255).toInt())
            }
        }

        //Fractal Iterations for Hour Minute
        private fun fractalIterHM(
            pt: Vector2f,
            dir: Vector2f,
            depth: Int,
            lineArray: Vector<Vertex>,
            pointArray: Vector<Vertex>,
            colorScheme: IntArray,
            initialCall: Boolean,
            canvas: Canvas
        ) {
            val col: Int = colorScheme[depth]
            if (depth == 0) {
                pointArray.addElement(
                    Vertex(
                        pt,
                        col
                    )
                )
            } else {
                val dirM = Vector2f(
                    (dir.x * rotM.x - dir.y * rotM.y) * ratioM,
                    (dir.y * rotM.x + dir.x * rotM.y) * ratioM
                )
                val dirH = Vector2f(
                    (dir.x * rotH.x - dir.y * rotH.y) * ratioH,
                    (dir.y * rotH.x + dir.x * rotH.y) * ratioH
                )
                fractalIterHM(pt + dirM, dirM, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                fractalIterHM(pt + dirH, dirH, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                if(((!initialCall) or (!drawClock)) and drawBranches) {
                    val paint = Paint(if (antiAliasing) Paint.ANTI_ALIAS_FLAG else 0)
                    paint.color = col
                    paint.strokeWidth = 2.0f
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirM).x / scalingFactor, (pt + dirM).y / scalingFactor, paint)
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirH).x / scalingFactor, (pt + dirH).y / scalingFactor, paint)
                }
                if(initialCall and drawClock) {
                    base = Vector2f(
                        pt.x / scalingFactor,
                        pt.y / scalingFactor
                    )
                    hour = Vector2f(
                        (pt + dirH).x / scalingFactor,
                        (pt + dirH).y / scalingFactor
                    )
                    minute = Vector2f(
                        (pt + dirM).x / scalingFactor,
                        (pt + dirM).y / scalingFactor
                    )
                }
            }
        }

        //Fractal Iterations for Hour Minute Second
        private fun fractalIterHMS(
            pt: Vector2f,
            dir: Vector2f,
            depth: Int,
            lineArray: Vector<Vertex>,
            pointArray: Vector<Vertex>,
            colorScheme: IntArray,
            initialCall: Boolean,
            canvas: Canvas
        ) {
            val col: Int = colorScheme[depth]
            if (depth == 0) {
                pointArray.addElement(
                    Vertex(
                        pt,
                        col
                    )
                )
            } else {
                val dirS = Vector2f(
                    (dir.x * rotS.x - dir.y * rotS.y) * ratioS,
                    (dir.y * rotS.x + dir.x * rotS.y) * ratioS
                )
                val dirM = Vector2f(
                    (dir.x * rotM.x - dir.y * rotM.y) * ratioM,
                    (dir.y * rotM.x + dir.x * rotM.y) * ratioM
                )
                val dirH = Vector2f(
                    (dir.x * rotH.x - dir.y * rotH.y) * ratioH,
                    (dir.y * rotH.x + dir.x * rotH.y) * ratioH
                )
                fractalIterHMS(pt + dirS, dirS, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                fractalIterHMS(pt + dirM, dirM, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                fractalIterHMS(pt + dirH, dirH, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                if(((!initialCall) or (!drawClock)) and drawBranches) {
                    val paint = Paint(if (antiAliasing) Paint.ANTI_ALIAS_FLAG else 0)
                    paint.color = col
                    paint.strokeWidth = 2.0f
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirS).x / scalingFactor, (pt + dirS).y / scalingFactor, paint)
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirM).x / scalingFactor, (pt + dirM).y / scalingFactor, paint)
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirH).x / scalingFactor, (pt + dirH).y / scalingFactor, paint)
                }
                if(initialCall and drawClock) {
                    base = Vector2f(
                        pt.x / scalingFactor,
                        pt.y / scalingFactor
                    )
                    hour = Vector2f(
                        (pt + dirH).x / scalingFactor,
                        (pt + dirH).y / scalingFactor
                    )
                    minute = Vector2f(
                        (pt + dirM).x / scalingFactor,
                        (pt + dirM).y / scalingFactor
                    )
                    second = Vector2f(
                        (pt + dirS).x / scalingFactor,
                        (pt + dirS).y / scalingFactor
                    )
                }
            }
        }

        //Fractal Iterations for Minute Second
        private fun fractalIterMS(
            pt: Vector2f,
            dir: Vector2f,
            depth: Int,
            lineArray: Vector<Vertex>,
            pointArray: Vector<Vertex>,
            colorScheme: IntArray,
            initialCall: Boolean,
            canvas: Canvas
        ) {
            val col: Int = colorScheme[depth]
            if (depth == 0) {
                pointArray.addElement(
                    Vertex(
                        pt,
                        col
                    )
                )
            } else {
                val dirS = Vector2f(
                    (dir.x * rotS.x - dir.y * rotS.y) * ratioS,
                    (dir.y * rotS.x + dir.x * rotS.y) * ratioS
                )
                val dirM = Vector2f(
                    (dir.x * rotM.x - dir.y * rotM.y) * ratioM,
                    (dir.y * rotM.x + dir.x * rotM.y) * ratioM
                )
                fractalIterMS(pt + dirS, dirS, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                fractalIterMS(pt + dirM, dirM, depth - 1, lineArray, pointArray, colorScheme, false, canvas)
                if(((!initialCall) or !drawClock) and drawBranches) {
                    val paint = Paint(if (antiAliasing) Paint.ANTI_ALIAS_FLAG else 0)
                    paint.strokeWidth = 2.0f / scalingFactor
                    paint.color = col
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirS).x / scalingFactor, (pt + dirS).y / scalingFactor, paint)
                    canvas.drawLine(pt.x / scalingFactor,pt.y / scalingFactor, (pt + dirM).x / scalingFactor, (pt + dirM).y / scalingFactor, paint)
                }
                if(initialCall and drawClock){
                    base = Vector2f(
                        pt.x / scalingFactor,
                        pt.y / scalingFactor
                    )
                    minute = Vector2f(
                        (pt + dirM).x / scalingFactor,
                        (pt + dirM).y / scalingFactor
                    )
                    second = Vector2f(
                        (pt + dirS).x / scalingFactor,
                        (pt + dirS).y / scalingFactor
                    )
                }
            }
        }
    }
}