import sys
import socket
from PySide6.QtWidgets import QApplication, QWidget
from PySide6.QtGui import QPainter, QPen, QMouseEvent
from PySide6.QtCore import Qt, QPoint


class ClientCanvas(QWidget):
    def __init__(self, sock):
        super().__init__()
        self.setWindowTitle("Client - Draw Here")
        self.setGeometry(100, 100, 800, 600)
        self.sock = sock
        self.drawing = False
        self.points = []

    def mousePressEvent(self, event: QMouseEvent):
        if event.button() == Qt.LeftButton:
            self.drawing = True
            self.points.append(event.position().toPoint())
            self.send_point(event.position().toPoint())

    def mouseMoveEvent(self, event: QMouseEvent):
        if self.drawing:
            self.points.append(event.position().toPoint())
            self.send_point(event.position().toPoint())
            self.update()

    def mouseReleaseEvent(self, event: QMouseEvent):
        self.drawing = False
        self.points.append(None)
        self.sock.sendall(b"NONE\n")
        self.update()

    def send_point(self, point: QPoint):
        message = f"{point.x()},{point.y()}\n".encode()
        self.sock.sendall(message)

    def paintEvent(self, event):
        painter = QPainter(self)
        pen = QPen(Qt.blue, 3)
        painter.setPen(pen)
        for p1, p2 in zip(self.points, self.points[1:]):
            if p1 and p2:
                painter.drawLine(p1, p2)


def connect_to_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(("localhost", 2570))
    return sock


if __name__ == "__main__":
    sock = connect_to_server()
    app = QApplication(sys.argv)
    canvas = ClientCanvas(sock)
    canvas.show()
    sys.exit(app.exec())
