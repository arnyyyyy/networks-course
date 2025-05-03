import sys
import socket
import threading
from PySide6.QtWidgets import QApplication, QWidget
from PySide6.QtGui import QPainter, QPen
from PySide6.QtCore import Qt, QPoint


class ServerCanvas(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Server - Remote Drawing")
        self.setGeometry(100, 100, 800, 600)
        self.points = []

    def paintEvent(self, event):
        painter = QPainter(self)
        pen = QPen(Qt.black, 3)
        painter.setPen(pen)
        for p1, p2 in zip(self.points, self.points[1:]):
            if p1 and p2:
                painter.drawLine(p1, p2)

    def add_point(self, x, y):
        self.points.append(QPoint(x, y))
        self.update()


def handle_client(conn, canvas):
    buffer = ""
    while True:
        data = conn.recv(1024).decode()
        if not data:
            break
        buffer += data
        while "\n" in buffer:
            line, buffer = buffer.split("\n", 1)
            if line == "NONE":
                canvas.points.append(None)
            else:
                x, y = map(int, line.strip().split(','))
                canvas.add_point(x, y)


def start_server(canvas):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(("0.0.0.0", 2570))
    server.listen(1)
    conn, addr = server.accept()
    threading.Thread(target=handle_client, args=(conn, canvas), daemon=True).start()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    canvas = ServerCanvas()
    canvas.show()
    server_thread = threading.Thread(target=start_server, args=(canvas,), daemon=True)
    server_thread.start()

    exit_code = app.exec()

    sys.exit(exit_code)
