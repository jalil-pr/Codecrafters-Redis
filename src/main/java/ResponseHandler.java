import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.DocFlavor.READER;

public class ResponseHandler implements Runnable {
  Socket clientSocket = null;

  ResponseHandler(Socket client) {
    clientSocket = client;
  }

  @Override
  public void run() {

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      OutputStream outputStream = clientSocket.getOutputStream();
      String input = reader.readLine();
      HashMap<String, String> records = new HashMap<String, String>();
      while (input != null && !input.isEmpty()) {
        // temprary set for storing set and get values

        if (input.startsWith("*")) {
          int numberOfLines = Integer.parseInt(String.valueOf(input.charAt(1)));
          ArrayList<String> storedCommands = new ArrayList<>(numberOfLines * 2);
          for (int i = 0; i < numberOfLines * 2; i++) {
            storedCommands.add(reader.readLine());

          }
          String command = storedCommands.get(1);
          switch (command.toLowerCase()) {
            case Commands.PING:
              String toBeSent = "+PONG" + "\r\n";
              outputStream.write(toBeSent.getBytes());
              break;
            case Commands.ECHO:
              String toBeEchoed = "$" + storedCommands.get(3).length() + "\r\n" + storedCommands.get(3) + "\r\n";
              outputStream.write(toBeEchoed.getBytes());
              break;
            case Commands.SET:
              String key = storedCommands.get(3);
              String value = storedCommands.get(5);
              records.put(key, value);

              String setReply = "+OK\r\n";
              outputStream.write(setReply.getBytes());
              break;
            case Commands.GET:
              String getKey = storedCommands.get(3);
              String getValue = records.get(getKey);

              if (getValue == null) {
                outputStream.write("$-1\r\n".getBytes());
              } else {
                String foundValue = "$" + getValue.length() + "\r\n" + getValue + "\r\n";
                outputStream.write(foundValue.getBytes());
              }
              break;
            default:
              outputStream.write("WRONG COMMAND".getBytes());
          }

        } else {
          outputStream.write("WRONG COMMAND".getBytes());

        }
        input = reader.readLine();
      }

    } catch (Exception e) {
      e.printStackTrace();
      try {
        clientSocket.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }

  }

}
