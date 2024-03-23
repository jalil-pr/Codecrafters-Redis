
import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.Socket;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main implements Runnable {
  Socket clientSocket;
  String regexArgumentsCounter = "\\*\\d+";
  String regexArgumentsCounterGroup = "\\*(\\d+)";

  Main(Socket client) {
    clientSocket = client;
  }

  @Override
  public void run() {
    try {
      BufferedReader br = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream()));
      OutputStream os = clientSocket.getOutputStream();
      String input = br.readLine();
      List<String> command = new ArrayList<String>();
      Pattern pattern = Pattern.compile(regexArgumentsCounterGroup);
      Matcher matcher;
      int arguments = 0;

      int argumentsCount = 0;
      while (input != null) {
        if (input.contains("ping")) {
          os.write("+PONG\r\n".getBytes());
        }
        if (argumentsCount < arguments) {
          command.add(input);
          argumentsCount++;

        }
        if (input.matches(regexArgumentsCounter)) {
          matcher = pattern.matcher(input);
          matcher.find();
          arguments = Integer.parseInt(matcher.group(1)) * 2;

          command.add(input);
        }
        if (arguments == argumentsCount) {
          Commands commandClass = new CommandBuilder(command).build();
          os.write(commandClass.execute().getBytes());
          os.flush();
          arguments = 0;
          argumentsCount = 0;

        }
        input = br.readLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
