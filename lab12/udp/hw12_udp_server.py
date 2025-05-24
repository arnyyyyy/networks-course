import sys
import socket
import struct
import time
from PySide6.QtWidgets import QApplication, QWidget, QVBoxLayout, QLabel, QLineEdit, QPushButton

class UDPReceiver(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("UDP Receiver")
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
        received_packets = set()

        self.cur_log.setText("Listening for UDP packets...")

        try:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.bind((ip, port))
                s.settimeout(5.0)  

                count = 0
                start = time.time()

                while True:
                    try:
                        data, addr = s.recvfrom(packet_size)
                        if len(data) < header_size:
                            continue  

                        pkt_num, pkt_time = struct.unpack('!Id', data[:header_size])
                        received_packets.add(pkt_num)
                        count += 1
                        self.cur_log.setText(f"Packets received: {count}")
                    except socket.timeout:
                        break

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
    receiver = UDPReceiver()
    receiver.show()
    sys.exit(app.exec())
