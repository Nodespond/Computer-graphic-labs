import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class FFD extends JFrame {
    private static final int WIDTH = 1300;
    private static final int HEIGHT = 1080;
    private static final int GRID_SIZE = 3; // 3x3 grid
    private static final int POINT_RADIUS = 8;

    private BufferedImage originalImage;
    private BufferedImage deformedImage;
    private List<Point> controlPoints = new ArrayList<>();
    private Point selectedPoint = null;

    public FFD() {
        setTitle("FFD");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Загрузка изображения
        try {
            originalImage = ImageIO.read(new File("ayanami_img.png")); // Замените на ваш файл
            deformedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
        } catch (IOException e) {
            e.printStackTrace();
            originalImage = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = originalImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 400, 300);
            g.setColor(Color.BLUE);
            g.drawString("No image loaded", 150, 150);
            g.dispose();
            deformedImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
        }

        // Инициализация контрольных точек (3x3 grid)
        initializeControlPoints();

        // Первоначальное применение FFD
        applyFFD();

        // Панель управления
        JPanel controlPanel = new JPanel();
        JButton resetButton = new JButton("Сброс");

        resetButton.addActionListener(e -> {
            initializeControlPoints();
            applyFFD();
            repaint();
        });

        controlPanel.add(resetButton);
        add(controlPanel, BorderLayout.NORTH);

        // Основная панель для рисования
        JPanel drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Рисуем деформированное изображение
                if (deformedImage != null) {
                    g2d.drawImage(deformedImage, 50, 50, null);
                }

                // Рисуем контрольные точки
                g2d.setColor(Color.RED);
                for (Point p : controlPoints) {
                    g2d.fillOval(p.x - POINT_RADIUS, p.y - POINT_RADIUS, 2 * POINT_RADIUS, 2 * POINT_RADIUS);
                }

                // Рисуем сетку
                g2d.setColor(new Color(255, 0, 0, 100));
                drawBezierGrid(g2d);
            }
        };

        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Проверяем, не нажали ли на существующую точку
                for (Point p : controlPoints) {
                    if (e.getPoint().distance(p) <= POINT_RADIUS) {
                        selectedPoint = p;
                        return;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectedPoint = null;
            }
        });

        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedPoint != null) {
                    selectedPoint.setLocation(e.getPoint());
                    applyFFD();
                    repaint();
                }
            }
        });

        add(drawingPanel, BorderLayout.CENTER);
    }

    private void initializeControlPoints() {
        controlPoints.clear();

        int imgWidth = originalImage.getWidth();
        int imgHeight = originalImage.getHeight();
        int startX = 50;
        int startY = 50;

        // Создаем равномерную сетку 3x3
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int x = startX + (imgWidth / (GRID_SIZE - 1)) * j;
                int y = startY + (imgHeight / (GRID_SIZE - 1)) * i;
                controlPoints.add(new Point(x, y));
            }
        }
    }

    private void drawBezierGrid(Graphics2D g2d) {
        // Рисуем кривые Безье по горизонтали
        for (int row = 0; row < GRID_SIZE; row++) {
            Point p0 = controlPoints.get(row * GRID_SIZE);
            Point p1 = controlPoints.get(row * GRID_SIZE + 1);
            Point p2 = controlPoints.get(row * GRID_SIZE + 2);

            Path2D path = new Path2D.Double();
            path.moveTo(p0.x, p0.y);

            for (double t = 0.1; t <= 1.0; t += 0.1) {
                Point p = evaluateQuadraticBezier(t, p0, p1, p2);
                path.lineTo(p.x, p.y);
            }

            g2d.draw(path);
        }

        // Рисуем кривые Безье по вертикали
        for (int col = 0; col < GRID_SIZE; col++) {
            Point p0 = controlPoints.get(col);
            Point p1 = controlPoints.get(col + GRID_SIZE);
            Point p2 = controlPoints.get(col + 2 * GRID_SIZE);

            Path2D path = new Path2D.Double();
            path.moveTo(p0.x, p0.y);

            for (double t = 0.1; t <= 1.0; t += 0.1) {
                Point p = evaluateQuadraticBezier(t, p0, p1, p2);
                path.lineTo(p.x, p.y);
            }

            g2d.draw(path);
        }
    }

    private Point evaluateQuadraticBezier(double t, Point p0, Point p1, Point p2) {
        double mt = 1 - t;
        double x = mt * mt * p0.x + 2 * mt * t * p1.x + t * t * p2.x;
        double y = mt * mt * p0.y + 2 * mt * t * p1.y + t * t * p2.y;
        return new Point((int) x, (int) y);
    }

    private void applyFFD() {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // Координаты исходной сетки (до деформации)
        int startX = 50;
        int startY = 50;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Нормализованные координаты в сетке [0..1]
                double u = (double) x / (width - 1);
                double v = (double) y / (height - 1);

                // Вычисляем новые координаты с использованием биквадратичной поверхности Безье
                Point newPoint = evaluateBiquadraticBezierSurface(u, v);

                // Проверяем границы
                if (newPoint.x >= 0 && newPoint.x < width && newPoint.y >= 0 && newPoint.y < height) {
                    deformedImage.setRGB(x, y, originalImage.getRGB(newPoint.x, newPoint.y));
                } else {
                    deformedImage.setRGB(x, y, 0); // Прозрачный/черный за границами
                }
            }
        }
    }

    private Point evaluateBiquadraticBezierSurface(double u, double v) {
        // Вычисляем точки вдоль оси u
        Point p0 = evaluateQuadraticBezier(u,
                controlPoints.get(0), controlPoints.get(1), controlPoints.get(2));
        Point p1 = evaluateQuadraticBezier(u,
                controlPoints.get(3), controlPoints.get(4), controlPoints.get(5));
        Point p2 = evaluateQuadraticBezier(u,
                controlPoints.get(6), controlPoints.get(7), controlPoints.get(8));

        // Интерполируем вдоль оси v
        return evaluateQuadraticBezier(v, p0, p1, p2);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FFD demo = new FFD();
            demo.setVisible(true);
        });
    }
}
