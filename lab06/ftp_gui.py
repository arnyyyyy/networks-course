import os
import sys
from ftplib import FTP
from io import BytesIO, StringIO

from PySide6.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QLineEdit, QPushButton, QListWidget, QTextEdit, QMessageBox, QFileDialog, QInputDialog
)
from PySide6.QtCore import QThread, Signal, QObject


class FTPClient:
    def __init__(self):
        self.ftp = None

    def connect(self, server, port, username, password):
        self.ftp = FTP()
        self.ftp.connect(server, port)
        self.ftp.login(username, password)
        self.ftp.set_pasv(True)

    def disconnect(self):
        if self.ftp:
            self.ftp.quit()
            self.ftp = None

    def list_files(self):
        files = []
        self.ftp.retrlines("LIST", files.append)
        return [line.split()[-1] for line in files if line.strip()]

    def upload_file(self, filepath):
        filename = os.path.basename(filepath)
        with open(filepath, "rb") as f:
            self.ftp.storbinary(f"STOR {filename}", f)
        return filename

    def download_file(self, filename, path):
        with open(path, "wb") as f:
            self.ftp.retrbinary(f"RETR " + filename, f.write)

    def retrieve_text_file(self, filename):
        bio = BytesIO()
        self.ftp.retrbinary(f"RETR {filename}", bio.write)
        try:
            return bio.getvalue().decode('utf-8')
        except UnicodeDecodeError:
            return "Failed to display"

    def retrieve_text_lines(self, filename):
        data = StringIO()
        self.ftp.retrlines(f"RETR {filename}", lambda line: data.write(line + "\n"))
        return data.getvalue()


class FileLoaderWorker(QObject):
    finished = Signal(str, str, str)

    def __init__(self, client: FTPClient, filename):
        super().__init__()
        self.client = client
        self.filename = filename

    def run(self):
        try:
            text = self.client.retrieve_text_file(self.filename)
            self.finished.emit("ok", self.filename, text)
        except Exception as e:
            self.finished.emit("error", self.filename, str(e))


class EditorWindow(QWidget):
    def __init__(self, client, parent, filename="", content="", is_new=False):
        super().__init__()
        self.client = client
        self.parent = parent
        self.filename = filename
        self.is_new = is_new

        self.setWindowTitle("Edit File" if not is_new else "Create File")
        layout = QVBoxLayout()
        self.text_edit = QTextEdit()
        self.text_edit.setText(content)
        layout.addWidget(self.text_edit)

        self.save_button = QPushButton("Save")
        self.save_button.clicked.connect(self.save_file)
        layout.addWidget(self.save_button)

        self.setLayout(layout)

    def save_file(self):
        try:
            content = self.text_edit.toPlainText().encode()
            data = BytesIO(content)
            self.client.ftp.storbinary(f"STOR {self.filename}", data)
            self.parent.refresh_file_list()
            self.close()
        except Exception as e:
            QMessageBox.critical(self, "Error", str(e))


class FTPClientGUI(QWidget):
    show_editor_signal = Signal(str, str)

    def __init__(self):
        super().__init__()
        self.client = FTPClient()
        self.show_editor_signal.connect(self.open_editor_window)

        self.setup_ui()

    def setup_ui(self):
        self.setWindowTitle("FTP Client - PySide")
        self.file_list = QListWidget()
        self.upload_btn = QPushButton("Upload")
        self.download_btn = QPushButton("Download")
        self.view_btn = QPushButton("View")
        self.edit_btn = QPushButton("Edit")
        self.create_btn = QPushButton("Create")
        self.connect_button = QPushButton("Connect")
        self.delete_btn = QPushButton("Delete")

        self.server_input = QLineEdit("ftp.dlptest.com")
        self.port_input = QLineEdit("21")
        self.user_input = QLineEdit("dlpuser")
        self.pass_input = QLineEdit("rNrKYTX9g7z3RgJRmxWuGHbeu")
        self.pass_input.setEchoMode(QLineEdit.Password)

        layout = QVBoxLayout()
        server_layout = QHBoxLayout()
        server_layout.addWidget(QLabel("Server:"))
        server_layout.addWidget(self.server_input)
        server_layout.addWidget(QLabel("Port:"))
        server_layout.addWidget(self.port_input)
        layout.addLayout(server_layout)

        auth_layout = QHBoxLayout()
        auth_layout.addWidget(QLabel("Username:"))
        auth_layout.addWidget(self.user_input)
        auth_layout.addWidget(QLabel("Password:"))
        auth_layout.addWidget(self.pass_input)
        layout.addLayout(auth_layout)

        layout.addWidget(self.connect_button)
        layout.addWidget(self.file_list)

        button_layout = QHBoxLayout()
        for btn in [self.upload_btn, self.download_btn, self.view_btn, self.edit_btn, self.create_btn, self.delete_btn]:
            button_layout.addWidget(btn)
            btn.setEnabled(False)

        layout.addLayout(button_layout)
        self.setLayout(layout)

        self.connect_button.clicked.connect(self.connect_to_server)
        self.upload_btn.clicked.connect(self.upload_file)
        self.download_btn.clicked.connect(self.download_file)
        self.view_btn.clicked.connect(self.view_file)
        self.edit_btn.clicked.connect(self.edit_file)
        self.create_btn.clicked.connect(self.create_file)
        self.delete_btn.clicked.connect(self.delete_file)

    def connect_to_server(self):
        try:
            self.client.connect(
                self.server_input.text(),
                int(self.port_input.text()),
                self.user_input.text(),
                self.pass_input.text()
            )
            self.refresh_file_list()
            for btn in [self.upload_btn, self.download_btn, self.view_btn, self.edit_btn, self.create_btn,
                        self.delete_btn]:
                btn.setEnabled(True)

        except Exception as e:
            QMessageBox.critical(self, "Connection Error", str(e))

    def refresh_file_list(self):
        try:
            self.file_list.clear()
            files = self.client.list_files()
            self.file_list.addItems(files)
        except Exception as e:
            QMessageBox.critical(self, "Error", str(e))

    def upload_file(self):
        filepath, _ = QFileDialog.getOpenFileName(self, "Select file to upload")
        if not filepath:
            return
        try:
            filename = self.client.upload_file(filepath)
            self.refresh_file_list()
            QMessageBox.information(self, "Upload", f"Uploaded {filename}")
        except Exception as e:
            QMessageBox.critical(self, "Error", str(e))

    def download_file(self):
        selected = self.file_list.currentItem()
        if not selected:
            QMessageBox.warning(self, "No file", "Select a file first.")
            return

        filename = selected.text()
        path, _ = QFileDialog.getSaveFileName(self, "Save file as", filename)
        if not path:
            return
        try:
            self.client.download_file(filename, path)
            QMessageBox.information(self, "Download", f"{filename} saved.")
        except Exception as e:
            QMessageBox.critical(self, "Error", str(e))

    def delete_file(self):
        selected = self.file_list.currentItem()
        if not selected:
            QMessageBox.warning(self, "No file", "Select a file to delete.")
            return

        filename = selected.text()
        confirm = QMessageBox.question(
            self, "Confirm Delete", f"Are you sure you want to delete '{filename}'?",
            QMessageBox.Yes | QMessageBox.No
        )

        if confirm == QMessageBox.Yes:
            try:
                self.client.ftp.delete(filename)
                self.refresh_file_list()
                QMessageBox.information(self, "Deleted", f"'{filename}' was deleted.")
            except Exception as e:
                QMessageBox.critical(self, "Error", str(e))

    def view_file(self):
        selected = self.file_list.currentItem()
        if not selected:
            QMessageBox.warning(self, "No file", "Select a file first.")
            return

        filename = selected.text()
        try:
            text = self.client.retrieve_text_lines(filename)

            viewer = QWidget()
            viewer.setWindowTitle(f"Viewing: {filename}")
            layout = QVBoxLayout()

            text_edit = QTextEdit()
            text_edit.setReadOnly(True)
            text_edit.setText(text)

            layout.addWidget(text_edit)
            viewer.setLayout(layout)
            viewer.resize(600, 400)
            viewer.show()

            self.viewer_window = viewer

        except Exception as e:
            QMessageBox.critical(self, "Error", f"Failed to open {filename}: {str(e)}")

    def create_file(self):
        name, ok = QInputDialog.getText(self, "New File", "Enter filename:")
        if not ok or not name.strip():
            return
        self.editor = EditorWindow(self.client, self, filename=name.strip(), content="", is_new=True)
        self.editor.show()

    def edit_file(self):
        selected = self.file_list.currentItem()
        if not selected:
            QMessageBox.warning(self, "No file", "Select a file first.")
            return

        filename = selected.text()
        self.thread = QThread()
        self.worker = FileLoaderWorker(self.client, filename)
        self.worker.moveToThread(self.thread)

        self.worker.finished.connect(self.handle_file_loaded)
        self.thread.started.connect(self.worker.run)
        self.worker.finished.connect(self.thread.quit)
        self.worker.finished.connect(self.worker.deleteLater)
        self.thread.finished.connect(self.thread.deleteLater)

        self.thread.start()

    def handle_file_loaded(self, status, filename, content):
        if status == "error":
            QMessageBox.critical(self, "Error", f"Failed to load '{filename}':\n{content}")
        elif status == "ok":
            self.show_editor_signal.emit(filename, content)

    def open_editor_window(self, filename, content):
        self.editor = EditorWindow(self.client, self, filename=filename, content=content)
        self.editor.show()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = FTPClientGUI()
    window.show()
    sys.exit(app.exec())
