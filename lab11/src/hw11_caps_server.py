import socket
import sys

HOST = '::1' 
DEFAULT_PORT = 8120 
BACKLOG = 5
BUFFER_SIZE = 1024

def run_server(port):
    try:
        with socket.socket(socket.AF_INET6, socket.SOCK_STREAM) as server_sock:
            server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server_sock.bind((HOST, port))
            server_sock.listen(BACKLOG)
            print(f"Server started on [{HOST}]:{port}")

            while True:
                conn, addr = server_sock.accept()
                with conn:
                    print(f"Connected by {addr}")
                    while True:
                        data = conn.recv(BUFFER_SIZE)
                        if not data:
                            break
                        received_message = data.decode()
                        response = received_message.upper().encode()
                        
                        print(f"Received from {addr}: {received_message}")
                        print(f"Sending to {addr}: {received_message.upper()}")
                        
                        conn.sendall(response)
    except Exception as e:
        print(f"Server error: {e}")
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
    
    run_server(port)
