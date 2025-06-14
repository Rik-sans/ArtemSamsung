

import android.content.Context;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.HashMap;

public class RubikCube {
    private static final int CUBE_COUNT = 27;
    private static final float CUBE_SPACING = 0.02f;
    private final Cube[] cubes;
    private final float cubeSize;
    private final Context context;

    public RubikCube(float size, Context context) {
        this.cubeSize = size;
        this.context = context;
        this.cubes = new Cube[CUBE_COUNT];
        initCubes();
    }

    private void initCubes() {
        int index = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    float px = x * (cubeSize + CUBE_SPACING);
                    float py = y * (cubeSize + CUBE_SPACING);
                    float pz = z * (cubeSize + CUBE_SPACING);

                    // Все грани видимы для центральных кубиков
                    boolean[] visibleFaces = {
                            z == 1 || Math.abs(z) != 1,  // FRONT
                            z == -1 || Math.abs(z) != 1, // BACK
                            x == -1 || Math.abs(x) != 1, // LEFT
                            x == 1 || Math.abs(x) != 1,  // RIGHT
                            y == 1 || Math.abs(y) != 1,  // UP
                            y == -1 || Math.abs(y) != 1  // DOWN
                    };

                    cubes[index++] = new Cube(px, py, pz, cubeSize, visibleFaces, context);
                }
            }
        }
    }

    public void draw(float[] mvpMatrix, int program) {
        for (Cube cube : cubes) {
            if (cube != null) {
                cube.draw(mvpMatrix, program);
            }
        }
    }

    public void rotateLayer(int axis, int layer, float angle) {
        for (Cube cube : cubes) {
            if (cube != null && isInLayer(cube, axis, layer)) {
                cube.rotate(angle,
                        axis == 0 ? 1 : 0,
                        axis == 1 ? 1 : 0,
                        axis == 2 ? 1 : 0);
            }
        }
    }

    private boolean isInLayer(Cube cube, int axis, int layer) {
        float pos;
        switch (axis) {
            case 0: pos = cube.getX(); break; // X-axis
            case 1: pos = cube.getY(); break; // Y-axis
            case 2: pos = cube.getZ(); break; // Z-axis
            default: pos = 0;
        }
        return Math.round(pos / (cubeSize + CUBE_SPACING)) == layer;
    }

    private void updateCubeColors(Cube cube, int[] facePositions, String facelets) {
        for (int face = 0; face < 6; face++) {
            if (facePositions[face] > 0 && facePositions[face] < facelets.length()) {
                char colorChar = facelets.charAt(facePositions[face]);
                float[] color = COLOR_MAP.get(colorChar);
                if (color != null) {
                    cube.setFaceColor(face, color);
                }
            }
        }
    }
    private static final Map<Character, float[]> COLOR_MAP = new HashMap<Character, float[]>() {{
        put('U', new float[]{1, 1, 1, 1});      // Белый
        put('D', new float[]{1, 1, 0, 1});      // Желтый
        put('F', new float[]{0, 1, 0, 1});      // Зеленый
        put('B', new float[]{0, 0, 1, 1});      // Синий
        put('L', new float[]{1, 0.5f, 0, 1});   // Оранжевый
        put('R', new float[]{1, 0, 0, 1});      // Красный
    }};

    // Карта соответствия: [кубик][грань] = позиция в строке facelets
    private static final int[][][] FACE_MAP = new int[27][6][2];

    static {
        // Инициализация карты соответствия
        initFaceMap();
    }

    private static void initFaceMap() {
        // Центральные кубики (по 1 грани)
        mapFace(4, 0, 4);   // U5
        mapFace(22, 1, 4);  // D5
        mapFace(10, 2, 4);  // F5
        mapFace(16, 3, 4);  // B5
        mapFace(12, 4, 4);  // L5
        mapFace(14, 5, 4);  // R5

        // Ребра (по 2 грани)
        // Верхние ребра
        mapEdge(1, 0, 1, 5, 3);   // U2 (U+R)
        mapEdge(3, 0, 5, 4, 1);   // U4 (U+L)
        mapEdge(5, 0, 7, 2, 1);   // U6 (U+F)
        mapEdge(7, 0, 3, 3, 1);   // U8 (U+B)

        // Средние ребра
        mapEdge(9, 2, 3, 5, 7);   // F3 (F+R)
        mapEdge(11, 2, 5, 4, 3);  // F7 (F+L)
        mapEdge(15, 3, 3, 5, 1);  // B3 (B+R)
        mapEdge(17, 3, 5, 4, 7);  // B7 (B+L)
        mapEdge(19, 1, 1, 2, 7);  // D1 (D+F)
        mapEdge(21, 1, 3, 3, 7);  // D3 (D+B)
        mapEdge(23, 1, 5, 4, 5);  // D7 (D+L)
        mapEdge(25, 1, 7, 5, 5);  // D9 (D+R)

        // Углы (по 3 грани)
        // Верхние углы
        mapCorner(0, 0, 0, 5, 2, 2, 6);  // U1 (U+R+F)
        mapCorner(2, 0, 2, 5, 8, 3, 0);  // U3 (U+R+B)
        mapCorner(6, 0, 6, 4, 0, 2, 8);  // U7 (U+L+F)
        mapCorner(8, 0, 8, 4, 6, 3, 2);  // U9 (U+L+B)

        // Нижние углы
        mapCorner(18, 1, 0, 2, 6, 4, 8); // D1 (D+F+L)
        mapCorner(20, 1, 2, 2, 8, 5, 6); // D3 (D+F+R)
        mapCorner(24, 1, 6, 3, 8, 4, 2); // D7 (D+B+L)
        mapCorner(26, 1, 8, 3, 6, 5, 8); // D9 (D+B+R)
    }

    private static void mapFace(int cubeIdx, int face, int faceletPos) {
        FACE_MAP[cubeIdx][face][0] = faceletPos;
        FACE_MAP[cubeIdx][face][1] = 1; // 1 грань
    }

    private static void mapEdge(int cubeIdx, int face1, int pos1, int face2, int pos2) {
        FACE_MAP[cubeIdx][face1][0] = pos1;
        FACE_MAP[cubeIdx][face1][1] = 2; // 2 грани (ребро)
        FACE_MAP[cubeIdx][face2][0] = pos2;
        FACE_MAP[cubeIdx][face2][1] = 2;
    }

    private static void mapCorner(int cubeIdx, int face1, int pos1, int face2, int pos2, int face3, int pos3) {
        FACE_MAP[cubeIdx][face1][0] = pos1;
        FACE_MAP[cubeIdx][face1][1] = 3; // 3 грани (угол)
        FACE_MAP[cubeIdx][face2][0] = pos2;
        FACE_MAP[cubeIdx][face2][1] = 3;
        FACE_MAP[cubeIdx][face3][0] = pos3;
        FACE_MAP[cubeIdx][face3][1] = 3;
    }


    public void updateColors(String facelets) {
        if (facelets == null || facelets.length() != 54) {
            // Сбрасываем на белый цвет, если ввод невалидный
            resetAllToWhite();
            return;
        }

        // Цвета по умолчанию (белые)
        float[] defaultColor = {1.0f, 1.0f, 1.0f, 1.0f}; // Белый
        Map<Character, float[]> colorMap = new HashMap<>();
        colorMap.put('U', new float[]{1, 1, 1, 1});    // Белый (верх)
        colorMap.put('D', new float[]{1, 1, 0, 1});    // Желтый (низ)
        colorMap.put('F', new float[]{0, 1, 0, 1});    // Зеленый (перед)
        colorMap.put('B', new float[]{0, 0, 1, 1});    // Синий (зад)
        colorMap.put('L', new float[]{1, 0.5f, 0, 1}); // Оранжевый (лево)
        colorMap.put('R', new float[]{1, 0, 0, 1});    // Красный (право)

        for (int i = 0; i < cubes.length; i++) {
            for (int face = 0; face < 6; face++) {
                if (cubes[i].isFaceVisible(face)) {
                    // Получаем символ из строки для текущей грани
                    char colorChar = facelets.charAt(getFaceletPosition(i, face));
                    float[] color = colorMap.getOrDefault(colorChar, defaultColor);
                    cubes[i].setFaceColor(face, color);
                }
            }
        }
    }
    private float[] getColorForChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'U': return new float[]{1, 1, 1, 1};    // Белый
            case 'D': return new float[]{1, 1, 0, 1};    // Жёлтый
            case 'F': return new float[]{0, 1, 0, 1};    // Зелёный
            case 'B': return new float[]{0, 0, 1, 1};    // Синий
            case 'L': return new float[]{1, 0.5f, 0, 1}; // Оранжевый
            case 'R': return new float[]{1, 0, 0, 1};    // Красный
            default:  return new float[]{1, 1, 1, 1};    // Белый по умолчанию
        }
    }
    public void resetAllToWhite() {
        float[] white = {1.0f, 1.0f, 1.0f, 1.0f};
        for (Cube cube : cubes) {
            for (int face = 0; face < 6; face++) {
                if (cube.isFaceVisible(face)) {
                    cube.setFaceColor(face, white);
                }
            }
        }
    }
    private int getFaceletPosition(int cubeIndex, int face) {
        // Определяем базовый индекс для каждой грани
        int baseIndex = 0;
        switch (face) {
            case 0: baseIndex = 0; break;   // U (верх)
            case 1: baseIndex = 27; break;  // D (низ)
            case 2: baseIndex = 18; break;  // F (перед)
            case 3: baseIndex = 36; break;  // B (зад)
            case 4: baseIndex = 9; break;   // L (лево)
            case 5: baseIndex = 45; break;  // R (право)
            default: return -1;
        }

        // Определяем позицию внутри грани на основе положения кубика
        int x = cubeIndex / 9;
        int y = (cubeIndex % 9) / 3;
        int z = cubeIndex % 3;

        // Преобразуем 3D координаты в индекс внутри грани
        int posInFace = 0;
        if (face == 0 || face == 1) { // U или D
            posInFace = z * 3 + x;
        } else if (face == 2 || face == 3) { // F или B
            posInFace = y * 3 + x;
        } else { // L или R
            posInFace = y * 3 + z;
        }

        return baseIndex + posInFace;
    }

}