import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  private static int DEFAULT_PORT=6379;
  public static boolean isReplica = false;
  public static String replicaOf=null;
  public static int serverPort=-1;
  public static boolean isFirstRequest=true;
  static int port = DEFAULT_PORT;

  
  public static void main(String[] args) {
    // public static 
    for(int i=0;i<args.length;i++){
      if (args[i].equalsIgnoreCase("--port")) {
        port = Integer.parseInt(args[i+1]);
      }
      if (args[i].equalsIgnoreCase("--replicaof")) {
        isReplica = true;
        replicaOf = args[i+1];
        serverPort = Integer.valueOf(args[i+2]);
      }
    }
    System.out.println("Logs from your program will appear here!");
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      if(isReplica){
        if (replicaOf != null) {
          if(serverPort!=-1){
            clientSocket = new Socket(replicaOf, serverPort);
            greetServer(clientSocket);
          }
        }
      }
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
  public static void greetServer(Socket clientSocket){
    try{

      PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
      BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String greetStr = "*1\r\n$4\r\nping\r\n";
      printWriter.write(greetStr);
      printWriter.flush();
      String pingResponse = reader.readLine();
      if (pingResponse != null) {
        String replConfStr = "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n"+port+"\r\n";
        printWriter.write(replConfStr);
        printWriter.flush();
        String secStageResponse = reader.readLine();
        if(secStageResponse != null){

          String secondConfStr = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n";
          printWriter.write(secondConfStr);
          printWriter.flush();

          String thirdStageResp = reader.readLine();
          // stage 3 handshake: sending psync
          if (thirdStageResp != null) {
            String psync = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n";
            printWriter.write(psync);
            printWriter.flush();
            
          }
        }

      }

    }catch(Exception e){
      e.printStackTrace();
    }

  }
 
}
