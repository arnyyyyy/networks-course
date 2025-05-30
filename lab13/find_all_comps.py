import sys
import socket
import ipaddress
import os
import uuid

from PySide6.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QPushButton, QTableWidget, QTableWidgetItem,
    QProgressBar, QLabel
)
from PySide6.QtCore import Qt, QThread, Signal
from scapy.layers.l2 import ARP, Ether
from scapy.sendrecv import srp

_local_ip = None


def get_local_ip():
    global _local_ip
    if _local_ip:
        return _local_ip
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        _local_ip = s.getsockname()[0]
        s.close()
    except Exception:
        _local_ip = socket.gethostbyname(socket.gethostname())
    return _local_ip


def get_local_mac():
    mac_int = uuid.getnode()
    if (mac_int >> 40) % 2:
        return None
    return ':'.join(f'{(mac_int >> ele) & 0xff:02x}' for ele in range(40, -1, -8))


def get_mac_address(ip):
    if ip == get_local_ip():
        return get_local_mac()
    try:
        arp_request = ARP(pdst=ip)
        broadcast = Ether(dst="ff:ff:ff:ff:ff:ff")
        answered_list = srp(broadcast / arp_request, timeout=2, verbose=False)[0]
        if answered_list:
            return answered_list[0][1].hwsrc
    except Exception:
        pass
    return None


class ScannerThread(QThread):
    result_signal = Signal(str, str, str)
    progress_signal = Signal(int)

    def __init__(self, network):
        super().__init__()
        self.network = network
        self.is_running = True

    def stop(self):
        self.is_running = False

    def run(self):
        try:
            hosts = list(ipaddress.ip_network(self.network).hosts())
            total = len(hosts)
            get_local_ip()

            for i, ip in enumerate(hosts):
                if not self.is_running:
                    break

                ip_str = str(ip)
                hostname = "-"
                try:
                    hostname_info = socket.gethostbyaddr(ip_str)
                    hostname = hostname_info[0]
                except (socket.herror, socket.timeout):
                    pass

                mac = get_mac_address(ip_str)
                if mac:
                    self.result_signal.emit(ip_str, mac, hostname)

                self.progress_signal.emit(int((i + 1) / total * 100))

        except Exception as e:
            print(f"Something went wrong: {e}")
        finally:
            self.progress_signal.emit(100)


class NetworkScanner(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Network Computer Scanner")
        self.resize(700, 500)
        self.scanner_thread = None
        self.local_ip = get_local_ip()

        layout = QVBoxLayout()

        self.label = QLabel("Click 'Scan' to start scanning")
        layout.addWidget(self.label)

        self.button = QPushButton("Scan")
        self.button.clicked.connect(self.scan_network)
        layout.addWidget(self.button)

        self.progress = QProgressBar()
        self.progress.setTextVisible(True)
        self.progress.setFormat("%p%")
        layout.addWidget(self.progress)

        self.table = QTableWidget(0, 3)
        self.table.setHorizontalHeaderLabels(["IP Address", "MAC Address", "Hostname"])
        self.table.horizontalHeader().setStretchLastSection(True)
        self.table.setSelectionBehavior(QTableWidget.SelectRows)
        layout.addWidget(self.table)

        self.setLayout(layout)

    def closeEvent(self, event):
        if self.scanner_thread and self.scanner_thread.isRunning():
            self.scanner_thread.stop()
            self.scanner_thread.wait()
        event.accept()

    def scan_network(self):
        self.table.setRowCount(0)
        self.progress.setValue(0)
        self.label.setText("Scanning...")
        self.button.setEnabled(False)

        try:
            net = ipaddress.IPv4Network(f"{self.local_ip}/24", strict=False)
            self.total_hosts = len(list(net.hosts()))
            self.progress.setMaximum(100)

            try:
                local_hostname = socket.gethostname()
                local_mac = get_mac_address(self.local_ip)
                self.add_table_row(self.local_ip, local_mac or "-", local_hostname, bold=True)
            except Exception as e:
                print(f"Something went wrong: {e}")

            self.scanner_thread = ScannerThread(str(net))
            self.scanner_thread.result_signal.connect(self.handle_result)
            self.scanner_thread.progress_signal.connect(self.update_progress)
            self.scanner_thread.finished.connect(self.scan_finished)
            self.scanner_thread.start()

        except Exception as e:
            self.label.setText(f"Error: {e}")
            self.button.setEnabled(True)

    def update_progress(self, value):
        self.progress.setValue(value)

    def scan_finished(self):
        self.label.setText("Scanning completed!")
        self.button.setEnabled(True)

    def handle_result(self, ip, mac, hostname):
        if ip == self.local_ip:
            return
        self.add_table_row(ip, mac, hostname)

    def add_table_row(self, ip, mac, hostname, bold=False):
        row = self.table.rowCount()
        self.table.insertRow(row)

        ip_item = QTableWidgetItem(ip)
        mac_item = QTableWidgetItem(mac)
        hostname_item = QTableWidgetItem(hostname)

        for item in (ip_item, mac_item, hostname_item):
            item.setFlags(Qt.ItemIsSelectable | Qt.ItemIsEnabled)
            if bold:
                font = item.font()
                font.setBold(True)
                item.setFont(font)

        self.table.setItem(row, 0, ip_item)
        self.table.setItem(row, 1, mac_item)
        self.table.setItem(row, 2, hostname_item)

        self.table.resizeColumnsToContents()


if __name__ == "__main__":
    if sys.platform == "darwin": # на винде посмотреть не могу, но скорее всего тоже лучше с рутом
        if os.geteuid() != 0:
            print("Please run with sudo: sudo python main.py")

    app = QApplication(sys.argv)
    window = NetworkScanner()
    window.show()
    sys.exit(app.exec())
