import socket
import sys

HOST = '::1'  
DEFAULT_PORT = 8120
BUFFER_SIZE = 1024

def run_client(port):
    try:
        with socket.socket(socket.AF_INET6, socket.SOCK_STREAM) as client_sock:
            client_sock.connect((HOST, port))
            print(f"Connected to server on port {port}.")

            while True:
                client_sock.sendall(message.encode())
                data = client_sock.recv(BUFFER_SIZE)
                print(f"Received: {data.decode()}")
    except Exception as e:
        print(f"Client error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print("Invalid port number. Using default port.")
            port = DEFAULT_PORT
    else:
        port = DEFAULT_PORT
    
    run_client(port)
