package com.example.artem52

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class RubikCube3D(context: Context) {
    // Константы
    private val CUBE_SIZE = 0.3f
    private val GAP = 0.02f

    // Матрицы
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)

    // Шейдеры
    private var shaderProgram: Int = 0
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    // Кубики
    private val cubes = Array(3) { x ->
        Array(3) { y ->
            Array(3) { z ->
                val visibleFaces = booleanArrayOf(
                    z == 2,  // FRONT
                    z == 0,  // BACK
                    x == 0,  // LEFT
                    x == 2,  // RIGHT
                    y == 2,  // UP
                    y == 0   // DOWN
                )
                Cube(CUBE_SIZE, visibleFaces).apply {
                    setPosition(
                        (x - 1) * (CUBE_SIZE + GAP),
                        (y - 1) * (CUBE_SIZE + GAP),
                        (z - 1) * (CUBE_SIZE + GAP)
                    )
                    setDefaultColors()
                }
            }
        }
    }

    init {
        initShaders()
    }

    private fun initShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        shaderProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            checkGlError("Program linking")
        }
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, -4f,  // Camera position
            0f, 0f, 0f,   // Look at center
            0f, 1f, 0f    // Up vector
        )
    }

    fun draw(viewProjectionMatrix: FloatArray) {  // Добавляем параметр
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(shaderProgram)

        val time = System.currentTimeMillis() % 10000L
        Matrix.setRotateM(rotationMatrix, 0, time * 0.09f, 0.5f, 1f, 0f)

        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val colorHandle = GLES20.glGetUniformLocation(shaderProgram, "vColor")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")

        for (x in 0..2) {
            for (y in 0..2) {
                for (z in 0..2) {
                    val cube = cubes[x][y][z]

                    // Рассчитываем MVP матрицу
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, cube.posX, cube.posY, cube.posZ)
                    Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0)
                    Matrix.multiplyMM(mvpMatrix, 0, rotationMatrix, 0, mvpMatrix, 0)

                    // Рисуем кубик
                    cube.draw(mvpMatrix, positionHandle, colorHandle, mvpMatrixHandle)
                }
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            checkGlError("Shader compilation")
        }
    }

    private fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op failed: GL error 0x${Integer.toHexString(error)}")
        }
    }

    inner class Cube(
        private val size: Float,
        private val visibleFaces: BooleanArray
    ) {
        var posX: Float = 0f
        var posY: Float = 0f
        var posZ: Float = 0f
        fun setPosition(x: Float, y: Float, z: Float) {
            posX = x
            posY = y
            posZ = z
        }
        private val vertexBuffer: FloatBuffer
        private val indexBuffer: ShortBuffer
        private val colors = Array(6) { FloatArray(4) }

        init {
            // Вершины куба (8 вершин)
            val vertices = floatArrayOf(
                // Нижний слой
                -size, -size, size,   // 0 перед-лев-низ
                size, -size, size,    // 1 перед-прав-низ
                size, -size, -size,   // 2 зад-прав-низ
                -size, -size, -size,  // 3 зад-лев-низ

                // Верхний слой
                -size, size, size,    // 4 перед-лев-верх
                size, size, size,     // 5 перед-прав-верх
                size, size, -size,    // 6 зад-прав-верх
                -size, size, -size    // 7 зад-лев-верх
            )

            // Индексы для 12 треугольников (6 граней)
            val indices = shortArrayOf(
                // Передняя грань
                4, 5, 1, 4, 1, 0,
                // Задняя грань
                6, 7, 3, 6, 3, 2,
                // Левая грань
                7, 4, 0, 7, 0, 3,
                // Правая грань
                5, 6, 2, 5, 2, 1,
                // Верхняя грань
                7, 6, 5, 7, 5, 4,
                // Нижняя грань
                0, 1, 2, 0, 2, 3
            )
            vertexBuffer = ByteBuffer
                .allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(vertices).position(0) }

            indexBuffer = ByteBuffer
                .allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .apply { put(indices).position(0) }
        }

        fun setDefaultColors() {
            colors[0] = floatArrayOf(0f, 1f, 0f, 1f)    // Зеленый (перед)
            colors[1] = floatArrayOf(0f, 0f, 1f, 1f)    // Синий (зад)
            colors[2] = floatArrayOf(1f, 0.5f, 0f, 1f)  // Оранжевый (лево)
            colors[3] = floatArrayOf(1f, 0f, 0f, 1f)    // Красный (право)
            colors[4] = floatArrayOf(1f, 1f, 1f, 1f)    // Белый (верх)
            colors[5] = floatArrayOf(1f, 1f, 0f, 1f)    // Желтый (низ)
        }

        fun draw(
            mvpMatrix: FloatArray,
            positionHandle: Int,
            colorHandle: Int,
            mvpMatrixHandle: Int
        ) {
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(positionHandle)
            vertexBuffer.position(0)
            GLES20.glVertexAttribPointer(
                positionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer
            )

            // Рисуем только видимые грани
            for (face in 0..5) {
                if (visibleFaces[face]) {
                    GLES20.glUniform4fv(colorHandle, 1, colors[face], 0)
                    GLES20.glDrawElements(
                        GLES20.GL_TRIANGLES,
                        6,
                        GLES20.GL_UNSIGNED_SHORT,
                        indexBuffer
                    )
                }
            }

            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }
}