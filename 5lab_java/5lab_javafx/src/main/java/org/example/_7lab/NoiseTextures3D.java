package org.example._7lab;

import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.scene.transform.Rotate;
import javafx.animation.AnimationTimer;

import java.util.Random;

public class NoiseTextures3D extends Application {

    private Group root;
    private Scene scene;
    private PerspectiveCamera camera;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Random random = new Random();

    @Override
    public void start(Stage primaryStage) {
        // Создаем корневой узел
        root = new Group();

        // Создаем сцену с 3D поддержкой
        scene = new Scene(root, 800, 600, true);
        scene.setFill(Color.LIGHTGRAY);

        // Настраиваем камеру
        setupCamera();

        // Создаем 3D объекты с текстурами
        create3DObjects();

        // Настраиваем обработку мыши для вращения сцены
        setupMouseControl();

        // Настраиваем анимацию вращения
        setupAnimation();

        // Настраиваем сцену и показываем окно
        primaryStage.setTitle("3D Textures: Marble and Wood");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-1000);
        scene.setCamera(camera);
    }

    private void create3DObjects() {
        // Создаем мраморную сферу
        Sphere marbleSphere = createMarbleSphere(100);
        marbleSphere.setTranslateX(-200);
        marbleSphere.setTranslateY(-100);

        // Создаем деревянный куб
        Box woodenBox = createWoodenBox(150);
        woodenBox.setTranslateX(200);
        woodenBox.setTranslateY(-100);

        root.getChildren().addAll(marbleSphere, woodenBox);
    }

    private Sphere createMarbleSphere(double radius) {
        Sphere sphere = new Sphere(radius);

        // Создаем процедурную мраморную текстуру
        int textureSize = 512;
        WritableImage marbleTexture = createMarbleTexture(textureSize, textureSize);

        PhongMaterial marbleMaterial = new PhongMaterial();
        marbleMaterial.setDiffuseMap(marbleTexture);
        marbleMaterial.setSpecularColor(Color.WHITE);
        marbleMaterial.setSpecularPower(64);

        sphere.setMaterial(marbleMaterial);
        return sphere;
    }

    private Box createWoodenBox(double size) {
        Box box = new Box(size, size, size);

        // Создаем процедурную деревянную текстуру
        int textureSize = 512;
        WritableImage woodTexture = createWoodTexture(textureSize, textureSize);

        PhongMaterial woodMaterial = new PhongMaterial();
        woodMaterial.setDiffuseMap(woodTexture);
        woodMaterial.setSpecularColor(Color.BROWN);
        woodMaterial.setSpecularPower(32);

        box.setMaterial(woodMaterial);
        return box;
    }

    private WritableImage createMarbleTexture(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        // Создаем временный массив для хранения цветов
        Color[][] colorBuffer = new Color[width][height];

        double scale = 0.02;
        double veinScale = 0.05;
        double veinFrequency = 8.0;
        double turbulence = 32.0;

        Color baseColor = Color.rgb(220, 220, 220);
        Color veinColor = Color.rgb(180, 180, 200);
        Color darkVeinColor = Color.rgb(100, 100, 120);

        // Первый проход - создаем основную текстуру
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double xt = x + turbulence * ImprovedNoise.noise(x * 0.01, y * 0.01, 0);
                double yt = y + turbulence * ImprovedNoise.noise(x * 0.01 + 5.2, y * 0.01 + 1.3, 0);

                double noiseValue = Math.abs(ImprovedNoise.noise(xt * scale, yt * scale, 0));
                double veins = Math.sin(xt * veinScale * veinFrequency) * 0.5 + 0.5;
                veins = Math.pow(veins, 4);

                double gradient = 1.0 - Math.sqrt(Math.pow(x - width/2.0, 2) + Math.pow(y - height/2.0, 2)) / (width/2.0);
                double pattern = noiseValue * 0.6 + veins * 0.3 + gradient * 0.1;

                Color color;
                if (pattern < 0.3) {
                    color = darkVeinColor.interpolate(veinColor, pattern / 0.3);
                } else if (pattern < 0.7) {
                    color = veinColor.interpolate(baseColor, (pattern - 0.3) / 0.4);
                } else {
                    color = baseColor.interpolate(Color.WHITE, (pattern - 0.7) / 0.3);
                }

                double variation = 0.9 + random.nextDouble() * 0.2;
                color = Color.color(
                        Math.min(1.0, color.getRed() * variation),
                        Math.min(1.0, color.getGreen() * variation),
                        Math.min(1.0, color.getBlue() * variation)
                );

                colorBuffer[x][y] = color;
                pixelWriter.setColor(x, y, color);
            }
        }

        // Второй проход - добавляем вкрапления
        for (int i = 0; i < width * height / 100; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = random.nextInt(3) + 1;

            for (int dx = -size; dx <= size; dx++) {
                for (int dy = -size; dy <= size; dy++) {
                    if (x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height) {
                        Color current = colorBuffer[x + dx][y + dy];
                        Color darker = current.darker();
                        colorBuffer[x + dx][y + dy] = darker;
                        pixelWriter.setColor(x + dx, y + dy, darker);
                    }
                }
            }
        }

        // Третий проход - добавляем светлые вкрапления
        for (int i = 0; i < width * height / 200; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = random.nextInt(2) + 1;

            for (int dx = -size; dx <= size; dx++) {
                for (int dy = -size; dy <= size; dy++) {
                    if (x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height) {
                        Color current = colorBuffer[x + dx][y + dy];
                        Color brighter = current.brighter();
                        colorBuffer[x + dx][y + dy] = brighter;
                        pixelWriter.setColor(x + dx, y + dy, brighter);
                    }
                }
            }
        }

        return image;
    }

    private WritableImage createWoodTexture(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter pixelWriter = image.getPixelWriter();

        double scale = 0.03;
        double ringFrequency = 15.0;
        double noiseIntensity = 0.3;

        Color baseColor = Color.rgb(150, 100, 60);
        Color ringColor = Color.rgb(100, 60, 30);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Основные кольца
                double distanceFromCenter = Math.sqrt(Math.pow(x - width/2.0, 2) + Math.pow(y - height/2.0, 2));
                double rings = (Math.sin(distanceFromCenter * ringFrequency / width * 2 * Math.PI) * 0.5 + 0.5);

                // Добавляем шум для естественного вида
                double nx = x * scale;
                double ny = y * scale;
                double noise = ImprovedNoise.noise(nx, ny, 0) * noiseIntensity;

                // Создаем цвет с учетом колец и шума
                double r = baseColor.getRed() + (ringColor.getRed() - baseColor.getRed()) * rings + noise;
                double g = baseColor.getGreen() + (ringColor.getGreen() - baseColor.getGreen()) * rings + noise;
                double b = baseColor.getBlue() + (ringColor.getBlue() - baseColor.getBlue()) * rings + noise;

                // Добавляем случайные вариации
                r += (random.nextDouble() - 0.5) * 0.1;
                g += (random.nextDouble() - 0.5) * 0.1;
                b += (random.nextDouble() - 0.5) * 0.1;

                // Ограничиваем значения цвета
                r = Math.min(1.0, Math.max(0, r));
                g = Math.min(1.0, Math.max(0, g));
                b = Math.min(1.0, Math.max(0, b));

                pixelWriter.setColor(x, y, Color.color(r, g, b));
            }
        }
        return image;
    }

    // Класс для генерации шума Перлина (нужно добавить в код)
    static class ImprovedNoise {
        static public double noise(double x, double y, double z) {
            int X = (int)Math.floor(x) & 255;
            int Y = (int)Math.floor(y) & 255;
            int Z = (int)Math.floor(z) & 255;

            x -= Math.floor(x);
            y -= Math.floor(y);
            z -= Math.floor(z);

            double u = fade(x);
            double v = fade(y);
            double w = fade(z);

            int A = p[X  ]+Y, AA = p[A]+Z, AB = p[A+1]+Z;
            int B = p[X+1]+Y, BA = p[B]+Z, BB = p[B+1]+Z;

            return lerp(w, lerp(v, lerp(u, grad(p[AA  ], x  , y  , z   ),
                                    grad(p[BA  ], x-1, y  , z   )),
                            lerp(u, grad(p[AB  ], x  , y-1, z   ),
                                    grad(p[BB  ], x-1, y-1, z   ))),
                    lerp(v, lerp(u, grad(p[AA+1], x  , y  , z-1 ),
                                    grad(p[BA+1], x-1, y  , z-1 )),
                            lerp(u, grad(p[AB+1], x  , y-1, z-1 ),
                                    grad(p[BB+1], x-1, y-1, z-1 ))));
        }

        static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
        static double lerp(double t, double a, double b) { return a + t * (b - a); }
        static double grad(int hash, double x, double y, double z) {
            int h = hash & 15;
            double u = h<8 ? x : y,
                    v = h<4 ? y : h==12||h==14 ? x : z;
            return ((h&1) == 0 ? u : -u) + ((h&2) == 0 ? v : -v);
        }

        static final int permutation[] = { 151,160,137,91,90,15,
                131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
                190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
                88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
                77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
                102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
                135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
                5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
                223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
                129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
                251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
                49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
                138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
        };
        static final int p[] = new int[512];
        static { for (int i=0; i<256; i++) p[256+i] = p[i] = permutation[i]; }
    }

    private void setupMouseControl() {
        final double[] anchorX = new double[1];
        final double[] anchorY = new double[1];
        final double[] anchorAngleX = new double[1];
        final double[] anchorAngleY = new double[1];

        scene.setOnMousePressed(event -> {
            anchorX[0] = event.getSceneX();
            anchorY[0] = event.getSceneY();
            anchorAngleX[0] = rotateX.getAngle();
            anchorAngleY[0] = rotateY.getAngle();
        });

        scene.setOnMouseDragged(event -> {
            rotateX.setAngle(anchorAngleX[0] - (anchorY[0] - event.getSceneY()));
            rotateY.setAngle(anchorAngleY[0] + anchorX[0] - event.getSceneX());
        });

        // Добавляем вращение к корневой группе
        root.getTransforms().addAll(rotateX, rotateY);
    }

    private void setupAnimation() {
        // Анимация автоматического вращения
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                rotateY.setAngle(rotateY.getAngle() + 0.5);
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}