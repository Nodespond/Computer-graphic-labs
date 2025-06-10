import matplotlib.pyplot as plt
import numpy as np
from matplotlib.patches import Polygon

def draw_triangulated_polygon(vertices, colors=('red', 'green', 'blue')):

    if len(vertices) < 3:
        raise ValueError("Полигон должен иметь как минимум 3 вершины")

    # Выбираем 3 точки для треугольников: первая, средняя и последняя вершина
    idx1 = 0
    idx2 = len(vertices) // 2
    idx3 = -1

    point1 = vertices[idx1]
    point2 = vertices[idx2]
    point3 = vertices[idx3]
    center = np.mean(vertices, axis=0)

    # Создаем 3 треугольника
    triangle1 = np.array([point1, point2, center])
    triangle2 = np.array([point2, point3, center])
    triangle3 = np.array([point3, point1, center])

    triangles = [triangle1, triangle2, triangle3]

    # Визуализация
    fig, ax = plt.subplots()
    patches = []

    # Добавляем треугольники на график с заданными цветами
    for i, triangle in enumerate(triangles):
        polygon = Polygon(triangle, closed=True, color=colors[i])
        patches.append(polygon)
        ax.add_patch(polygon)

    # Настройка графика
    all_points = np.concatenate([triangle1, triangle2, triangle3])
    min_x, min_y = np.min(all_points, axis=0)
    max_x, max_y = np.max(all_points, axis=0)

    ax.set_xlim(min_x - 1, max_x + 1)
    ax.set_ylim(min_y - 1, max_y + 1)
    ax.set_aspect('equal')
    plt.title("Полигон с 3-мя треугольниками")
    plt.show()

polygon_vertices = np.array([
    [0, 0],
    [4, 0],
    [5, 2],
    [3, 4],
    [1, 3]
])
