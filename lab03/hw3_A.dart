import 'dart:io';
import 'dart:convert';

void main(List<String> args) {
  if (args.length != 1) {
    print("Invalid number of args");
    return;
  }

  int? num = int.tryParse(args[0]);
  if (num == null) {
    print("Invalid port number");
    return;
  }

  runServer(num);
}


void runServer(int port) {
  print("Trying to make connection");

  ServerSocket.bind(InternetAddress.anyIPv4, port).then((server) {
    print("Server work is started");
    server.listen((socket) {
      handleConnection(socket);
    });
  }).catchError((error) {
    print("Failed to start server: $error");
  });
}

void handleConnection(Socket socket) {
  String remoteAddress = socket.remoteAddress.address;
  int remotePort = socket.remotePort;
  print("Connection from $remoteAddress:$remotePort");

  Stream<String> stringStream = socket.cast<List<int>>().transform(utf8.decoder);
  StringBuffer requestBuffer = StringBuffer();

  stringStream.listen(
    (String data) {
      requestBuffer.write(data);
    
        String request = requestBuffer.toString();
        print("Request received:\n$request");

        List<String> info = request.split('\n')[0].split(' ');
        if (info.length != 3) {  // protocol is the third
          sendResponse(socket, 400, "Incorrect request format");
          return;
        }

        String method = info[0];
        String path = info[1];

        if (method != "GET") {
          sendResponse(socket, 405, "Method Not Allowed");
          return;
        }

        handleFileRequest(socket, path);
    },
    onDone: () {
      print("Client disconnected: $remoteAddress:$remotePort");
    },
    onError: (error) {
      print("Error with client $remoteAddress:$remotePort: $error");
    },
  );
}

void handleFileRequest(Socket socket, String path) {
  String filePath = "." + path;
  print("Serving file: $filePath");
  File file = File(filePath);

  file.exists().then((exists) {
    if (!exists) {
      sendResponse(socket, 404, "Not Found");
      return;
    }

    file.readAsString().then((content) {
      sendResponse(socket, 200, "OK", content);
    }).catchError((error) {
      print("Error reading file '$filePath': $error");
      sendResponse(socket, 500, "Internal Server Error");
    });
  }).catchError((error) {
    print("Error handling file request '$filePath': $error");
    sendResponse(socket, 500, "Internal Server Error");
  });
}

void sendResponse(Socket socket, int statusCode, String statusMessage, [String? content]) {
  StringBuffer response = StringBuffer()
    ..write("HTTP/1.1 $statusCode $statusMessage\r\n")
    ..write("Content-Type: text/html; charset=UTF-8\r\n")
    ..write("Connection: close\r\n")
    ..write("Content-Length: ${content?.length ?? statusMessage.length}\r\n")
    ..write('\r\n');
      
  response.write(content ?? statusMessage);
  
  socket.write(response.toString());
  socket.close();
}