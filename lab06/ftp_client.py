import socket
import os

BUFFER_SIZE = 1024


class FTPClient:
    def __init__(self, server, user, passwd):
        self.server = server
        self.user = user
        self.passwd = passwd
        self.ctrl_sock = None

    def connect(self):
        self.ctrl_sock = socket.create_connection(self.server)
        self._receive_data()
        self._login()

    def _receive_data(self):
        data = b""
        while True:
            part = self.ctrl_sock.recv(BUFFER_SIZE)
            data += part
            if len(part) < BUFFER_SIZE:
                break
        return data

    def _send_command(self, command):
        self.ctrl_sock.sendall(command.encode() + b"\r\n")
        return self._receive_data().decode()

    def _login(self):
        self._send_command(f"USER {self.user}")
        self._send_command(f"PASS {self.passwd}")
        self._send_command("TYPE I")

    def _enter_pasv(self):
        response = self._send_command("PASV")
        start, end = response.find("(") + 1, response.find(")")
        numbers = list(map(int, response[start:end].split(",")))
        host = ".".join(map(str, numbers[:4]))
        port = numbers[4] * 256 + numbers[5]
        return host, port

    def list_files(self):
        host, port = self._enter_pasv()
        with socket.create_connection((host, port)) as data_sock:
            self._send_command("LIST")
            data = self._receive_socket(data_sock)
            print(data.decode())
            self._receive_data()

    def upload_file(self, filepath):
        if not os.path.isfile(filepath):
            print(f"File '{filepath}' does not exist.")
            return

        host, port = self._enter_pasv()
        with socket.create_connection((host, port)) as data_sock:
            filename = os.path.basename(filepath)
            self.ctrl_sock.sendall(f"STOR {filename}\r\n".encode())
            with open(filepath, "rb") as f:
                data_sock.sendfile(f)
            self._receive_data()

    def download_file(self, filename, save_as):
        host, port = self._enter_pasv()
        with socket.create_connection((host, port)) as data_sock:
            self._send_command(f"RETR {filename}")
            data = self._receive_socket(data_sock)
            with open(save_as, "wb") as f:
                f.write(data)
            self._receive_data()

    @staticmethod
    def _receive_socket(sock):
        data = b""
        while True:
            part = sock.recv(BUFFER_SIZE)
            if not part:
                break
            data += part
        return data

    def close(self):
        if self.ctrl_sock:
            self.ctrl_sock.close()


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print("Usage: python script.py [ls|upload|download] [filename] [save_as]")
        sys.exit(1)

    command = sys.argv[1]
    filename = sys.argv[2] if len(sys.argv) > 2 else ""
    save_as = sys.argv[3] if len(sys.argv) > 3 else ""

    client = FTPClient(('ftp.dlptest.com', 21), "dlpuser", "rNrKYTX9g7z3RgJRmxWuGHbeu")
    try:
        client.connect()

        if command == "ls":
            client.list_files()
        elif command == "upload":
            client.upload_file(filename)
        elif command == "download":
            if not save_as:
                save_as = filename
            client.download_file(filename, save_as)
        else:
            print("Unknown command:", command)

    except Exception as e:
        print("Error:", e)

    finally:
        client.close()
