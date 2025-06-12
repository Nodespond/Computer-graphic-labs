import numpy as np
import matplotlib.pyplot as plt
from matplotlib.widgets import Button, Slider
from matplotlib.path import Path
from matplotlib.patches import PathPatch


class ParametricCurveVisualizer:
    def __init__(self):
        self.weights = []
        self.selected_point = None
        self.control_points = []
        self.curve_type = "B-spline"
        self.fig, self.ax = plt.subplots(figsize=(10, 6))
        self.setup_ui()
        self.fig.canvas.mpl_connect('button_press_event', self.on_click)
        self.fig.canvas.mpl_connect('motion_notify_event', self.on_motion)
        self.fig.canvas.mpl_connect('button_release_event', self.on_release)
        self.dragging_point = None
        self.update_plot()

    def setup_ui(self):
        # Настройка области графика
        self.ax.set_xlim(0, 10)
        self.ax.set_ylim(0, 10)
        self.ax.set_title('Параметрические кубические кривые')
        self.ax.grid(True)

        # Создание кнопок
        ax_clear = plt.axes([0.7, 0.02, 0.1, 0.05])
        ax_bspline = plt.axes([0.81, 0.02, 0.1, 0.05])
        ax_catmull = plt.axes([0.81, 0.08, 0.1, 0.05])

        self.btn_clear = Button(ax_clear, 'Очистить')
        self.btn_bspline = Button(ax_bspline, 'B-spline')
        self.btn_catmull = Button(ax_catmull, 'Catmull-Rom')

        self.btn_clear.on_clicked(self.clear_points)
        self.btn_bspline.on_clicked(lambda event: self.set_curve_type("B-spline"))
        self.btn_catmull.on_clicked(lambda event: self.set_curve_type("Catmull-Rom"))

        ax_nurbs = plt.axes([0.7, 0.08, 0.1, 0.05])
        ax_weight = plt.axes([0.2, 0.02, 0.4, 0.03])

        self.btn_nurbs = Button(ax_nurbs, 'NURBS')
        self.slider_weight = Slider(ax_weight, 'Вес точки', 0.1, 5.0, valinit=1.0)

        self.btn_nurbs.on_clicked(lambda event: self.set_curve_type("NURBS"))
        self.slider_weight.on_changed(self.update_weight)

    def on_click(self, event):
        if event.inaxes != self.ax or event.button != 1:
            return

        # Проверяем, кликнули ли на существующую точку
        for i, point in enumerate(self.control_points):
            if np.linalg.norm(np.array([event.xdata, event.ydata]) - point) < 0.5:
                self.dragging_point = i
                self.selected_point = i
                self.slider_weight.set_val(self.weights[i])
                return

        # Добавляем новую точку
        if event.xdata is not None and event.ydata is not None:
            self.control_points.append(np.array([event.xdata, event.ydata]))
            self.update_plot()
            self.weights.append(1.0)  # Вес по умолчанию
            self.selected_point = len(self.control_points) - 1
            self.slider_weight.set_val(1.0)

    def on_motion(self, event):
        if self.dragging_point is None or event.inaxes != self.ax:
            return

        if event.xdata is not None and event.ydata is not None:
            self.control_points[self.dragging_point] = np.array([event.xdata, event.ydata])
            self.update_plot()

    def on_release(self, event):
        self.dragging_point = None

    def clear_points(self, event):
        self.control_points = []
        self.weights = []
        self.selected_point = None
        self.update_plot()

    def set_curve_type(self, curve_type):
        self.curve_type = curve_type
        self.update_plot()

    def calculate_curve(self):
        if len(self.control_points) < 4:
            return []

        points = np.array(self.control_points)
        # Добавляем единичные веса для новых точек, если нужно
        weights = np.array(self.weights + [1.0] * (len(self.control_points) - len(self.weights)))
        curve_points = []

        for i in range(1, len(points) - 2):
            for t in np.linspace(0, 1, 20):
                if self.curve_type == "B-spline":
                    basis = self.bspline_basis(t)
                    # Взвешенный B-spline
                    weighted_basis = basis * weights[i - 1:i + 3]
                    weighted_points = points[i - 1:i + 3] * weighted_basis[:, np.newaxis]
                    p = np.sum(weighted_points, axis=0) / np.sum(weighted_basis)

                elif self.curve_type == "Catmull-Rom":
                    basis = self.catmull_rom_basis(t)
                    # Взвешенный Catmull-Rom
                    weighted_basis = basis * weights[i - 1:i + 3]
                    weighted_points = points[i - 1:i + 3] * weighted_basis[:, np.newaxis]
                    p = np.sum(weighted_points, axis=0) / np.sum(weighted_basis)

                elif self.curve_type == "NURBS":
                    basis = self.bspline_basis(t)
                    # Классический NURBS
                    weighted_basis = basis * weights[i - 1:i + 3]
                    weighted_points = points[i - 1:i + 3] * weights[i - 1:i + 3, np.newaxis]
                    p = np.sum(weighted_points * basis[:, np.newaxis], axis=0) / np.sum(weighted_basis)

                curve_points.append(p)

        return np.array(curve_points)

    @staticmethod
    def bspline_basis(t):
        t2 = t * t
        t3 = t2 * t
        return np.array([
            (-t3 + 3 * t2 - 3 * t + 1) / 6,
            (3 * t3 - 6 * t2 + 4) / 6,
            (-3 * t3 + 3 * t2 + 3 * t + 1) / 6,
            t3 / 6
        ])

    @staticmethod
    def catmull_rom_basis(t):
        t2 = t * t
        t3 = t2 * t
        return np.array([
            -0.5 * t3 + t2 - 0.5 * t,
            1.5 * t3 - 2.5 * t2 + 1,
            -1.5 * t3 + 2 * t2 + 0.5 * t,
            0.5 * t3 - 0.5 * t2
        ])

    def update_plot(self):
        self.ax.clear()
        self.ax.set_xlim(0, 10)
        self.ax.set_ylim(0, 10)
        self.ax.grid(True)
        self.ax.set_title(f'{self.curve_type} кривая (веса точек активны)')

        if self.control_points:
            points = np.array(self.control_points)
            self.ax.scatter(points[:, 0], points[:, 1], color='red', s=50)
            self.ax.plot(points[:, 0], points[:, 1], 'k--', alpha=0.5)

            # Добавляем подписи весов
            weights = self.weights + [1.0] * (len(self.control_points) - len(self.weights))
            for i, (point, weight) in enumerate(zip(points, weights)):
                self.ax.text(point[0], point[1], f'w={weight:.2f}', fontsize=8)

            curve = self.calculate_curve()
            if len(curve) > 0:
                self.ax.plot(curve[:, 0], curve[:, 1], 'b-', linewidth=2)

        self.fig.canvas.draw()

    def update_weight(self, val):
        if self.selected_point is not None and 0 <= self.selected_point < len(self.weights):
            self.weights[self.selected_point] = val
            self.update_plot()


if __name__ == "__main__":
    visualizer = ParametricCurveVisualizer()
    plt.show()