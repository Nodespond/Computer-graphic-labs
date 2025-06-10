from polygon import *
from triang_warping import *
import cv2

# Точки для триангуляционного warping
image = cv2.imread("image_input.jpg")
h, w = image.shape[:2]

src_points = [
    [w // 2, h // 2],  # P0 (центр)
    [100, 100],  # P1
    [w - 100, 100],  # P2
    [w - 50, h - 50],  # P3
    [100, h - 100],  # P4
    [w // 4, h // 2]  # P5
]

dst_points = [
    [w // 2 + 70, h // 2 - 30],  # P0' (центр смещен)
    [150, 200],  # P1'
    [w - 200, 50],  # P2'
    [w - 100, h - 150],  # P3'
    [50, h - 50],  # P4'
    [w // 4 - 50, h // 2 + 100]  # P5'
]

triangles = [
    [0, 1, 2],  # Треугольник P0-P1-P2
    [0, 2, 3],  # Треугольник P0-P2-P3
    [0, 3, 4],  # Треугольник P0-P3-P4
    [0, 4, 5],  # Треугольник P0-P4-P5
    [0, 5, 1]  # Треугольник P0-P5-P1
]

if __name__ == "__main__":
    draw_triangulated_polygon(polygon_vertices, colors=('red', 'green', 'blue'))

    # Создаем тестовое изображение с сеткой
    w, h = 600, 400
    image = np.zeros((h, w, 3), dtype=np.uint8)

    # Рисуем вертикальные и горизонтальные линии
    for x in range(0, w, 50):
        cv2.line(image, (x, 0), (x, h), (255, 255, 255), 1)
    for y in range(0, h, 50):
        cv2.line(image, (0, y), (w, y), (255, 255, 255), 1)

        # 5 точек: центр + 4 угла
        src_points = [
            [w // 2, h // 2],  # Центр (P0)
            [0, 0],  # Левый верх (P1)
            [w, 0],  # Правый верх (P2)
            [w, h],  # Правый низ (P3)
            [0, h]  # Левый низ (P4)
        ]

        # Сильно смещаем точки для драматичного эффекта
        dst_points = [
            [w // 2 + 80, h // 2 - 50],  # Центр смещен вправо-вверх
            [100, 50],  # Левый верх смещен внутрь
            [w - 50, 70],  # Правый верх смещен вниз
            [w - 100, h - 80],  # Правый низ смещен влево
            [120, h - 60]  # Левый низ смещен вправо
        ]

        # 4 треугольника (веером из центра)
        triangles = [
            [0, 1, 2],  # Верхние треугольники
            [0, 2, 3],  # Правые
            [0, 3, 4],  # Нижние
            [0, 4, 1]  # Левые
        ]

    # Применяем с визуализацией сетки
    strong_triangulation_warp(
        "image_input.jpg",
        src_points=src_points,
        dst_points=dst_points,
        triangles=triangles,
        output_path="triangl_warp.jpg",
        show_grid=True
    )