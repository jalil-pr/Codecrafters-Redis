package redis;

import redis.Commands.RedisCommand;

public class TerminateCommand extends RedisCommand {

    public TerminateCommand() {
        super(Type.TERMINATE);
    }

    @Override
    public byte[] execute(RedisServiceBase service) {
        return null;
    }

    @Override
    public String toString() {
        return "TerminateCommand []";
    }

}
