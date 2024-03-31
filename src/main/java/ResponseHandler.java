import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
      PrintWriter printWriter = new PrintWriter(outputStream, false);
      String input = reader.readLine();
      if (Main.isFirstRequest) {
        System.out.println("########### FIRST REQUEST #############");
        Main.isFirstRequest = false;
        StringBuilder stringBuilder =new StringBuilder();
        stringBuilder.append("*1\r\n$4\r\nping\r\n");
        printWriter.write(stringBuilder.toString());
        
      }
      HashMap<String, String> records = new HashMap<String, String>();
      HashMap<String, Long> recordsExpiry = new HashMap<String, Long>();
      HashMap<String, Date> timesStore = new HashMap<String, Date>();
      HashMap<String, String> infoCommand = new HashMap<>();
      if(Main.isReplica){
        infoCommand.put("replica", "slave");
      }else{
        // TODO: remove the hard codes
        infoCommand.put("replica", "master");
        Main.masterReplid="8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
        Main.masterReplOffset=String.valueOf(0);
        // infoCommand.put("master_replid", "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb");
        // infoCommand.put("master_repl_offset", "0");
      }
     
      while (input != null && !input.isEmpty()) {

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
            case Commands.INFO:
              String reqInfo = storedCommands.get(3);
              if (reqInfo.equalsIgnoreCase("replication")) {
                String response = "";
                response = response+("role".length()+infoCommand.get("replica").length()+1)+""+"role:"+infoCommand.get("replica")+"\n";
                if(!Main.isReplica){
                
                  response=response+("master_repl_offset".length()+1+Main.masterReplOffset.length())+""+"master_repl_offset"+":"+Main.masterReplOffset+"\n";
                }
                if(!Main.isReplica){
                  response=response+("master_replid".length()+1+Main.masterReplid.length())+""+"master_replid"+":"+Main.masterReplid;
                }
                String finalResponse = "$"+response.length()+"\r\n"+response+"\r\n";
                outputStream.write(finalResponse.getBytes());
              }
              break;

            case Commands.SET:
              String key = storedCommands.get(3);
              String value = storedCommands.get(5);
              records.put(key, value);
              if (storedCommands.size() > 6) {
                String px = storedCommands.get(7);
                if (px.equalsIgnoreCase("px")) {
                  Long validUpto = Long.parseLong(storedCommands.get(9));
                  timesStore.put(key, new Date());
                  recordsExpiry.put(key, validUpto);
                }
              }
              String setReply = "+OK\r\n";
              outputStream.write(setReply.getBytes());
              break;
            case Commands.GET:
              String getKey = storedCommands.get(3);
              String getValue = records.get(getKey);
              Long expiry = null;
              Date storedDate = null;
              if (timesStore != null) {
                storedDate = timesStore.get(getKey);
              }
              if (recordsExpiry != null) {
                expiry = recordsExpiry.get(getKey);
              }
              Long difference = null;
              if (storedDate != null) {
                difference = (new Date().getTime() - storedDate.getTime());
              }
              if (getValue == null) {
                outputStream.write("$-1\r\n".getBytes());
              } else if (difference != null && difference > expiry) {
                outputStream.write("$-1\r\n".getBytes());
              } else {
                String foundValue = "$" + getValue.length() + "\r\n" + getValue + "\r\n";
                outputStream.write(foundValue.getBytes());
              }
              break;
            case Commands.REPLCONF:
              String replConfResp = "+OK" + "\r\n";
              outputStream.write(replConfResp.getBytes());
              break;
            case Commands.PSYNC:
              String pSyncResp = "+FULLRESYNC"+" "+Main.masterReplid+" " +Main.masterReplOffset +"\r\n";
              outputStream.write(pSyncResp.getBytes());
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
