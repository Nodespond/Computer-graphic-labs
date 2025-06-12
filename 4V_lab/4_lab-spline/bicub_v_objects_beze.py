import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D


def bernstein_poly(i, n, t):
    """Вычисление полинома Бернштейна"""
    from math import comb
    return comb(n, i) * (t ** i) * ((1 - t) ** (n - i))


def bezier_surface(points, num_samples=20):
    """Построение бикубической поверхности Безье"""
    n = len(points) - 1
    m = len(points[0]) - 1

    u = np.linspace(0, 1, num_samples)
    v = np.linspace(0, 1, num_samples)

    surface = np.zeros((num_samples, num_samples, 3))

    for i in range(num_samples):
        for j in range(num_samples):
            for k in range(n + 1):
                for l in range(m + 1):
                    bu = bernstein_poly(k, n, u[i])
                    bv = bernstein_poly(l, m, v[j])
                    surface[i, j] += bu * bv * points[k, l]

    return surface


def read_control_points(filename):
    """Чтение контрольных точек из файла"""
    with open(filename, 'r') as f:
        lines = f.readlines()

    points = []
    for line in lines:
        if line.strip():
            x, y, z = map(float, line.strip().split())
            points.append([x, y, z])

    return np.array(points).reshape(4, 4, 3)


def plot_cube(filenames, num_samples=15):
    """Построение куба из 6 поверхностей Безье"""
    fig = plt.figure(figsize=(10, 8))
    ax = fig.add_subplot(111, projection='3d')

    for filename in filenames:
        # Читаем контрольные точки из файла
        control_points = read_control_points(filename)

        # Строим поверхность Безье
        surface = bezier_surface(control_points, num_samples)

        # Рисуем поверхность
        ax.plot_surface(
            surface[:, :, 0], surface[:, :, 1], surface[:, :, 2],
            color='b', alpha=0.6, rstride=1, cstride=1
        )

        # Рисуем контрольные точки
        ax.scatter(
            control_points[:, :, 0], control_points[:, :, 1], control_points[:, :, 2],
            color='r', s=20, depthshade=True
        )

        # Рисуем контрольную сетку
        for i in range(4):
            ax.plot(control_points[i, :, 0], control_points[i, :, 1], control_points[i, :, 2], 'r-', linewidth=0.5)
            ax.plot(control_points[:, i, 0], control_points[:, i, 1], control_points[:, i, 2], 'r-', linewidth=0.5)

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')
    ax.set_title('Куб из бикубических поверхностей Безье')
    plt.tight_layout()
    plt.show()


def main():
    # Список файлов с контрольными точками (по одному на грань)
    face_files = [
        "front.txt",
        "back.txt",
        "right.txt",
        "left.txt",
        "top.txt",
        "bottom.txt"
    ]

    # Построение куба
    plot_cube(face_files, num_samples=15)


if __name__ == "__main__":
    main()