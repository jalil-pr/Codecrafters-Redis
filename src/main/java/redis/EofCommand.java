package redis;

import redis.Commands.RedisCommand;

public class EofCommand extends RedisCommand {

    public EofCommand() {
        super(Type.EOF);
    }

    @Override
    public byte[] execute(RedisServiceBase service) {
        return null;
    }

    @Override
    public String toString() {
        return "EofCommand []";
    }

}
