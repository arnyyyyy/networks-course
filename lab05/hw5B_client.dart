import 'dart:io';
import 'dart:convert';

void main(List<String> args) async {
  if (args.length != 2) {
    print("Invalid number of args");
    return;
  }

  String host = args[0];
  int? port = int.tryParse(args[1]);
  if (port == null) {
    print("Invalid port number");
    return;
  }

  
  try {
    final socket = await Socket.connect(host, port);
    print("Connected to $host:$port");

    stdout.write("Enter command: ");
    final command = stdin.readLineSync();

    if (command == null || command.isEmpty) {
      print('Invalid command');
      return;
    }

    socket.writeln(command);
    await socket.flush();
    await socket.listen((data) { print(utf8.decode(data));}).asFuture();  
    socket.close();
    print('Connection closed');
  } catch (e) {
    print("Error: $e");
  }
}
