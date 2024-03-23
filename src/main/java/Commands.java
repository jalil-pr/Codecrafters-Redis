import java.util.List;

public class Commands implements Icommand {
    protected Ecommand command;
    protected String commandString;
    protected List<Argument> arguments;
    public Commands(String commandString, List<Argument> arguments) {
      this.commandString = commandString;
      this.arguments = arguments;
      this.command = getCommand(commandString);
    }
    protected Ecommand getCommand(String commandStr) {
      String command = commandStr.toUpperCase();
      switch (command) {
      case "PING":
        return Ecommand.PING;
      case "ECHO":
        return Ecommand.ECHO;
      }
      return Ecommand.PING;
    }
    @Override
    public String execute() {
      return null;

    }
  }