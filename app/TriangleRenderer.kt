

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TriangleRenderer : GLSurfaceView.Renderer {
    private val vertexShaderCode =
        "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    private var triangleCoords = floatArrayOf(
        0.0f, 0.5f, 0.0f,   // верхняя вершина
        -0.5f, -0.5f, 0.0f, // левая нижняя
        0.5f, -0.5f, 0.0f   // правая нижняя
    )

    private val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f) // красный цвет

    private var program: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f) // черный фон

        // Подготовка шейдеров
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Активируем программу
        GLES20.glUseProgram(program)

        // Получаем handle для вершинного атрибута
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Подготавливаем координаты вершин
        val vertexBuffer = java.nio.ByteBuffer
            .allocateDirect(triangleCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(triangleCoords).position(0)

        // Передаем координаты вершин
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            12, vertexBuffer
        )

        // Получаем handle для цвета
        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")

        // Устанавливаем цвет
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        // Рисуем треугольник
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // Отключаем массив вершин
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}