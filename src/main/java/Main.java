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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    int port = 6379;
    ExecutorService es = null;

    try {
      serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);
      es = Executors.newFixedThreadPool(4);
      class Handler implements Runnable {
        Socket clientSocket;

        Handler(Socket clientSocket) {
          this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
          try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line;
            while (!(line = in.readLine()).isEmpty()) {
              System.out.println("Received:" + line);
              if ("ping".equals(line) || line.contains("ping")) {
                outputStreamWriter.write("+PONG\r\n");
                outputStreamWriter.flush();
              }
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
      while ((clientSocket = serverSocket.accept()) != null) {
        es.submit(new Handler(clientSocket));
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
