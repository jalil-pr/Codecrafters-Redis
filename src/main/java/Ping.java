import java.util.List;

public class Ping extends Commands{
    public Ping(String commandString, List<Argument> arguments){

        super(commandString, arguments);
    }

    @Override
    public String execute(){
        return "+PONG\r\n";
    }
    
}
