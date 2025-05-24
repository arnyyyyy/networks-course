import sys
import socket
import struct
import time
from PySide6.QtWidgets import QApplication, QWidget, QVBoxLayout, QLabel, QLineEdit, QPushButton

class TCPReceiver(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("TCP Receiver")
        layout = QVBoxLayout()

        self.ip = QLineEdit("127.0.0.1")
        self.port = QLineEdit("8080")
        self.cur_log = QLabel("Waiting to start...")
        self.speed = QLabel("Speed:")
        self.received = QLabel("Packets received:")
        self.lost = QLabel("Packets lost:")

        self.receive_button = QPushButton("Start Listening")
        layout.addWidget(QLabel("IP"))
        layout.addWidget(self.ip)
        layout.addWidget(QLabel("Port"))
        layout.addWidget(self.port)
        layout.addWidget(self.cur_log)
        layout.addWidget(self.speed)
        layout.addWidget(self.received)
        layout.addWidget(self.lost)
        layout.addWidget(self.receive_button)

        self.receive_button.clicked.connect(self.receive_data)
        self.setLayout(layout)

    def receive_data(self):
        ip = self.ip.text()
        port = int(self.port.text())
        packet_size = 1024
        header_size = 4 + 8

        self.cur_log.setText("Waiting for connection...")

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.bind((ip, port))
                s.listen(1)
                conn, addr = s.accept()
                self.cur_log.setText(f"Client connected: {addr}")

                with conn:
                    buffer = b""
                    received_packets = set()
                    count = 0
                    start = time.time()

                    while True:
                        data = conn.recv(4096)
                        if not data:
                            break
                        buffer += data

                        while len(buffer) >= packet_size:
                            packet = buffer[:packet_size]
                            buffer = buffer[packet_size:]

                            if len(packet) < header_size:
                                continue

                            pkt_num, pkt_time = struct.unpack('!Id', packet[:header_size])
                            received_packets.add(pkt_num)
                            count += 1
                            self.cur_log.setText(f"Packets received: {count}")

                    end = time.time()
                    elapsed = end - start if end > start else 1
                    speed = (count * packet_size) / elapsed / 1024

                    if received_packets:
                        max_packet = max(received_packets)
                        lost = max_packet + 1 - len(received_packets)
                    else:
                        lost = 0

                    self.speed.setText(f"Speed: {speed:.2f} KB/s")
                    self.received.setText(f"Packets received: {count}")
                    self.lost.setText(f"Packets lost: {lost}")
                    self.cur_log.setText("Reception complete.")
        except Exception as e:
            self.cur_log.setText(f"Error: {e}")

if __name__ == "__main__":
    app = QApplication(sys.argv)
    receiver = TCPReceiver()
    receiver.show()
    sys.exit(app.exec())
