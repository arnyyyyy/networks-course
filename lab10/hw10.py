import socket
import struct
import time
import sys

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

def ping(host, count=4):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.getprotobyname('icmp'))
    except PermissionError:
        print("Error: ICMP messages can only be sent from root processes (add sudo).")
        sys.exit(1)
        
    dest_addr = socket.gethostbyname(host)
    id = 25723
    seq_num = 1
    rtt_list = []
    sent_packets = 0
    received_packets = 0

    print(f"Pinging {dest_addr}:")

    for _ in range(count):
        header = struct.pack('!BBHHH', 8, 0, 0, id, seq_num)
        data = struct.pack('d', time.time())
        checksum_val = calculate_checksum(header + data)
        header = struct.pack('!BBHHH', 8, 0, checksum_val, id, seq_num)
        packet = header + data

        sock.sendto(packet, (host, 0))
        sent_packets += 1

        sock.settimeout(1)
        
        try:
            recv_packet, addr = sock.recvfrom(1024)
            end_time = time.time()
            
            icmp_header = recv_packet[20:28] 
            type, _, _, p_id, _ = struct.unpack('!BBHHH', icmp_header)

            if p_id == id and type == 0:
                rtt = (end_time - struct.unpack('d', recv_packet[28:36])[0]) * 1000
                rtt_list.append(rtt)
                received_packets += 1
                print(f"reply from {addr[0]} -- seq: {seq_num} rtt: {rtt:.1f} ms")
            else:
                print(f"unexpected packet from {addr[0]}")
        except socket.timeout:
            print(f"request timeout for seq {seq_num}")

        seq_num += 1
        time.sleep(1)

    sock.close()

    loss = ((sent_packets - received_packets) / sent_packets) * 100
    if rtt_list:
        min_rtt = min(rtt_list)
        max_rtt = max(rtt_list)
        avg_rtt = sum(rtt_list) / len(rtt_list)
    else:
        min_rtt = max_rtt = avg_rtt = 0

    print("\nPing statistics:")
    print(f"packets: sent = {sent_packets}, received = {received_packets}, lost = {sent_packets - received_packets} ({loss:.0f}% loss)")
    print(f"rtt: min = {min_rtt:.1f} ms, max = {max_rtt:.1f} ms, avg = {avg_rtt:.1f} ms")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(1)

    target_host = sys.argv[1]
    ping(target_host, count=8)
