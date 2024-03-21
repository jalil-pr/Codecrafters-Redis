import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    int port = 6379;

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors

    try {
      serverSocket = new ServerSocket(port);

      serverSocket.setReuseAddress(true);
      // Wait for connection from client.
      while (true) {
        clientSocket = serverSocket.accept();
        // Thread t = new Thread();
        Main mainClass = new Main();
        Main.Responder res = mainClass.new Responder(clientSocket);
        res.run();

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

  class Responder implements Runnable {
    Socket socket;

    Responder(Socket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        OutputStream outputStream = socket.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String inputLine = bufferedReader.readLine();
        while (inputLine != null) {
          
          if (inputLine.contains("ping") || inputLine.equalsIgnoreCase("ping")) {
            outputStream.write("+PONG\r\n".getBytes());
            outputStream.flush();
          } // 2nd statement
          inputLine = bufferedReader.readLine();
          // new PrintWriter(clientSocket.getOutputStream(), true);
        }
        outputStream.close();
      } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
      } finally {
        try {
          if (socket != null) {
            socket.close();

          }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
      }

    }

  }
}
