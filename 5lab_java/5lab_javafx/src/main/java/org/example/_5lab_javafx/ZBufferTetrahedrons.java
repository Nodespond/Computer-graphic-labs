package org.example._5lab_javafx;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ZBufferTetrahedrons extends JFrame {
    private final ZBufferPanel zBufferPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ZBufferTetrahedrons frame = new ZBufferTetrahedrons();
            frame.setVisible(true);
        });
    }

    public ZBufferTetrahedrons() {
        setTitle("3D Tetrahedrons with Gouraud Shading");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        zBufferPanel = new ZBufferPanel(800, 600);
        add(zBufferPanel);

        createTetrahedrons();
        setupMouseControls();
    }

    private void createTetrahedrons() {
        List<Triangle3D> triangles = new ArrayList<>();

        // Тетраэдр 1 (красный)
        Point3D center1 = new Point3D(0, 0, 3);
        double size1 = 100.0;
        Tetrahedron tetra1 = new Tetrahedron(center1, size1, Color.RED);
        triangles.addAll(tetra1.getTriangles());

        // Тетраэдр 2 (зеленый)
        Point3D center2 = new Point3D(2, 1, 110);
        double size2 = 80;
        Tetrahedron tetra2 = new Tetrahedron(center2, size2, Color.GREEN);
        triangles.addAll(tetra2.getTriangles());

        // Тетраэдр 3 (синий)
        Point3D center3 = new Point3D(-1, -1, 190);
        double size3 = 60;
        Tetrahedron tetra3 = new Tetrahedron(center3, size3, Color.BLUE);
        triangles.addAll(tetra3.getTriangles());

        zBufferPanel.setTriangles(triangles);
        zBufferPanel.render();
    }

    private void setupMouseControls() {
        final Point[] lastPoint = new Point[1];

        zBufferPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint[0] = e.getPoint();
            }
        });

        zBufferPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastPoint[0].x;
                int dy = e.getY() - lastPoint[0].y;

                zBufferPanel.rotateCamera(dy * 0.01, dx * 0.01, 0);
                zBufferPanel.render();

                lastPoint[0] = e.getPoint();
            }
        });

        addMouseWheelListener(e -> {
            double scale = 1.0 + e.getWheelRotation() * 0.1;
            zBufferPanel.zoomCamera(scale);
            zBufferPanel.render();
        });
    }
}

class ZBufferPanel extends JPanel {
    private final int width;
    private final int height;
    private double[][] zBuffer;
    private BufferedImage image;
    private List<Triangle3D> triangles;
    private Camera camera;
    private Point3D lightDirection;

    public ZBufferPanel(int width, int height) {
        this.width = width;
        this.height = height;
        this.zBuffer = new double[width][height];
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.triangles = new ArrayList<>();
        this.camera = new Camera();
        this.lightDirection = new Point3D(0, 0, 200).normalize(); //источник света
        initializeZBuffer();
    }

    public void setTriangles(List<Triangle3D> triangles) {
        this.triangles = triangles;
    }

    public void rotateCamera(double angleX, double angleY, double angleZ) {
        camera.rotate(angleX, angleY, angleZ);
    }

    public void zoomCamera(double scale) {
        camera.zoom(scale);
    }

    private void initializeZBuffer() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                zBuffer[x][y] = Double.MAX_VALUE;
            }
        }
    }

    public void render() {
        initializeZBuffer();
        Graphics g = image.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        for (Triangle3D triangle : triangles) {
            renderTriangleWithGouraud(triangle);
        }

        repaint();
    }

    private void renderTriangleWithGouraud(Triangle3D triangle) {
        // Преобразуем вершины и нормали
        Point3D p1 = camera.transform(triangle.p1);
        Point3D p2 = camera.transform(triangle.p2);
        Point3D p3 = camera.transform(triangle.p3);

        Point3D n1 = camera.transformNormal(triangle.n1);
        Point3D n2 = camera.transformNormal(triangle.n2);
        Point3D n3 = camera.transformNormal(triangle.n3);

        // Рассчитываем освещение для каждой вершины
        double i1 = calculateLightIntensity(n1);
        double i2 = calculateLightIntensity(n2);
        double i3 = calculateLightIntensity(n3);

        // Проекция
        double fov = 300;
        p1 = projectPoint(p1, fov);
        p2 = projectPoint(p2, fov);
        p3 = projectPoint(p3, fov);

        // Ограничивающий прямоугольник
        int minX = (int) Math.max(0, Math.min(Math.min(p1.x, p2.x), p3.x));
        int maxX = (int) Math.min(width - 1, Math.max(Math.max(p1.x, p2.x), p3.x));
        int minY = (int) Math.max(0, Math.min(Math.min(p1.y, p2.y), p3.y));
        int maxY = (int) Math.min(height - 1, Math.max(Math.max(p1.y, p2.y), p3.y));

        // Рендеринг с интерполяцией освещения
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                double[] barycentric = computeBarycentric(x, y, p1, p2, p3);
                double alpha = barycentric[0];
                double beta = barycentric[1];
                double gamma = barycentric[2];

                if (alpha >= 0 && beta >= 0 && gamma >= 0) {
                    double z = alpha * p1.z + beta * p2.z + gamma * p3.z;

                    if (z < zBuffer[x][y]) {
                        zBuffer[x][y] = z;

                        // Интерполяция интенсивности освещения
                        double intensity = alpha * i1 + beta * i2 + gamma * i3;
                        Color shadedColor = applyLighting(triangle.color, intensity);
                        image.setRGB(x, y, shadedColor.getRGB());
                    }
                }
            }
        }
    }

    private double calculateLightIntensity(Point3D normal) {
        // Скалярное произведение нормали и направления света (то же самое что угол между ними на модули векторов)
        double dot = normal.x * lightDirection.x +
                normal.y * lightDirection.y +
                normal.z * lightDirection.z;

        // Ограничиваем значение от 0.2 до 1.0 для избежания полной темноты
        return Math.max(0.2, dot);
    }

    private Color applyLighting(Color color, double intensity) {
        int r = (int) (color.getRed() * intensity);
        int g = (int) (color.getGreen() * intensity);
        int b = (int) (color.getBlue() * intensity);

        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return new Color(r, g, b);
    }

    private Point3D projectPoint(Point3D p, double fov) {
        double scale = fov / (fov + p.z);
        double x = p.x * scale + width / 2;
        double y = -p.y * scale + height / 2;
        return new Point3D(x, y, p.z);
    }

    private double[] computeBarycentric(int x, int y, Point3D p1, Point3D p2, Point3D p3) {
        double detT = (p2.y - p3.y) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.y - p3.y);

        double alpha = ((p2.y - p3.y) * (x - p3.x) + (p3.x - p2.x) * (y - p3.y)) / detT;
        double beta = ((p3.y - p1.y) * (x - p3.x) + (p1.x - p3.x) * (y - p3.y)) / detT;
        double gamma = 1 - alpha - beta;

        return new double[]{alpha, beta, gamma};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
    }
}

class Camera {
    private double angleX = 0;
    private double angleY = 0;
    private double angleZ = 0;
    private double zoom = 1.0;

    public void rotate(double deltaX, double deltaY, double deltaZ) {
        angleX += deltaX;
        angleY += deltaY;
        angleZ += deltaZ;
    }

    public void zoom(double scale) {
        zoom *= scale;
    }

    public Point3D transform(Point3D p) {
        Point3D rotated = rotateX(p, angleX);
        rotated = rotateY(rotated, angleY);
        rotated = rotateZ(rotated, angleZ);

        return new Point3D(
                rotated.x * zoom,
                rotated.y * zoom,
                rotated.z * zoom
        );
    }

    public Point3D transformNormal(Point3D normal) {
        // Нормали преобразуются обратной транспонированной матрицей
        // Для ортогональной матрицы вращения это то же самое вращение
        return rotateX(rotateY(rotateZ(normal, angleZ), angleY), angleX);
    }

    private Point3D rotateX(Point3D p, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Point3D(
                p.x,
                p.y * cos - p.z * sin,
                p.y * sin + p.z * cos
        );
    }

    private Point3D rotateY(Point3D p, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Point3D(
                p.x * cos + p.z * sin,
                p.y,
                -p.x * sin + p.z * cos
        );
    }

    private Point3D rotateZ(Point3D p, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Point3D(
                p.x * cos - p.y * sin,
                p.x * sin + p.y * cos,
                p.z
        );
    }
}

class Tetrahedron {
    private final Point3D[] vertices;
    private final Point3D[] normals;
    private final Color baseColor;

    public Tetrahedron(Point3D center, double size, Color color) {
        this.baseColor = color;
        this.vertices = new Point3D[4];
        this.normals = new Point3D[4];

        // Вершины тетраэдра
        vertices[0] = new Point3D(center.x, center.y + size, center.z); // верх
        vertices[1] = new Point3D(center.x - size, center.y - size, center.z - size); // основание
        vertices[2] = new Point3D(center.x + size, center.y - size, center.z - size); // основание
        vertices[3] = new Point3D(center.x, center.y - size, center.z + size); // основание

        // Нормали вершин (среднее значение нормалей смежных граней)
        Point3D topNormal = calculateFaceNormal(vertices[0], vertices[1], vertices[2]);
        Point3D face1Normal = calculateFaceNormal(vertices[0], vertices[2], vertices[3]);
        Point3D face2Normal = calculateFaceNormal(vertices[0], vertices[3], vertices[1]);
        Point3D baseNormal = calculateFaceNormal(vertices[1], vertices[3], vertices[2]);

        normals[0] = averageNormals(topNormal, face1Normal, face2Normal).normalize();
        normals[1] = averageNormals(topNormal, face2Normal, baseNormal).normalize();
        normals[2] = averageNormals(topNormal, face1Normal, baseNormal).normalize();
        normals[3] = averageNormals(face1Normal, face2Normal, baseNormal).normalize();
    }

    private Point3D calculateFaceNormal(Point3D a, Point3D b, Point3D c) {
        Point3D ab = new Point3D(b.x - a.x, b.y - a.y, b.z - a.z);
        Point3D ac = new Point3D(c.x - a.x, c.y - a.y, c.z - a.z);

        // Векторное произведение
        double nx = ab.y * ac.z - ab.z * ac.y;
        double ny = ab.z * ac.x - ab.x * ac.z;
        double nz = ab.x * ac.y - ab.y * ac.x;

        return new Point3D(nx, ny, nz).normalize();
    }

    private Point3D averageNormals(Point3D... normals) {
        double x = 0, y = 0, z = 0;
        for (Point3D n : normals) {
            x += n.x;
            y += n.y;
            z += n.z;
        }
        double len = normals.length;
        return new Point3D(x/len, y/len, z/len);
    }

    public List<Triangle3D> getTriangles() {
        List<Triangle3D> triangles = new ArrayList<>();

        // 4 грани тетраэдра с нормалями вершин
        triangles.add(new Triangle3D(
                vertices[0], vertices[1], vertices[2],
                normals[0], normals[1], normals[2],
                baseColor
        ));

        triangles.add(new Triangle3D(
                vertices[0], vertices[2], vertices[3],
                normals[0], normals[2], normals[3],
                baseColor
        ));

        triangles.add(new Triangle3D(
                vertices[0], vertices[3], vertices[1],
                normals[0], normals[3], normals[1],
                baseColor
        ));

        triangles.add(new Triangle3D(
                vertices[1], vertices[3], vertices[2],
                normals[1], normals[3], normals[2],
                baseColor.darker()
        ));

        return triangles;
    }
}

class Point3D {
    public double x, y, z;

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point3D normalize() {
        double length = Math.sqrt(x*x + y*y + z*z);
        if (length == 0) return this;
        return new Point3D(x/length, y/length, z/length);
    }
}

class Triangle3D {
    public Point3D p1, p2, p3;
    public Point3D n1, n2, n3; // Нормали вершин
    public Color color;

    public Triangle3D(Point3D p1, Point3D p2, Point3D p3,
                      Point3D n1, Point3D n2, Point3D n3,
                      Color color) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
        this.color = color;
    }
}