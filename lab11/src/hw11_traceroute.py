import socket
import struct
import time
import sys

ICMP_ECHO_REQUEST = 8
ICMP_ECHO_REPLY = 0
ICMP_TIME_EXCEEDED = 11
TIMEOUT = 1
CNT = 10
PACKET_CNT = 5

def bit_checksum(data):
    K = 16
    total_sum = 0
    for i in range(0, len(data), 2):
        word = (data[i] << 8) + (data[i+1] if i+1 < len(data) else 0)
        total_sum += word
    while total_sum >> K:
        total_sum = (total_sum & 0xFFFF) + (total_sum >> K)
    return total_sum & 0xFFFF

def calculate_checksum(data):
    raw_sum = bit_checksum(data)
    return ~raw_sum & 0xFFFF

def create_icmp_packet(id, seq_num):
    header = struct.pack('!BBHHH', ICMP_ECHO_REQUEST, 0, 0, id, seq_num)
    data = struct.pack('d', time.time())
    checksum_val = calculate_checksum(header + data)
    header = struct.pack('!BBHHH', ICMP_ECHO_REQUEST, 0, checksum_val, id, seq_num)
    return header + data

def send_packet(sock, packet, dest_addr, ttl):
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_TTL, ttl)
    start_time = time.time()
    sock.sendto(packet, (dest_addr, 0))
    return start_time

def traceroute(host):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.getprotobyname('icmp'))
    except PermissionError:
        print("Error: ICMP messages can only be sent from root processes (add sudo).")
        sys.exit(1)
        
    try:
        dest_addr = socket.gethostbyname(host)
    except socket.gaierror:
        print(f"Failed to get host address: {host}")
        sys.exit(1)

    id = 25723
    seq_num = 1

    print(f"Traceroute to {host} [{dest_addr}]:")

    for ttl in range(CNT):
        ip_addr = None
        rtt_list = []
        
        print(f"{ttl + 1:2d}", end="  ")
        
        for _ in range(PACKET_CNT):
            packet = create_icmp_packet(id, seq_num)
            
            try:
                start_time = send_packet(sock, packet, dest_addr, ttl)
            except socket.error as e:
                print(f"Failed to send packet: {e}")
                return

            sock.settimeout(TIMEOUT)
            try:
                while True:
                    try:
                        recv_packet, addr = sock.recvfrom(1024)
                        end_time = time.time()
                        
                        icmp_header = recv_packet[20:28]
                        type, _, _, packet_id, _ = struct.unpack('!BBHHH', icmp_header)
                        
                        if (type == ICMP_TIME_EXCEEDED or 
                            (type == ICMP_ECHO_REPLY and packet_id == id)):
                            current_addr = addr[0]
                            if ip_addr is None:
                                ip_addr = current_addr
                            rtt = (end_time - start_time) * 1000
                            rtt_list.append(rtt)
                            break
                    except socket.timeout:
                        break
            except socket.error:
                pass

            seq_num += 1
            
        if ip_addr:
            try:
                hostname = socket.gethostbyaddr(ip_addr)[0]
            except socket.herror:
                hostname = "Unknown host"
            
            print(f"{ip_addr} ({hostname})", end="  ")
            
            for rtt in rtt_list:
                print(f"{rtt:.2f} ms", end="  ")
            
            for _ in range(PACKET_CNT - len(rtt_list)):
                print("*", end="  ")
        else:
            print("* * *", end="")
        
        print() 
        
        if ip_addr == dest_addr:
            break

    sock.close()
    print("\nTraceroute completed.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(1)

    target_host = sys.argv[1]

    traceroute(target_host)