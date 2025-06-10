import math
from typing import Tuple, List
import pygame
from collections import deque

pygame.init()
screen_width = 800
screen_height = 800
screen = pygame.display.set_mode((screen_width, screen_height))
pygame.display.set_caption("Растровая графика")

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
RED = (255, 0, 0)
GREEN = (0, 255, 0)
BLUE = (0, 0, 255)
YELLOW = (255, 255, 0)

def draw_pixel(x: int, y: int, color: Tuple[int, int, int] = WHITE):
    pygame.draw.rect(screen, color, (x, y, 1, 1))


#отрезок (Алгоритм Брезенхема)
def draw_line(x1: int, y1: int, x2: int, y2: int, color: Tuple[int, int, int] = WHITE):

    dx = abs(x2 - x1)
    dy = abs(y2 - y1)
    sx = 1 if x1 < x2 else -1
    sy = 1 if y1 < y2 else -1
    err = (dx - dy) / 2

    while True:
        draw_pixel(x1, y1, color)
        if x1 == x2 and y1 == y2:
            break
        e2 = err
        if e2 > -dx:
            err -= dy
            x1 += sx
        if e2 < dy:
            err += dx
            y1 += sy


#окружность (Алгоритм Брезенхема)
def draw_circle(x0: int, y0: int, radius: int, color: Tuple[int, int, int] = WHITE):

    x = 0
    y = radius
    d = 3 - 2 * radius

    def plot_circle_points(cx: int, cy: int, x: int, y: int):
        draw_pixel(cx + x, cy + y, color)
        draw_pixel(cx - x, cy + y, color)
        draw_pixel(cx + x, cy - y, color)
        draw_pixel(cx - x, cy - y, color)
        draw_pixel(cx + y, cy + x, color)
        draw_pixel(cx - y, cy + x, color)
        draw_pixel(cx + y, cy - x, color)
        draw_pixel(cx - y, cy - x, color)

    plot_circle_points(x0, y0, x, y)
    while y >= x:
        x += 1
        if d > 0:
            y -= 1
            d = d + 4 * (x - y) + 10
        else:
            d = d + 4 * x + 6
        plot_circle_points(x0, y0, x, y)


#круг (Алгоритм Брезенхема с заполнением)
def draw_filled_circle(x0: int, y0: int, radius: int, color: Tuple[int, int, int] = WHITE):

    for y in range(-radius, radius + 1):
        for x in range(-radius, radius + 1):
            if x * x + y * y <= radius * radius:
                draw_pixel(x0 + x, y0 + y, color)




def get_pixel_color(x: int, y: int) -> Tuple[int, int, int]:

    if 0 <= x < screen_width and 0 <= y < screen_height:
        return screen.get_at((x, y))[:3]
    else:
        return BLACK

def is_valid_pixel(x: int, y: int) -> bool:
    return 0 <= x < screen_width and 0 <= y < screen_height

#заливка границы
def flood_fill_iterative(x: int, y: int, old_color: Tuple[int, int, int], new_color: Tuple[int, int, int]):

    if not is_valid_pixel(x, y):
        return

    if get_pixel_color(x, y) != old_color:
        return

    queue = deque([(x, y)])

    while queue:
        x, y = queue.popleft()
        if get_pixel_color(x, y) == old_color:
            draw_pixel(x, y, new_color)

            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = x + dx, y + dy
                if is_valid_pixel(nx, ny) and get_pixel_color(nx, ny) == old_color:
                    queue.append((nx, ny))

#заливка области
def fill_inside_shape(x: int, y: int, boundary_color: Tuple[int, int, int], fill_color: Tuple[int, int, int]):

    #boundary_color: Цвет границы
    #fill_color: Цвет, которым нужно заполнить область.
    if not is_valid_pixel(x, y):
        return

    queue = deque([(x, y)])

    while queue:
        x, y = queue.popleft()
        current_color = get_pixel_color(x, y)

        if current_color != boundary_color and current_color != fill_color:
            draw_pixel(x, y, fill_color)

            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                nx, ny = x + dx, y + dy
                if is_valid_pixel(nx, ny):
                    neighbor_color = get_pixel_color(nx, ny)
                    if neighbor_color != boundary_color and neighbor_color != fill_color:
                        queue.append((nx, ny))
#загрузка шрифтов
def load_font(filename: str) -> dict:

    font = {}
    try:
        with open(filename, 'r') as f:
            line = f.read().strip()
            hex_values = line.split()
            glyph_size = 8

            char_ranges = [
                (65, 91),    # A-Z
                (48, 58),    # 0-9
                (32, 32),    #
            ]

            current_index = 0
            for start, end in char_ranges:
                for char_code in range(start, end):
                    glyph = []
                    for j in range(glyph_size):
                        glyph.append(int(hex_values[current_index], 16))
                        current_index += 1
                    font[chr(char_code)] = glyph

    except FileNotFoundError:
        print(f"Ошибка: файл шрифта '{filename}' не найден.")
        return {}
    except Exception as e:
        print(f"Ошибка при загрузке шрифта: {e}")
        return {}

    return font
#отрисовка символа
def draw_char(x: int, y: int, char: str, font: dict, color: Tuple[int, int, int] = WHITE, scale: int = 1):

    if char not in font:
        return

    glyph = font[char]
    for row_index, row_byte in enumerate(glyph):
        for bit_index in range(8):
            if (row_byte >> (7 - bit_index)) & 1:
                pygame.draw.rect(screen, color,
                                 (x + bit_index * scale,
                                  y + row_index * scale,
                                  scale, scale))
#отрисовка текста
def draw_text(x: int, y: int, text: str, font: dict, color: Tuple[int, int, int] = WHITE, space_width: int = 8, scale: int = 1):

    current_x = x
    for char in text:
        if char == ' ':
            current_x += space_width * scale
        else:
            draw_char(current_x, y, char, font, color, scale)
            current_x += 8 * scale


if __name__ == '__main__':
    #рисуем квадраты
    draw_line(50, 100, 300, 100, WHITE)
    draw_line(50, 300, 300, 300, WHITE)
    draw_line(50, 100, 50, 300, WHITE)
    draw_line(300, 100, 300, 300, WHITE)
    pygame.display.flip()
    draw_line(350, 100, 500, 100, WHITE)
    draw_line(350, 300, 500, 300, WHITE)
    draw_line(350, 100, 350, 300, WHITE)
    draw_line(500, 100, 500, 300, WHITE)
    pygame.display.flip()
    #закраска границы фигуры
    flood_fill_iterative(50, 100, WHITE, YELLOW)  #первый квадрат - желтая границы
    fill_inside_shape(51, 101, YELLOW, RED) #а затем заливка красным

    #окружность
    draw_circle(100, 400, 50, GREEN)

    #круг
    draw_filled_circle(250, 400, 50, BLUE)

    #шрифты растровые
    font = load_font("font.txt")

    if font:
        draw_text(50, 500, "G O I D A ", font, WHITE, space_width=8, scale=3)
        draw_text(50, 600, "1 3 3 7", font, WHITE,space_width=8, scale=3)

    pygame.display.flip()

    running = True
    while running:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
    pygame.quit()