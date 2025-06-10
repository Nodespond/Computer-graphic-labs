import colorsys

from PIL import Image
import numpy as np


def dither_errordiff(input_image_path, output_image_path, method):
    # загружаем и преобразуем в grayscale
    image = Image.open(input_image_path).convert('L')
    pixels = np.array(image, dtype=np.float32) / 255.0  # нормализация в [0, 1]
    width, height = image.size

    if method == 'dither':
        # dithering с матрицей Байера 4x4
        bayer_matrix = np.array([
            [0, 8, 2, 10],
            [12, 4, 14, 6],
            [3, 11, 1, 9],
            [15, 7, 13, 5]
        ]) / 16.0  # нормализация матрицы

        dithered = np.zeros_like(pixels)
        for y in range(height):
            for x in range(width):
                threshold = bayer_matrix[y % 4, x % 4]
                dithered[y, x] = 1.0 if pixels[y, x] > threshold else 0.0
    elif method == 'error_diffusion':
        # error diffusion
        dithered = pixels.copy()
        for y in range(height):
            for x in range(width):
                old_pixel = dithered[y, x]
                new_pixel = 1.0 if old_pixel > 0.5 else 0.0
                dithered[y, x] = new_pixel
                error = old_pixel - new_pixel

                # кидаем ошибку на соседние пиксели
                if x + 1 < width:
                    dithered[y, x + 1] += error * 7 / 16
                if y + 1 < height:
                    if x - 1 >= 0:
                        dithered[y + 1, x - 1] += error * 3 / 16
                    dithered[y + 1, x] += error * 5 / 16
                    if x + 1 < width:
                        dithered[y + 1, x + 1] += error * 1 / 16
    else:
        raise ValueError("Метод должен быть 'dither' или 'error_diffusion'")

    # делаем обратно в 0-255 и сохраняем
    output = Image.fromarray((dithered * 255).astype(np.uint8))
    output.save(output_image_path)

# RGB correction with LUT
def rgb_correction(
        input_image_path: str,
        output_image_path: str,
        red_lut: callable = lambda x: x,
        green_lut: callable = lambda x: x,
        blue_lut: callable = lambda x: x,
):

    img = Image.open(input_image_path)
    pixels = np.array(img, dtype=np.float32)

    for y in range(pixels.shape[0]):
        for x in range(pixels.shape[1]):
            r, g, b = pixels[y, x]
            pixels[y, x] = [
                np.clip(red_lut(r), 0, 255),
                np.clip(green_lut(g), 0, 255),
                np.clip(blue_lut(b), 0, 255),
            ]

    corrected_img = Image.fromarray(pixels.astype(np.uint8))
    corrected_img.save(output_image_path)

# HCV correction with LUT
def hsv_correction(
        input_image_path: str,
        output_image_path: str,
        hue_lut: callable = lambda x: x,        # оттенок
        saturation_lut: callable = lambda x: x, # насыщенность
        value_lut: callable = lambda x: x,      # яркость
):

    img = Image.open(input_image_path)
    pixels = np.array(img, dtype=np.float32) / 255.0

    for y in range(pixels.shape[0]):
        for x in range(pixels.shape[1]):
            r, g, b = pixels[y, x]
            h, s, v = colorsys.rgb_to_hsv(r, g, b)

            #применение lut и нормализация
            h = np.clip(hue_lut(h * 360) / 360.0, 0.0, 1.0)
            s = np.clip(saturation_lut(s * 100) / 100.0, 0.0, 1.0)
            v = np.clip(value_lut(v * 100) / 100.0, 0.0, 1.0)

            #конвертируем в rgb
            r, g, b = colorsys.hsv_to_rgb(h, s, v)
            pixels[y, x] = [r * 255, g * 255, b * 255]

    corrected_img = Image.fromarray(pixels.astype(np.uint8))
    corrected_img.save(output_image_path)

if __name__ == "__main__":
    print("Начало обработки изображений...")
    print("Dither......")
    dither_errordiff("image_input.jpg", "bayer_output.jpg", 'dither')
    print("Error diffusion......")
    dither_errordiff("image_input.jpg", "floyd_output.jpg", 'error_diffusion')

    print("RGB correction") # color inversion
    rgb_correction(
        "image_input.jpg",
        "rgb_corr.jpg",
        red_lut=lambda x:255-x,
        green_lut=lambda x:255-x,
        blue_lut=lambda x:255-x
    )
    print("HCV correction") #насыщенность на +50%
    hsv_correction(
        "image_input.jpg",
        "hcv_corr.jpg",
        saturation_lut=lambda s: min(s * 1.5, 100)
    )

    print("Всё выполнено!")