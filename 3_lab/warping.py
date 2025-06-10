import cv2
import numpy as np

# Загрузка изображения
image = cv2.imread("image_input.jpg")
h, w = image.shape[:2]

# Исходные точки (углы изображения)
src_points = np.float32([[0, 0], [w, 0], [w, h], [0, h]])

# Целевые точки (деформированные)
dst_points = np.float32([[120, 50], [w-80, 370], [w-70, h-90], [330, h-30]])

# Находим матрицу билинейного преобразования
M, _ = cv2.findHomography(src_points, dst_points)

# Применяем преобразование
warped = cv2.warpPerspective(image, M, (w, h))

# Сохраняем результат
cv2.imwrite("bilinear_warp.jpg", warped)