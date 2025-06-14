package com.example.artem52

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.content.Context
import android.os.SystemClock
import android.util.Log

import java.util.Arrays
import java.util.HashMap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.random.Random
import android.os.Handler
import android.os.Looper
import kotlin.math.abs




class RotatingCubeRenderer : GLSurfaceView.Renderer {
    val cubes = mutableListOf<Cube>() // Делаем cubes публичным
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var cameraPosition = floatArrayOf(5f, 5f, 14f)
    private var shaderProgram = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0
    private var animating = false
    private var currentAnimation: AnimationData? = null
    private var animationProgress = 0f
    private var lastFrameTime = 0L
    private val targetFPS = 60
    private val frameTime = 1000
    private val animationDuration = 2000 // milliseconds
    private var requestRender: (() -> Unit)? = null
    private var currentAngle = 0f
    private var isSolving = false
    private var baseRotationAngle = 0f
    private lateinit var rubikManager: RubikCubeManager
    // Функция для изменения цвета внешних граней
    fun setOuterFacesColor(r: Float, g: Float, b: Float, a: Float) {
        cubes.forEach { cube ->
            // Проверяем, является ли куб внешним (x, y или z == ±1)
            if (abs(cube.x) == 1f || abs(cube.y) == 1f || abs(cube.z) == 1f) {
                cube.setColor(r, g, b, a)
            }
        }
    }
    data class AnimationData(
        val axis: Int,
        val layer: Int,
        val targetAngle: Float,
        var currentAngle: Float = 0f, // Добавляем currentAngle
        val startTime: Long,
        val cubesToRotate: List<Cube>,
        val callback: () -> Unit // Добавляем callback параметр
    )
    fun setRenderRequestCallback(callback: () -> Unit) {
        this.requestRender = callback
    }
    private val vertexShader = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec4 vColor;
        varying vec4 fColor;
        void main() {
            fColor = vColor;
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        varying vec4 fColor;
        void main() {
            gl_FragColor = fColor;
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Фиолетовый фон
        GLES20.glClearColor(0.5f, 0.0f, 0.5f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Создаем 27 кубов (3x3x3)
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val cube = Cube()
                    cube.setPosition(x.toFloat(), y.toFloat(), z.toFloat())
                    cube.setColor(0f, 0f, 0f, 1f) // Черный цвет
                    cubes.add(cube)
                }
            }
        }

        // Инициализация шейдеров
        val vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)

        shaderProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShaderId)
            GLES20.glAttachShader(it, fragmentShaderId)
            GLES20.glLinkProgram(it)
        }

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        rubikManager = RubikCubeManager(cubes)
        rubikManager.initializeColors()
    }
    fun resetAllCubes() {
        cubes.forEach { it.resetFaces() }
    }
    fun applyMove(move: String) {
        rubikManager.applyMove(move)
        requestRender?.invoke()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 20f)
        setCameraPosition(cameraPosition) // Инициализируем viewMatrix
    }
    fun setCameraPosition(position: FloatArray) {
        System.arraycopy(position, 0, cameraPosition, 0, 3)
        Matrix.setLookAtM(viewMatrix, 0,
            cameraPosition[0], cameraPosition[1], cameraPosition[2],
            0f, 0f, 0f,
            0f, 1f, 0f)
    }
    fun startAnimation(axis: Int, layer: Int, angle: Float, callback: () -> Unit) {
        isSolving = true // Останавливаем базовое вращение

        val cubesToRotate = cubes.filter { cube ->
            when (axis) {
                0 -> cube.x == layer.toFloat() // X axis (L/R)
                1 -> cube.y == layer.toFloat() // Y axis (U/D)
                2 -> cube.z == layer.toFloat() // Z axis (F/B)
                else -> false
            }
        }

        animating = true
        currentAnimation = AnimationData(
            axis = axis,
            layer = layer,
            targetAngle = angle,
            startTime = SystemClock.uptimeMillis(),
            cubesToRotate = cubesToRotate,
            callback = {
                applyMove(when {
                    axis == 0 && layer == -1 -> "L${if (angle < 0) "'" else ""}"
                    axis == 0 && layer == 1 -> "R${if (angle < 0) "'" else ""}"
                    axis == 1 && layer == -1 -> "D${if (angle < 0) "'" else ""}"
                    axis == 1 && layer == 1 -> "U${if (angle < 0) "'" else ""}"
                    axis == 2 && layer == -1 -> "B${if (angle < 0) "'" else ""}"
                    axis == 2 && layer == 1 -> "F${if (angle < 0) "'" else ""}"
                    else -> ""
                })
                callback()
            }
        )

        requestRender?.invoke()
    }
    private fun applyMoveToCubes(axis: Int, layer: Int, angle: Float, cubesToRotate: List<Cube>) {
        val rotationAngle = if (abs(angle) == 180f) angle/2 else angle

        // Вращаем позиции кубиков
        cubesToRotate.forEach { cube ->
            val (x, y, z) = when (axis) {
                // X axis rotation (L/R)
                0 -> Triple(
                    cube.x,
                    if (rotationAngle > 0) -cube.z else cube.z,
                    if (rotationAngle > 0) cube.y else -cube.y
                )
                // Y axis rotation (U/D)
                1 -> Triple(
                    if (rotationAngle > 0) cube.z else -cube.z,
                    cube.y,
                    if (rotationAngle > 0) -cube.x else cube.x
                )
                // Z axis rotation (F/B)
                else -> Triple(
                    if (rotationAngle > 0) -cube.y else cube.y,
                    if (rotationAngle > 0) cube.x else -cube.x,
                    cube.z
                )
            }
            cube.setPosition(x.toFloat(), y.toFloat(), z.toFloat())
        }

        // Обновляем цвета граней после поворота
        cubesToRotate.forEach { cube ->
            val newColors = Array(6) { floatArrayOf(0f, 0f, 0f, 1f) }
            val colors = cube.getFaceColors()

            when (axis) {
                // X axis rotation (L/R)
                0 -> {
                    newColors[0] = if (rotationAngle > 0) colors[4] else colors[5] // Front
                    newColors[1] = if (rotationAngle > 0) colors[5] else colors[4] // Back
                    newColors[2] = colors[2] // Left
                    newColors[3] = colors[3] // Right
                    newColors[4] = if (rotationAngle > 0) colors[1] else colors[0] // Up
                    newColors[5] = if (rotationAngle > 0) colors[0] else colors[1] // Down
                }
                // Y axis rotation (U/D)
                1 -> {
                    newColors[0] = if (rotationAngle > 0) colors[2] else colors[3] // Front
                    newColors[1] = if (rotationAngle > 0) colors[3] else colors[2] // Back
                    newColors[2] = if (rotationAngle > 0) colors[1] else colors[0] // Left
                    newColors[3] = if (rotationAngle > 0) colors[0] else colors[1] // Right
                    newColors[4] = colors[4] // Up
                    newColors[5] = colors[5] // Down
                }
                // Z axis rotation (F/B)
                else -> {
                    newColors[0] = colors[0] // Front
                    newColors[1] = colors[1] // Back
                    newColors[2] = if (rotationAngle > 0) colors[5] else colors[4] // Left
                    newColors[3] = if (rotationAngle > 0) colors[4] else colors[5] // Right
                    newColors[4] = if (rotationAngle > 0) colors[2] else colors[3] // Up
                    newColors[5] = if (rotationAngle > 0) colors[3] else colors[2] // Down
                }
            }

            // Устанавливаем новые цвета
            for (i in 0..5) {
                cube.setFaceColor(i, newColors[i])
            }
        }
    }
    private fun updateAnimation() {
        currentAnimation?.let { anim ->
            val currentTime = SystemClock.uptimeMillis()
            val elapsed = currentTime - anim.startTime

            if (elapsed < animationDuration) {
                val progress = (elapsed.toFloat() / animationDuration).coerceIn(0f, 1f)
                anim.currentAngle = anim.targetAngle * progress
                requestRender?.invoke()
            } else {
                anim.currentAngle = anim.targetAngle
                applyMoveToCubes(anim.axis, anim.layer, anim.targetAngle, anim.cubesToRotate)

                animating = false
                currentAnimation = null
                anim.callback() // Вызываем callback
                requestRender?.invoke()
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        updateAnimation()

        GLES20.glUseProgram(shaderProgram)

        // Базовое вращение - только когда не решаем
        Matrix.setIdentityM(modelMatrix, 0)
        if (!isSolving) {
            val time = SystemClock.uptimeMillis() % 10000L
            baseRotationAngle = time * 0.03f
            Matrix.rotateM(modelMatrix, 0, baseRotationAngle, 0f, 1f, 0f)
        }

        // Отрисовка всех кубов
        cubes.forEach { cube ->
            val cubeModelMatrix = FloatArray(16).apply {
                Matrix.setIdentityM(this, 0)

                // Применяем анимацию стороны, если кубик в текущей анимации
                currentAnimation?.let { anim ->
                    if (anim.cubesToRotate.contains(cube)) {
                        // Вращение вокруг центральной оси всей стороны
                        when (anim.axis) {
                            0 -> { // X-ось (L/R)
                                Matrix.rotateM(this, 0, anim.currentAngle, 1f, 0f, 0f)
                            }
                            1 -> { // Y-ось (U/D)
                                Matrix.rotateM(this, 0, anim.currentAngle, 0f, 1f, 0f)
                            }
                            2 -> { // Z-ось (F/B)
                                Matrix.rotateM(this, 0, anim.currentAngle, 0f, 0f, 1f)
                            }
                        }
                    }
                }

                // Позиция кубика
                Matrix.translateM(this, 0, cube.x, cube.y, cube.z)
                Matrix.multiplyMM(this, 0, modelMatrix, 0, this, 0)
            }

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, cubeModelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            cube.draw(mvpMatrix, positionHandle, colorHandle, mvpMatrixHandle)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                throw RuntimeException("Shader compilation error: ${GLES20.glGetShaderInfoLog(shader)}")
            }
        }
    }
}

class Cube {
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val faceSegments = Array(6) { Array(9) { FloatArray(4) } }
    var x = 0f
    var y = 0f
    var z = 0f
    fun setSegmentColor(face: Int, segment: Int, color: FloatArray) {
        System.arraycopy(color, 0, faceSegments[face][segment], 0, 4)
        updateColorBuffer()
    }
    fun getSegmentColor(face: Int, segment: Int): FloatArray {
        return faceSegments[face][segment].copyOf()
    }
    companion object {
        private val vertices = floatArrayOf(
            -0.4f, -0.4f, 0.4f,  0.4f, -0.4f, 0.4f,
            0.4f, 0.4f, 0.4f,  -0.4f, 0.4f, 0.4f,
            -0.4f, -0.4f, -0.4f,  -0.4f, 0.4f, -0.4f,
            0.4f, 0.4f, -0.4f,  0.4f, -0.4f, -0.4f,
            -0.4f, -0.4f, -0.4f,  -0.4f, -0.4f, 0.4f,
            -0.4f, 0.4f, 0.4f,  -0.4f, 0.4f, -0.4f,
            0.4f, -0.4f, -0.4f,  0.4f, 0.4f, -0.4f,
            0.4f, 0.4f, 0.4f,  0.4f, -0.4f, 0.4f,
            -0.4f, 0.4f, -0.4f,  -0.4f, 0.4f, 0.4f,
            0.4f, 0.4f, 0.4f,  0.4f, 0.4f, -0.4f,
            -0.4f, -0.4f, -0.4f,  0.4f, -0.4f, -0.4f,
            0.4f, -0.4f, 0.4f,  -0.4f, -0.4f, 0.4f
        )

        private val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23
        )
    }

    init {
        vertexBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices).position(0) }

        // Изначально черный цвет
        val initialColor = FloatArray(24 * 4)
        colorBuffer = ByteBuffer
            .allocateDirect(initialColor.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(initialColor).position(0) }

        indexBuffer = ByteBuffer
            .allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply { put(indices).position(0) }
    }

    fun setPosition(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        val color = floatArrayOf(r, g, b, a)
        val colors = FloatArray(24 * 4)
        for (i in 0 until 24) {
            System.arraycopy(color, 0, colors, i * 4, 4)
        }
        colorBuffer.put(colors).position(0)
    }

    fun draw(mvpMatrix: FloatArray, positionHandle: Int, colorHandle: Int, mvpMatrixHandle: Int) {
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            0, vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(colorHandle)
        colorBuffer.position(0)
        GLES20.glVertexAttribPointer(
            colorHandle, 4,
            GLES20.GL_FLOAT, false,
            0, colorBuffer
        )

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indices.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    private val faceColors = Array(6) { floatArrayOf(0.2f, 0.2f, 0.2f, 1f) }

    fun setFaceColor(face: Int, color: FloatArray) {
        if (face in 0..5) {
            System.arraycopy(color, 0, faceColors[face], 0, 4)
            updateColorBuffer()
        }
    }

    fun resetFaces() {
        for (i in 0..5) {
            faceColors[i] = floatArrayOf(0.2f, 0.2f, 0.2f, 1f)
        }
        updateColorBuffer()
    }

    private fun updateColorBuffer() {
        val colors = FloatArray(24 * 4)

        // Порядок граней: front, back, left, right, up, down
        // Каждая грань состоит из 4 вершин (4 угла квадрата)

        // Front face (0)
        System.arraycopy(faceColors[0], 0, colors, 0, 4)
        System.arraycopy(faceColors[0], 0, colors, 4, 4)
        System.arraycopy(faceColors[0], 0, colors, 8, 4)
        System.arraycopy(faceColors[0], 0, colors, 12, 4)

        // Back face (1)
        System.arraycopy(faceColors[1], 0, colors, 16, 4)
        System.arraycopy(faceColors[1], 0, colors, 20, 4)
        System.arraycopy(faceColors[1], 0, colors, 24, 4)
        System.arraycopy(faceColors[1], 0, colors, 28, 4)

        // Left face (2)
        System.arraycopy(faceColors[2], 0, colors, 32, 4)
        System.arraycopy(faceColors[2], 0, colors, 36, 4)
        System.arraycopy(faceColors[2], 0, colors, 40, 4)
        System.arraycopy(faceColors[2], 0, colors, 44, 4)

        // Right face (3)
        System.arraycopy(faceColors[3], 0, colors, 48, 4)
        System.arraycopy(faceColors[3], 0, colors, 52, 4)
        System.arraycopy(faceColors[3], 0, colors, 56, 4)
        System.arraycopy(faceColors[3], 0, colors, 60, 4)

        // Up face (4)
        System.arraycopy(faceColors[4], 0, colors, 64, 4)
        System.arraycopy(faceColors[4], 0, colors, 68, 4)
        System.arraycopy(faceColors[4], 0, colors, 72, 4)
        System.arraycopy(faceColors[4], 0, colors, 76, 4)

        // Down face (5)
        System.arraycopy(faceColors[5], 0, colors, 80, 4)
        System.arraycopy(faceColors[5], 0, colors, 84, 4)
        System.arraycopy(faceColors[5], 0, colors, 88, 4)
        System.arraycopy(faceColors[5], 0, colors, 92, 4)

        colorBuffer.put(colors).position(0)
    }
    fun getFaceColors(): Array<FloatArray> {
        return faceColors.map { it.copyOf() }.toTypedArray()
    }
}
class RubikCubeManager(private val cubes: List<Cube>) {
    // Индексы граней
    companion object {
        const val FRONT = 0
        const val BACK = 1
        const val LEFT = 2
        const val RIGHT = 3
        const val UP = 4
        const val DOWN = 5

        // Схема нумерации сегментов:
        // 0 1 2
        // 3 4 5
        // 6 7 8
    }

    // Инициализация цветов кубика
    fun initializeColors() {
        cubes.forEach { cube ->
            // Для каждого куба определяем его положение и инициализируем соответствующие грани
            when {
                cube.z == 1f -> initFace(cube, FRONT, floatArrayOf(0f, 1f, 0f, 1f)) // Зеленый
                cube.z == -1f -> initFace(cube, BACK, floatArrayOf(0f, 0.5f, 0f, 1f)) // Темно-зеленый
                cube.x == -1f -> initFace(cube, LEFT, floatArrayOf(1f, 0.5f, 0f, 1f)) // Оранжевый
                cube.x == 1f -> initFace(cube, RIGHT, floatArrayOf(1f, 0f, 0f, 1f)) // Красный
                cube.y == 1f -> initFace(cube, UP, floatArrayOf(1f, 1f, 1f, 1f)) // Белый
                cube.y == -1f -> initFace(cube, DOWN, floatArrayOf(1f, 1f, 0f, 1f)) // Желтый
            }
        }
    }
    private fun initFace(cube: Cube, face: Int, color: FloatArray) {
        for (i in 0..8) {
            cube.setSegmentColor(face, i, color)
        }
    }
    // Функция для выполнения хода
    fun applyMove(move: String) {
        when {
            move.contains("F") -> rotateFront(move.contains("'"), move.contains("2"))
            move.contains("B") -> rotateBack(move.contains("'"), move.contains("2"))
            move.contains("L") -> rotateLeft(move.contains("'"), move.contains("2"))
            move.contains("R") -> rotateRight(move.contains("'"), move.contains("2"))
            move.contains("U") -> rotateUp(move.contains("'"), move.contains("2"))
            move.contains("D") -> rotateDown(move.contains("'"), move.contains("2"))
        }
    }
    private fun getCube(x: Int, y: Int, z: Int): Cube {
        return cubes.firstOrNull {
            it.x == x.toFloat() && it.y == y.toFloat() && it.z == z.toFloat()
        } ?: throw IllegalStateException("Cube not found at ($x, $y, $z)")
    }
    private fun updateFrontAdjacents(clockwise: Boolean) {
        // Получаем кубы соседних граней
        val upRow = listOf(getCube(-1, 1, 1), getCube(0, 1, 1), getCube(1, 1, 1))
        val rightCol = listOf(getCube(1, 1, 1), getCube(1, 0, 1), getCube(1, -1, 1))
        val downRow = listOf(getCube(1, -1, 1), getCube(0, -1, 1), getCube(-1, -1, 1))
        val leftCol = listOf(getCube(-1, -1, 1), getCube(-1, 0, 1), getCube(-1, 1, 1))

        // Сохраняем текущие цвета
        val tempUp = upRow.map { it.getSegmentColor(UP, 6) to it.getSegmentColor(UP, 7) to it.getSegmentColor(UP, 8) }
        val tempRight = rightCol.map { it.getSegmentColor(RIGHT, 0) to it.getSegmentColor(RIGHT, 3) to it.getSegmentColor(RIGHT, 6) }
        val tempDown = downRow.map { it.getSegmentColor(DOWN, 0) to it.getSegmentColor(DOWN, 1) to it.getSegmentColor(DOWN, 2) }
        val tempLeft = leftCol.map { it.getSegmentColor(LEFT, 2) to it.getSegmentColor(LEFT, 5) to it.getSegmentColor(LEFT, 8) }

        if (clockwise) {
            // Переносим цвета по часовой стрелке
            upRow.forEachIndexed { i, cube ->
                cube.setSegmentColor(UP, 6+i, tempLeft[2-i].second)
                cube.setSegmentColor(UP, 3+i, tempLeft[2-i].first)
                cube.setSegmentColor(UP, i, tempLeft[2-i].first)
            }
            // Аналогично для других граней
            ...
        } else {
            // Переносим цвета против часовой стрелки
            ...
        }
    }

    fun rotateFace(face: Int, clockwise: Boolean) {
        // 1. Получаем все кубы текущей грани
        val faceCubes = when(face) {
            FRONT -> cubes.filter { it.z == 1f }
            BACK -> cubes.filter { it.z == -1f }
            LEFT -> cubes.filter { it.x == -1f }
            RIGHT -> cubes.filter { it.x == 1f }
            UP -> cubes.filter { it.y == 1f }
            DOWN -> cubes.filter { it.y == -1f }
            else -> emptyList()
        }

        // 2. Вращаем позиции кубов
        faceCubes.forEach { cube ->
            val (x, y, z) = when(face) {
                FRONT, BACK -> if (clockwise) Triple(-cube.y, cube.x, cube.z)
                else Triple(cube.y, -cube.x, cube.z)
                LEFT, RIGHT -> if (clockwise) Triple(cube.x, -cube.z, cube.y)
                else Triple(cube.x, cube.z, -cube.y)
                UP, DOWN -> if (clockwise) Triple(cube.z, cube.y, -cube.x)
                else Triple(-cube.z, cube.y, cube.x)
                else -> Triple(cube.x, cube.y, cube.z)
            }
            cube.setPosition(x, y, z)
        }

        // 3. Обновляем цвета сегментов соседних граней
        updateAdjacentFaces(face, clockwise)
    }
    private fun updateAdjacentFaces(face: Int, clockwise: Boolean) {
        when(face) {
            FRONT -> updateFrontAdjacents(clockwise)
            // Реализовать аналогично для других граней

        }
    }
    private fun rotateBack(clockwise: Boolean, double: Boolean) {
        // Получаем кубы задней грани (z = -1)
        val backCubes = cubes.filter { it.z == -1f }

        // 1. Вращаем позиции кубиков
        backCubes.forEach { cube ->
            val (x, y) = if (clockwise) {
                Pair(cube.y, -cube.x)
            } else {
                Pair(-cube.y, cube.x)
            }
            cube.setPosition(x, y, cube.z)
        }

        if (double) {
            rotateBack(clockwise, false)
            rotateBack(clockwise, false)
            return
        }

        // Получаем кубы, грани которых нужно обновить
        val upRow = cubes.filter { it.y == 1f && it.z == -1f }.sortedBy { -it.x }
        val leftCol = cubes.filter { it.x == -1f && it.z == -1f }.sortedBy { it.y }
        val downRow = cubes.filter { it.y == -1f && it.z == -1f }.sortedBy { it.x }
        val rightCol = cubes.filter { it.x == 1f && it.z == -1f }.sortedBy { -it.y }

        val tempColors = upRow.map { it.getFaceColors()[UP]!!.copyOf() }

        if (clockwise) {
            upRow.forEachIndexed { index, cube ->
                cube.setFaceColor(UP, rightCol[index].getFaceColors()[RIGHT]!!)
            }
            leftCol.forEachIndexed { index, cube ->
                cube.setFaceColor(LEFT, upRow[index].getFaceColors()[UP]!!)
            }
            downRow.forEachIndexed { index, cube ->
                cube.setFaceColor(DOWN, leftCol.reversed()[index].getFaceColors()[LEFT]!!)
            }
            rightCol.forEachIndexed { index, cube ->
                cube.setFaceColor(RIGHT, downRow[index].getFaceColors()[DOWN]!!)
            }
        } else {
            upRow.forEachIndexed { index, cube ->
                cube.setFaceColor(UP, leftCol.reversed()[index].getFaceColors()[LEFT]!!)
            }
            leftCol.forEachIndexed { index, cube ->
                cube.setFaceColor(LEFT, downRow[index].getFaceColors()[DOWN]!!)
            }
            downRow.forEachIndexed { index, cube ->
                cube.setFaceColor(DOWN, rightCol.reversed()[index].getFaceColors()[RIGHT]!!)
            }
            rightCol.forEachIndexed { index, cube ->
                cube.setFaceColor(RIGHT, tempColors[index])
            }
        }
    }

    private fun rotateLeft(clockwise: Boolean, double: Boolean) {
        // Получаем кубы левой грани (x = -1)
        val leftCubes = cubes.filter { it.x == -1f }

        // 1. Вращаем позиции кубиков
        leftCubes.forEach { cube ->
            val (y, z) = if (clockwise) {
                Pair(-cube.z, cube.y)
            } else {
                Pair(cube.z, -cube.y)
            }
            cube.setPosition(cube.x, y, z)
        }

        if (double) {
            rotateLeft(clockwise, false)
            rotateLeft(clockwise, false)
            return
        }

        // Получаем кубы, грани которых нужно обновить
        val upCol = cubes.filter { it.x == -1f && it.y == 1f }.sortedBy { it.z }
        val frontCol = cubes.filter { it.x == -1f && it.z == 1f }.sortedBy { -it.y }
        val downCol = cubes.filter { it.x == -1f && it.y == -1f }.sortedBy { -it.z }
        val backCol = cubes.filter { it.x == -1f && it.z == -1f }.sortedBy { it.y }

        val tempColors = upCol.map { it.getFaceColors()[UP]!!.copyOf() }

        if (clockwise) {
            upCol.forEachIndexed { index, cube ->
                cube.setFaceColor(UP, backCol[index].getFaceColors()[BACK]!!)
            }
            frontCol.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, upCol[index].getFaceColors()[UP]!!)
            }
            downCol.forEachIndexed { index, cube ->
                cube.setFaceColor(DOWN, frontCol.reversed()[index].getFaceColors()[FRONT]!!)
            }
            backCol.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, downCol[index].getFaceColors()[DOWN]!!)
            }
        } else {
            upCol.forEachIndexed { index, cube ->
                cube.setFaceColor(UP, frontCol[index].getFaceColors()[FRONT]!!)
            }
            frontCol.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, downCol[index].getFaceColors()[DOWN]!!)
            }
            downCol.forEachIndexed { index, cube ->
                cube.setFaceColor(DOWN, backCol.reversed()[index].getFaceColors()[BACK]!!)
            }
            backCol.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, tempColors[index])
            }
        }
    }

    private fun rotateRight(clockwise: Boolean, double: Boolean) {
        // Получаем кубы правой грани (x = 1)
        val rightCubes = cubes.filter { it.x == 1f }

        // 1. Вращаем позиции кубиков
        rightCubes.forEach { cube ->
            val (y, z) = if (clockwise) {
                Pair(cube.z, -cube.y)
            } else {
                Pair(-cube.z, cube.y)
            }
            cube.setPosition(cube.x, y, z)
        }

        if (double) {
            rotateRight(clockwise, false)
            rotateRight(clockwise, false)
            return
        }

        // Получаем кубы, грани которых нужно обновить
        val upCol = cubes.filter { it.x == 1f && it.y == 1f }.sortedBy { -it.z }
        val backCol = cubes.filter { it.x == 1f && it.z == -1f }.sortedBy { -it.y }
        val downCol = cubes.filter { it.x == 1f && it.y == -1f }.sortedBy { it.z }
        val frontCol = cubes.filter { it.x == 1f && it.z == 1f }.sortedBy { it.y }

        val tempColors = upCol.map { it.getFaceColors()[UP]!!.copyOf() }

        if (clockwise) {
            upCol.forEachIndexed { index, cube ->
                cube.setFaceColor(UP, frontCol[index].getFaceColors()[FRONT]!!)
            }
            backCol.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, upCol[index].getFaceColors()[UP]!!)
            }
            downCol.forEachIndexed { index, cube ->
                cube.setFaceColor(DOWN, backCol.reversed()[index].getFaceColors()[BACK]!!)
            }
            frontCol.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, downCol[index].getFaceColors()[DOWN]!!)
            }
        } else {
            upCol.forEachIndexed { index, cube ->
                cube.setFaceColor(UP, backCol.reversed()[index].getFaceColors()[BACK]!!)
            }
            backCol.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, downCol[index].getFaceColors()[DOWN]!!)
            }
            downCol.forEachIndexed { index, cube ->
                cube.setFaceColor(DOWN, frontCol.reversed()[index].getFaceColors()[FRONT]!!)
            }
            frontCol.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, tempColors[index])
            }
        }
    }

    private fun rotateUp(clockwise: Boolean, double: Boolean) {
        // Получаем кубы верхней грани (y = 1)
        val upCubes = cubes.filter { it.y == 1f }

        // 1. Вращаем позиции кубиков
        upCubes.forEach { cube ->
            val (x, z) = if (clockwise) {
                Pair(cube.z, -cube.x)
            } else {
                Pair(-cube.z, cube.x)
            }
            cube.setPosition(x, cube.y, z)
        }

        if (double) {
            rotateUp(clockwise, false)
            rotateUp(clockwise, false)
            return
        }

        // Получаем кубы, грани которых нужно обновить
        val frontRow = cubes.filter { it.y == 1f && it.z == 1f }.sortedBy { it.x }
        val rightRow = cubes.filter { it.y == 1f && it.x == 1f }.sortedBy { -it.z }
        val backRow = cubes.filter { it.y == 1f && it.z == -1f }.sortedBy { -it.x }
        val leftRow = cubes.filter { it.y == 1f && it.x == -1f }.sortedBy { it.z }

        val tempColors = frontRow.map { it.getFaceColors()[FRONT]!!.copyOf() }

        if (clockwise) {
            frontRow.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, rightRow[index].getFaceColors()[RIGHT]!!)
            }
            rightRow.forEachIndexed { index, cube ->
                cube.setFaceColor(RIGHT, backRow[index].getFaceColors()[BACK]!!)
            }
            backRow.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, leftRow[index].getFaceColors()[LEFT]!!)
            }
            leftRow.forEachIndexed { index, cube ->
                cube.setFaceColor(LEFT, tempColors[index])
            }
        } else {
            frontRow.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, leftRow[index].getFaceColors()[LEFT]!!)
            }
            rightRow.forEachIndexed { index, cube ->
                cube.setFaceColor(RIGHT, tempColors[index])
            }
            backRow.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, rightRow[index].getFaceColors()[RIGHT]!!)
            }
            leftRow.forEachIndexed { index, cube ->
                cube.setFaceColor(LEFT, backRow[index].getFaceColors()[BACK]!!)
            }
        }
    }

    private fun rotateDown(clockwise: Boolean, double: Boolean) {
        // Получаем кубы нижней грани (y = -1)
        val downCubes = cubes.filter { it.y == -1f }

        // 1. Вращаем позиции кубиков
        downCubes.forEach { cube ->
            val (x, z) = if (clockwise) {
                Pair(-cube.z, cube.x)
            } else {
                Pair(cube.z, -cube.x)
            }
            cube.setPosition(x, cube.y, z)
        }

        if (double) {
            rotateDown(clockwise, false)
            rotateDown(clockwise, false)
            return
        }

        // Получаем кубы, грани которых нужно обновить
        val frontRow = cubes.filter { it.y == -1f && it.z == 1f }.sortedBy { -it.x }
        val leftRow = cubes.filter { it.y == -1f && it.x == -1f }.sortedBy { -it.z }
        val backRow = cubes.filter { it.y == -1f && it.z == -1f }.sortedBy { it.x }
        val rightRow = cubes.filter { it.y == -1f && it.x == 1f }.sortedBy { it.z }

        val tempColors = frontRow.map { it.getFaceColors()[FRONT]!!.copyOf() }

        if (clockwise) {
            frontRow.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, leftRow[index].getFaceColors()[LEFT]!!)
            }
            leftRow.forEachIndexed { index, cube ->
                cube.setFaceColor(LEFT, backRow[index].getFaceColors()[BACK]!!)
            }
            backRow.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, rightRow[index].getFaceColors()[RIGHT]!!)
            }
            rightRow.forEachIndexed { index, cube ->
                cube.setFaceColor(RIGHT, tempColors[index])
            }
        } else {
            frontRow.forEachIndexed { index, cube ->
                cube.setFaceColor(FRONT, rightRow[index].getFaceColors()[RIGHT]!!)
            }
            leftRow.forEachIndexed { index, cube ->
                cube.setFaceColor(LEFT, tempColors[index])
            }
            backRow.forEachIndexed { index, cube ->
                cube.setFaceColor(BACK, leftRow[index].getFaceColors()[LEFT]!!)
            }
            rightRow.forEachIndexed { index, cube ->
                cube.setFaceColor(RIGHT, backRow[index].getFaceColors()[BACK]!!)
            }
        }
    }
}