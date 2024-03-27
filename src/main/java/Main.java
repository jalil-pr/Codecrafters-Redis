import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  private static int DEFAULT_PORT=6379;
  public static void main(String[] args) {
    int port = DEFAULT_PORT;
    for(int i=0;i<args.length;i++){
      if (args[i].equalsIgnoreCase("--port")) {
        port = Integer.parseInt(args[i+1]);
      }
    }
    System.out.println("Logs from your program will appear here!");
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);
      while ((clientSocket = serverSocket.accept()) != null) {
        ResponseHandler rh = new ResponseHandler(clientSocket);
        Thread t = new Thread(rh);
        t.start();

      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (clientSocket != null) {
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
      }
    }

  }
}
