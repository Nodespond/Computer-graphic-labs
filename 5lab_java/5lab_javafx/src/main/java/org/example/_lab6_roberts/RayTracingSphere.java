package org.example._lab6_roberts;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class RayTracingSphere extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final double FOV = Math.PI / 2;
    private static final double ASPECT_RATIO = (double) WIDTH / HEIGHT;
    private static final Color BACKGROUND = Color.BLACK;

    class Sphere {
        double[] center = new double[3];
        double radius;
        Color color;
        double speedX, speedY;

        Sphere(double x, double y, double z, double r, Color c, double sx, double sy) {
            center[0] = x; center[1] = y; center[2] = z;
            radius = r; color = c; speedX = sx; speedY = sy;
        }
    }

    private List<Sphere> spheres = new ArrayList<>();
    private double[][] zBuffer = new double[WIDTH][HEIGHT];

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Инициализация сфер
        spheres.add(new Sphere(0, 0, -2, 0.5, Color.RED, 0.5, 0.3));
        spheres.add(new Sphere(1, 0, -3, 0.4, Color.BLUE, 0.3, 0.4));
        spheres.add(new Sphere(-1, 0, -4, 0.6, Color.GREEN, 0.4, 0.5));

        WritableImage image = new WritableImage(WIDTH, HEIGHT);
        ImageView imageView = new ImageView(image);
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        primaryStage.setTitle(" Сферы трассировка лучей");
        primaryStage.setScene(scene);
        primaryStage.show();

        new AnimationTimer() {
            private long startTime = System.nanoTime();

            @Override
            public void handle(long now) {
                double time = (now - startTime) / 1_000_000_000.0;
                updateSpheres(time);
                renderFrame(image.getPixelWriter());
            }
        }.start();
    }

    private void updateSpheres(double time) {
        for (Sphere sphere : spheres) {
            sphere.center[0] = Math.sin(time * sphere.speedX);
            sphere.center[1] = Math.cos(time * sphere.speedY);
        }
    }

    private void renderFrame(PixelWriter pw) {
        // Очистка Z-буфера
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                zBuffer[x][y] = Double.POSITIVE_INFINITY;
                pw.setColor(x, y, BACKGROUND);
            }
        }

        double[] cameraPos = {0, 0, 0};

        // Рендеринг сфер с учетом Z-буфера
        for (Sphere sphere : spheres) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    double px = (2 * (x + 0.5) / WIDTH - 1) * Math.tan(FOV / 2) * ASPECT_RATIO;
                    double py = (1 - 2 * (y + 0.5) / HEIGHT) * Math.tan(FOV / 2);
                    double[] rayDir = normalize(new double[]{px, py, -1});

                    double t = intersectSphere(cameraPos, rayDir, sphere);
                    if (t > 0 && t < zBuffer[x][y]) {
                        zBuffer[x][y] = t;
                        double[] point = add(cameraPos, multiply(rayDir, t));
                        double[] normal = normalize(subtract(point, sphere.center));
                        double[] lightDir = normalize(new double[]{1, 1, -1});
                        double diffuse = Math.max(0, dot(normal, lightDir));
                        double brightness = 0.2 + 0.8 * diffuse;
                        pw.setColor(x, y, adjustColor(sphere.color, brightness));
                    }
                }
            }
        }
    }

    private double intersectSphere(double[] origin, double[] dir, Sphere sphere) {
        double[] oc = subtract(origin, sphere.center);
        double a = dot(dir, dir);
        double b = 2 * dot(oc, dir);
        double c = dot(oc, oc) - sphere.radius * sphere.radius;
        double discriminant = b * b - 4 * a * c;
        return discriminant < 0 ? -1 : (-b - Math.sqrt(discriminant)) / (2 * a);
    }

    // Векторные операции
    private static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return new double[]{v[0]/len, v[1]/len, v[2]/len};
    }

    private static double dot(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    private static double[] add(double[] a, double[] b) {
        return new double[]{a[0]+b[0], a[1]+b[1], a[2]+b[2]};
    }

    private static double[] subtract(double[] a, double[] b) {
        return new double[]{a[0]-b[0], a[1]-b[1], a[2]-b[2]};
    }

    private static double[] multiply(double[] v, double s) {
        return new double[]{v[0]*s, v[1]*s, v[2]*s};
    }

    private static Color adjustColor(Color color, double factor) {
        return new Color(
                clamp(color.getRed() * factor),
                clamp(color.getGreen() * factor),
                clamp(color.getBlue() * factor),
                1.0
        );
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}