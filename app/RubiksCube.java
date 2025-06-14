/*import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

public class RubiksCube {
    private ModelInstance cubeInstance;
    private final Color[] faceColors = new Color[6];

    public RubiksCube(ModelBuilder modelBuilder) {
        // Создаем 3D модель
        Model model = modelBuilder.createBox(1f, 1f, 1f,
                new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                Usage.Position | Usage.Normal);

        cubeInstance = new ModelInstance(model);

        // Цвета по умолчанию (как у классического кубика)
        faceColors[0] = Color.WHITE;   // Верх
        faceColors[1] = Color.YELLOW;  // Низ
        faceColors[2] = Color.BLUE;    // Перед
        faceColors[3] = Color.GREEN;   // Зад
        faceColors[4] = Color.RED;     // Право
        faceColors[5] = Color.ORANGE;  // Лево
    }

    public void setFaceColor(int faceIndex, Color color) {
        faceColors[faceIndex] = color;
        // Здесь нужно обновить материалы куба
    }
}*/