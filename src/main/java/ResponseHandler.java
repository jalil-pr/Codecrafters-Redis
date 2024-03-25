import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseHandler implements Runnable {
  Socket clientSocket = null;

  ResponseHandler(Socket client) {
    clientSocket = client;
  }

  @Override
  public void run() {
    // get the input and output
  
    try{
      BufferedReader  reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      OutputStream outputStream = clientSocket.getOutputStream();
      PrintWriter pw = new PrintWriter(outputStream);
    
      String input = reader.readLine();
      if (input != null && !input.isEmpty()) {
        if (input.startsWith("*")) {
          int numberOfLines = input.charAt(1);
          ArrayList<String> storedCommonds = new ArrayList<>(numberOfLines*2);
          for(int i=0;i<numberOfLines*2;i++){
            storedCommonds.add(reader.readLine());
          }
          String commond = storedCommonds.get(1);
          switch (commond.toLowerCase()) {
            case Commands.PING:
              String toBeSent = "+PONG"+"\r\n";
              pw.write(toBeSent);
              break;
          
            case Commands.ECHO:
              String toBeEchoed="$"+storedCommonds.get(3).length()+storedCommonds.get(3)+"\r\n";
              pw.write(toBeEchoed);
            default:
              pw.write("WRONG COMMOND!");
          }

          
        }else {
            pw.write("WRONG COMMOND");
        } 
      }

    }catch(Exception e){

    }
   
  }

}
