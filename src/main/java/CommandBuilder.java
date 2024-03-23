import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandBuilder {
    private List<String> command;
    private final String regexArgumentsInCommand = "\\*\\d+";
    private final String regexArgumentsInCommandGroup = "\\*(\\d+)";

    public CommandBuilder(List<String> command) {
        this.command = command;
    }

    public Commands build() {
        int argInCommand = 0;
        int index = 1;
        String commandName = "";
        List<Argument> arguments = new ArrayList<Argument>();
        if (command.get(0).matches(regexArgumentsInCommand)) {
            Pattern pattern = Pattern.compile(regexArgumentsInCommandGroup);
            Matcher matcher = pattern.matcher(command.get(0));
            matcher.find();
            argInCommand = Integer.parseInt(matcher.group(1));
        } else {
            new Exception("Test");
        }
        for (int i = 0; i < argInCommand; i++) {
            arguments.add(new Argument(command.get(index), command.get(index + 1)));
            index = index + 2;
        }
        commandName = arguments.get(0).getArgument();
        arguments.remove(0);
        if (commandName.toUpperCase().equals("PING")) {
            return new Ping(commandName, arguments);
        } else if (commandName.toUpperCase().equals("ECHO")) {
            return new Echo(commandName, arguments);
        }

        return null;
    }
}
