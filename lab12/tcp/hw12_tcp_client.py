import sys
import socket
import random
import struct
import time
from PySide6.QtWidgets import QApplication, QWidget, QVBoxLayout, QLabel, QLineEdit, QPushButton

class TCPSender(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("TCP Sender")
        layout = QVBoxLayout()

        self.ip = QLineEdit("127.0.0.1")
        self.port = QLineEdit("8080")
        self.num_of_packets = QLineEdit("5")
        self.cur_log = QLabel("Waiting to start...")

        self.send_button = QPushButton("Send")
        layout.addWidget(QLabel("Receiver IP"))
        layout.addWidget(self.ip)
        layout.addWidget(QLabel("Receiver Port"))
        layout.addWidget(self.port)
        layout.addWidget(QLabel("Number of packets"))
        layout.addWidget(self.num_of_packets)
        layout.addWidget(self.cur_log)
        layout.addWidget(self.send_button)

        self.send_button.clicked.connect(self.send_data)
        self.setLayout(layout)

    def send_data(self):
        ip = self.ip.text()
        port = int(self.port.text())
        count = int(self.num_of_packets.text())
        packet_size = 1024
        header_size = 12
        data_size = packet_size - header_size

        self.cur_log.setText(f"Connecting to {ip}:{port}...")

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.connect((ip, port))
                self.cur_log.setText("Connected. Starting transmission...")
                for i in range(count):
                    timestamp = time.time()
                    random_data = bytes([random.randint(0, 255) for _ in range(data_size)])
                    header = struct.pack('!Id', i, timestamp)
                    packet = header + random_data
                    s.sendall(packet)
                    self.cur_log.setText(f"Sent packet {i + 1} of {count}")
                self.cur_log.setText("Transmission complete.")
        except Exception as e:
            self.cur_log.setText(f"Error (pls click start listening button in receiver): {e}")

if __name__ == "__main__":
    app = QApplication(sys.argv)
    sender = TCPSender()
    sender.show()
    sys.exit(app.exec())
