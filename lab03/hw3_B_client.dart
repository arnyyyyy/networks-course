import 'dart:io';
import 'dart:convert';

void main(List<String> args) async {
  if (args.length != 3) {
    print("Invalid number of args");
    return;
  }

  String host = args[0];
  int? port = int.tryParse(args[1]);
  if (port == null) {
    print("Invalid port number");
    return;
  }
  String fileName = args[2];


  try {
    final socket = await Socket.connect(host, port);
    print("Connected to $host:$port");

    socket.write("GET /$fileName HTTP/1.1\r\n");
    socket.write("Host: $host\r\n");
    socket.write("Connection: close\r\n");
    socket.write("\r\n");
    await socket.flush();
    await socket.listen((data) { print(utf8.decode(data));}).asFuture();  
    socket.close();
  } catch (e, stackTrace) {
    print("${e}");
    print('Stack trace: ${stackTrace}');

  }
}