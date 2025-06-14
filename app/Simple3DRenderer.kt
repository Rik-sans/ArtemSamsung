

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Simple3DRenderer : GLSurfaceView.Renderer {
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

    // Координаты 3D пирамиды (4 треугольника)
    private val pyramidCoords = floatArrayOf(
        // Основание (2 треугольника)
        -0.5f, -0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.0f, 0.5f, 0.0f,  // Передняя грань
        -0.5f, -0.5f, 0.5f,   0.5f, -0.5f, 0.5f,   0.0f, 0.5f, 0.0f,  // Задняя грань
        -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f,   0.0f, 0.5f, 0.0f,  // Левая грань
        0.5f, -0.5f, -0.5f,   0.5f, -0.5f, 0.5f,   0.0f, 0.5f, 0.0f   // Правая грань
    )

    private val colors = arrayOf(
        floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f),  // Красный
        floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),  // Зеленый
        floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f),  // Синий
        floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)   // Желтый
    )

    // Матрицы
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)

    private var vertexBuffer: FloatBuffer
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    private var angle: Float = 0f

    init {
        // Инициализация буфера вершин
        val bb = ByteBuffer.allocateDirect(pyramidCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(pyramidCoords)
        vertexBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Компиляция шейдеров
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Создание OpenGL программы
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        // Получаем handles
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        // Соотношение сторон
        val ratio = width.toFloat() / height.toFloat()

        // Матрица проекции
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)

        // Матрица вида (камера)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, -3f,  // Позиция камеры (x,y,z)
            0f, 0f, 0f,   // Точка, на которую смотрим
            0f, 1f, 0f)   // Вектор "вверх"
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Вращение модели
        angle = (angle + 0.5f) % 360
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 1f, 0f)

        // Матрица модели
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)

        // Рассчитываем итоговую MVP матрицу
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Активируем программу
        GLES20.glUseProgram(program)

        // Передаем матрицу в шейдер
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Разрешаем массив вершин
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Подготавливаем координаты вершин
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            0, vertexBuffer
        )

        // Рисуем каждый треугольник с разным цветом
        for (i in 0..3) {
            // Устанавливаем цвет для текущего треугольника
            GLES20.glUniform4fv(colorHandle, 1, colors[i], 0)

            // Рисуем треугольник (3 вершины начиная с i*9)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, i*3, 3)
        }

        // Отключаем массив вершин
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        // Проверка ошибок компиляции
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compilation error: $error")
        }
        return shader
    }
}