package com.example.artem52

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.artem52.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.opengl.GLSurfaceView
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.os.Handler
import android.os.Looper
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: RotatingCubeRenderer
    private lateinit var binding: ActivityMainBinding
    private var solvingJob: Job? = null

    private var cameraFront = true
    private val frontCameraPosition = floatArrayOf(5f, 5f, 14f)
    private val backCameraPosition = floatArrayOf(-5f, -5f, -14f)

    // Цвета для каждой стороны куба
    private val colorMap = mapOf(
        'U' to floatArrayOf(1f, 1f, 1f, 1f),    // Белый
        'R' to floatArrayOf(1f, 0f, 0f, 1f),    // Красный
        'F' to floatArrayOf(0f, 1f, 0f, 1f),    // Зеленый
        'D' to floatArrayOf(1f, 1f, 0f, 1f),    // Желтый
        'L' to floatArrayOf(1f, 0.5f, 0f, 1f),  // Оранжевый
        'B' to floatArrayOf(0f, 0f, 1f, 1f),    // Синий
        ' ' to floatArrayOf(0.2f, 0.2f, 0.2f, 1f) // Серый (невидимые грани)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderer = RotatingCubeRenderer().apply {
            setCameraPosition(frontCameraPosition)
            // Устанавливаем callback для requestRender
            setRenderRequestCallback {
                glSurfaceView.requestRender()
            }
        }

        glSurfaceView = binding.glSurfaceView.apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        setupInputListener()
        setupSolveButton()
        setupCameraButton()
    }
    private fun setupCameraButton() {
        binding.cameraSwitchButton.setOnClickListener {
            cameraFront = !cameraFront
            val newPosition = if (cameraFront) frontCameraPosition else backCameraPosition
            renderer.setCameraPosition(newPosition)
            glSurfaceView.requestRender()
        }
    }
    private fun setupInputListener() {
        binding.faceletsInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { input ->
                    val paddedInput = input.padEnd(54, ' ')
                    updateCubeColors(paddedInput)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSolveButton() {
        binding.solveButton.setOnClickListener {
            val facelets = binding.faceletsInput.text.toString().trim()
            if (validateInput(facelets)) {
                solveCube()
            }
        }
    }
    private fun solveCube() {
        if (solvingJob?.isActive == true) return

        val facelets = binding.faceletsInput.text.toString().trim()
        if (!validateInput(facelets)) return

        setUiState(isSolving = true)

        solvingJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val solution = withContext(Dispatchers.Default) {
                    solveCube(facelets.uppercase(), null)
                }
                showSolution(solution)
                animateSolution(solution)
            } catch (e: Exception) {
                showError(e.message ?: "Unknown error occurred")
            } finally {
                setUiState(isSolving = false)
            }
        }
    }
    private fun animateSolution(solution: String) {
        if (solution.isBlank()) return

        val moves = solution.split(" ").filter { it.isNotBlank() }
        if (moves.isEmpty()) return

        animateMovesSequentially(moves, 0)
    }

    private fun animateMovesSequentially(moves: List<String>, index: Int) {
        if (index >= moves.size) return

        val move = moves[index]
        val (axis, layer, angle) = parseMove(move)

        // Анимируем поворот
        renderer.startAnimation(axis, layer, angle) {
            // После завершения анимации применяем ход к состоянию куба
            runOnUiThread {
                renderer.applyMove(move)
                // Переходим к следующему ходу
                animateMovesSequentially(moves, index + 1)
            }
        }
    }

    private fun parseMove(move: String): Triple<Int, Int, Float> {
        return when {
            move.contains("U") -> Triple(1, 1, if (move.contains("'")) -90f else if (move.contains("2")) 180f else 90f)
            move.contains("D") -> Triple(1, -1, if (move.contains("'")) -90f else if (move.contains("2")) 180f else 90f)
            move.contains("L") -> Triple(0, -1, if (move.contains("'")) -90f else if (move.contains("2")) 180f else 90f)
            move.contains("R") -> Triple(0, 1, if (move.contains("'")) -90f else if (move.contains("2")) 180f else 90f)
            move.contains("F") -> Triple(2, 1, if (move.contains("'")) -90f else if (move.contains("2")) 180f else 90f)
            move.contains("B") -> Triple(2, -1, if (move.contains("'")) -90f else if (move.contains("2")) 180f else 90f)
            else -> Triple(0, 0, 90f)
        }

    }
    private fun validateInput(facelets: String): Boolean {
        return when {
            facelets.length != 54 -> {
                showError("Input must be exactly 54 characters")
                false
            }
            !facelets.matches(Regex("^[UDFBRLudfbrl ]+$")) -> {
                showError("Invalid characters. Only U,D,F,B,R,L allowed")
                false
            }
            else -> true
        }
    }

    private fun showSolution(solution: String) {
        binding.resultTextView.text = if (solution.isEmpty()) "No solution found" else "Solution: $solution"
    }

    private fun updateCubeColors(facelets: String) {
        // Сначала сбрасываем все цвета
        renderer.resetAllCubes()

        // Обновляем цвета согласно вводу
        facelets.forEachIndexed { index, char ->
            if (char != ' ') {
                val color = colorMap[char.uppercaseChar()] ?: colorMap[' ']!!
                setFaceletColor(index, color)
            }
        }

        glSurfaceView.requestRender()
    }
    private fun setFaceletColor(faceletIndex: Int, color: FloatArray) {
        val (cubePos, face) = when (faceletIndex) {
            // Up face (U1-U9)
            in 0..8 -> {
                val x = when (faceletIndex % 3) {
                    0 -> -1; 1 -> 0; 2 -> 1; else -> 0
                }
                val z = when (faceletIndex / 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                Triple(x, 1, z) to 4 // Up face
            }
            // Right face (R1-R9)
            in 9..17 -> {
                val y = when ((faceletIndex - 9) % 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                val z = when ((faceletIndex - 9) / 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                Triple(1, y, z) to 3 // Right face
            }
            // Front face (F1-F9)
            in 18..26 -> {
                val x = when ((faceletIndex - 18) % 3) {
                    0 -> -1; 1 -> 0; 2 -> 1; else -> 0
                }
                val y = when ((faceletIndex - 18) / 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                Triple(x, y, 1) to 0 // Front face
            }
            // Down face (D1-D9)
            in 27..35 -> {
                val x = when ((faceletIndex - 27) % 3) {
                    0 -> -1; 1 -> 0; 2 -> 1; else -> 0
                }
                val z = when ((faceletIndex - 27) / 3) {
                    0 -> -1; 1 -> 0; 2 -> 1; else -> 0
                }
                Triple(x, -1, z) to 5 // Down face
            }
            // Left face (L1-L9)
            in 36..44 -> {
                val y = when ((faceletIndex - 36) % 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                val z = when ((faceletIndex - 36) / 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                Triple(-1, y, z) to 2 // Left face
            }
            // Back face (B1-B9)
            in 45..53 -> {
                val x = when ((faceletIndex - 45) % 3) {
                    0 -> -1; 1 -> 0; 2 -> 1; else -> 0
                }
                val y = when ((faceletIndex - 45) / 3) {
                    0 -> 1; 1 -> 0; 2 -> -1; else -> 0
                }
                Triple(x, y, -1) to 1 // Back faceUUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB
            }
            else -> return
        }

        // Устанавливаем цвет только для внешних кубиков
        if (abs(cubePos.first) == 1 || abs(cubePos.second) == 1 || abs(cubePos.third) == 1) {
            renderer.cubes.find { cube ->
                cube.x == cubePos.first.toFloat() &&
                        cube.y == cubePos.second.toFloat() &&
                        cube.z == cubePos.third.toFloat()
            }?.setFaceColor(face, color)
        }
    }

    private fun setUiState(isSolving: Boolean) {
        binding.progressBar.visibility = if (isSolving) View.VISIBLE else View.GONE
        binding.solveButton.isEnabled = !isSolving
        binding.scrambleButton.isEnabled = !isSolving
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }
    private external fun solveCube(facelets: String, pattern: String?): String


    companion object {
        init {
            System.loadLibrary("cube-solver")
        }
    }
}