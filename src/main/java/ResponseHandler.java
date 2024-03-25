import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    try {
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      OutputStream outputStream = clientSocket.getOutputStream();
      String input = bufferedReader.readLine();
      while ((input = bufferedReader.readLine()) != null) {
        System.out.println("the input>>>" + input);
        if (input != null) {
          var result = ProtocolParser.parse(input);
          var finalResult = ProtocolParser.encode(result);
          byte []b = finalResult.getBytes(StandardCharsets.UTF_8);
          outputStream.write(b);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
