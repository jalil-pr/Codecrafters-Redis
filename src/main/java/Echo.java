import java.util.List;

public class Echo extends Commands {

    public Echo(String commandString, List<Argument> arguments) {
        super(commandString, arguments);

    }

    @Override
    public String execute() {
        return arguments.get(0).getLenghtString() + "\r\n" + arguments.get(0).getArgument() + "\r\n";
    }

}
