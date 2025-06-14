
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class RubikCubeRenderer implements GLSurfaceView.Renderer {
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] rotationMatrix = new float[16];
    private final Context context;
    private String currentFacelets = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
    private RubikCube rubikCube;
    private int shaderProgram;

    private int animatingAxis = 0;
    private int animatingLayer = 0;
    private float targetAngle = 0;
    private float currentAngle = 0;
    private boolean isAnimating = false;
    private Runnable animationCallback;

    public RubikCubeRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Черный фон
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        rubikCube = new RubikCube(0.5f, context); // Увеличенный размер кубика
        shaderProgram = createShaderProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d("GL", "Surface changed: " + width + "x" + height);
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        Log.d("GL", "Projection matrix: " + Arrays.toString(projectionMatrix));
        Log.d("GL", "View matrix: " + Arrays.toString(viewMatrix));

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 10);

        Matrix.setLookAtM(viewMatrix, 0,
                0, 0, 5,  // Позиция камеры (дальше от объекта)
                0f, 0f, 0f,  // Точка, на которую смотрим
                0f, 1.0f, 0.0f); // Вектор "вверх"
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 20, 1, 1, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, rotationMatrix, 0);

        rubikCube.draw(mvpMatrix, shaderProgram);

        if (isAnimating) {
            handleAnimation();
        }
    }

    public void updateColors(String facelets) {
        if (facelets != null) {
            currentFacelets = facelets;
            rubikCube.updateColors(facelets);
        }
    }

    public void resetToWhite() {
        rubikCube.resetAllToWhite();
    }

    public void startAnimation(int axis, int layer, float angle, Runnable callback) {
        this.animatingAxis = axis;
        this.animatingLayer = layer;
        this.targetAngle = angle;
        this.currentAngle = 0;
        this.isAnimating = true;
        this.animationCallback = callback;
    }

    private void handleAnimation() {
        float step = 5f;

        if (Math.abs(currentAngle - targetAngle) <= step) {
            rubikCube.rotateLayer(animatingAxis, animatingLayer, targetAngle - currentAngle);
            currentAngle = targetAngle;
            isAnimating = false;
            if (animationCallback != null) {
                animationCallback.run();
            }
        } else {
            float direction = targetAngle > currentAngle ? 1 : -1;
            rubikCube.rotateLayer(animatingAxis, animatingLayer, direction * step);
            currentAngle += direction * step;
        }
    }

    public void applyMove(String move) {
        if (move == null || move.isEmpty()) return;

        char face = move.charAt(0);
        boolean clockwise = !move.contains("'");
        boolean doubleTurn = move.contains("2");

        char[] cubeState = currentFacelets.toCharArray();

        int rotations = doubleTurn ? 2 : 1;
        for (int i = 0; i < rotations; i++) {
            switch (face) {
                case 'U': rotateUFace(cubeState, clockwise); break;
                case 'D': rotateDFace(cubeState, clockwise); break;
                case 'F': rotateFFace(cubeState, clockwise); break;
                case 'B': rotateBFace(cubeState, clockwise); break;
                case 'L': rotateLFace(cubeState, clockwise); break;
                case 'R': rotateRFace(cubeState, clockwise); break;
            }
        }

        currentFacelets = new String(cubeState);
        updateColors(currentFacelets);
    }

    // Методы вращения граней (остаются без изменений)
    private void rotateUFace(char[] state, boolean clockwise) {
        rotateFace(state, new int[]{0, 1, 2, 5, 8, 7, 6, 3}, clockwise);
        rotateEdges(state, new int[]{9, 36, 27, 18}, clockwise);
    }

    private void rotateDFace(char[] state, boolean clockwise) {
        rotateFace(state, new int[]{45, 46, 47, 50, 53, 52, 51, 48}, clockwise);
        rotateEdges(state, new int[]{24, 33, 42, 15}, clockwise);
    }

    private void rotateFFace(char[] state, boolean clockwise) {
        rotateFace(state, new int[]{18, 19, 20, 23, 26, 25, 24, 21}, clockwise);
        rotateEdges(state, new int[]{6, 11, 47, 38}, clockwise);
    }

    private void rotateBFace(char[] state, boolean clockwise) {
        rotateFace(state, new int[]{36, 37, 38, 41, 44, 43, 42, 39}, clockwise);
        rotateEdges(state, new int[]{2, 29, 51, 20}, clockwise);
    }

    private void rotateLFace(char[] state, boolean clockwise) {
        rotateFace(state, new int[]{9, 10, 11, 14, 17, 16, 15, 12}, clockwise);
        rotateEdges(state, new int[]{0, 18, 45, 44}, clockwise);
    }

    private void rotateRFace(char[] state, boolean clockwise) {
        rotateFace(state, new int[]{27, 28, 29, 32, 35, 34, 33, 30}, clockwise);
        rotateEdges(state, new int[]{8, 26, 53, 36}, clockwise);
    }

    private void rotateFace(char[] state, int[] indices, boolean clockwise) {
        if (clockwise) {
            char temp = state[indices[6]];
            state[indices[6]] = state[indices[4]];
            state[indices[4]] = state[indices[2]];
            state[indices[2]] = state[indices[0]];
            state[indices[0]] = temp;

            temp = state[indices[7]];
            state[indices[7]] = state[indices[5]];
            state[indices[5]] = state[indices[3]];
            state[indices[3]] = state[indices[1]];
            state[indices[1]] = temp;
        } else {
            char temp = state[indices[0]];
            state[indices[0]] = state[indices[2]];
            state[indices[2]] = state[indices[4]];
            state[indices[4]] = state[indices[6]];
            state[indices[6]] = temp;

            temp = state[indices[1]];
            state[indices[1]] = state[indices[3]];
            state[indices[3]] = state[indices[5]];
            state[indices[5]] = state[indices[7]];
            state[indices[7]] = temp;
        }
    }

    private void rotateEdges(char[] state, int[] indices, boolean clockwise) {
        if (clockwise) {
            char temp = state[indices[3]];
            state[indices[3]] = state[indices[2]];
            state[indices[2]] = state[indices[1]];
            state[indices[1]] = state[indices[0]];
            state[indices[0]] = temp;
        } else {
            char temp = state[indices[0]];
            state[indices[0]] = state[indices[1]];
            state[indices[1]] = state[indices[2]];
            state[indices[2]] = state[indices[3]];
            state[indices[3]] = temp;
        }
    }

    private int createShaderProgram() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Добавьте проверку ошибок
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.e("GL", "Shader compilation error: " + error);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed");
        }
        return shader;
    }

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";
}