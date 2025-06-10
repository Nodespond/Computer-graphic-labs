import cv2
import numpy as np


def draw_triangulation(image, points, triangles, color=(0, 255, 0), thickness=1):
    vis = image.copy()
    for tri in triangles:
        pt1 = tuple(points[tri[0]].astype(int))
        pt2 = tuple(points[tri[1]].astype(int))
        pt3 = tuple(points[tri[2]].astype(int))

        cv2.line(vis, pt1, pt2, color, thickness)
        cv2.line(vis, pt2, pt3, color, thickness)
        cv2.line(vis, pt3, pt1, color, thickness)
    return vis


def strong_triangulation_warp(
        input_image,
        src_points, #5 исходных точек
        dst_points, # 5 целевых точек
        triangles,  #треугольники
        output_path,
        show_grid=False, grid_color=(0, 255, 255)):

    # Загрузка изображения
    if isinstance(input_image, str):
        image = cv2.imread(input_image)
        if image is None:
            raise ValueError("Не удалось загрузить изображение")
    else:
        image = input_image.copy()

    h, w = image.shape[:2]

    # Конвертация точек
    src_points = np.float32(src_points)
    dst_points = np.float32(dst_points)

    # Создаем пустое изображение для результата
    warped = np.zeros_like(image)

    # Обрабатываем каждый треугольник
    for tri in triangles:
        src_tri = src_points[tri]
        dst_tri = dst_points[tri]

        # Вычисляем аффинное преобразование
        M = cv2.getAffineTransform(src_tri, dst_tri)

        # Деформируем треугольник
        warped_tri = cv2.warpAffine(image, M, (w, h))

        # Создаем маску для треугольника
        mask = np.zeros((h, w), dtype=np.uint8)
        cv2.fillConvexPoly(mask, dst_tri.astype(int), 1)

        # Накладываем результат
        warped[mask == 1] = warped_tri[mask == 1]

    # Визуализация результата
    if show_grid:
        dst_vis = draw_triangulation(warped, dst_points, triangles, grid_color)
        cv2.imwrite("warped_grid.jpg", dst_vis)

    cv2.imwrite(output_path, warped)