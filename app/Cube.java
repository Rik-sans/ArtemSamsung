package com.example.artem52;

import android.content.Context;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.Matrix;


public class Cube {
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer indexBuffer;
    private final float[] modelMatrix = new float[16];
    private final boolean[] visibleFaces;
    private final float[][] faceColors;
    private final Context context;

    // Конструктор с 6 параметрами (включая Context)
    public Cube(float x, float y, float z, float size, boolean[] visibleFaces, Context context) {

        this.context = context;
        this.visibleFaces = visibleFaces;
        this.faceColors = new float[6][4];
        resetToWhite();
        // Инициализация цветов граней (по умолчанию)
        initDefaultColors();

        // Инициализация вершин и индексов
        float[] vertices = createCubeVertices(size);
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        short[] indices = createCubeIndices();
        ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);

        // Инициализация матрицы модели
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
    }

    private void initDefaultColors() {
        // Белый (верх)
        faceColors[0] = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        // Желтый (низ)
        faceColors[1] = new float[]{1.0f, 1.0f, 0.0f, 1.0f};
        // Зеленый (перед)
        faceColors[2] = new float[]{0.0f, 1.0f, 0.0f, 1.0f};
        // Синий (зад)
        faceColors[3] = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
        // Оранжевый (лево)
        faceColors[4] = new float[]{1.0f, 0.5f, 0.0f, 1.0f};
        // Красный (право)
        faceColors[5] = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
    }

    public void draw(float[] vpMatrix, int program) {
        float[] finalMatrix = new float[16];
        Matrix.multiplyMM(finalMatrix, 0, vpMatrix, 0, modelMatrix, 0);

        int mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        int colorHandle = GLES20.glGetUniformLocation(program, "vColor");

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, finalMatrix, 0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        for (int face = 0; face < 6; face++) {
            if (visibleFaces[face]) {
                GLES20.glUniform4fv(colorHandle, 1, faceColors[face], 0);
                GLES20.glDrawElements(
                        GLES20.GL_TRIANGLES,
                        6,
                        GLES20.GL_UNSIGNED_SHORT,
                        indexBuffer
                );
            }
        }

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void rotate(float angle, float x, float y, float z) {
        Matrix.rotateM(modelMatrix, 0, angle, x, y, z);
    }

    public void setFaceColor(int face, float[] color) {
        if (face >= 0 && face < 6 && color != null && color.length == 4) {
            System.arraycopy(color, 0, faceColors[face], 0, 4);
        }
    }

    private float[] createCubeVertices(float size) {
        return new float[] {
                // Передняя грань
                -size, -size, size, size, -size, size,
                size, size, size, -size, size, size,
                // Задняя грань
                -size, -size, -size, -size, size, -size,
                size, size, -size, size, -size, -size,
                // Левая грань
                -size, -size, -size, -size, -size, size,
                -size, size, size, -size, size, -size,
                // Правая грань
                size, -size, -size, size, size, -size,
                size, size, size, size, -size, size,
                // Верхняя грань
                -size, size, -size, -size, size, size,
                size, size, size, size, size, -size,
                // Нижняя грань
                -size, -size, -size, size, -size, -size,
                size, -size, size, -size, -size, size
        };
    }

    private short[] createCubeIndices() {
        return new short[] {
                0, 1, 2, 0, 2, 3,    // Передняя грань
                4, 5, 6, 4, 6, 7,    // Задняя грань
                8, 9,10, 8,10,11,    // Левая грань
                12,13,14,12,14,15,   // Правая грань
                16,17,18,16,18,19,   // Верхняя грань
                20,21,22,20,22,23    // Нижняя грань
        };
    }

    public float getX() { return modelMatrix[12]; }
    public float getY() { return modelMatrix[13]; }
    public float getZ() { return modelMatrix[14]; }
    public boolean isFaceVisible(int face) {
        if (face < 0 || face >= 6) return false;
        return visibleFaces[face];
    }
    public void resetToWhite() {
        float[] white = {1.0f, 1.0f, 1.0f, 1.0f};
        for (int i = 0; i < 6; i++) {
            if (visibleFaces[i]) {
                setFaceColor(i, white);
            }
        }
    }

}