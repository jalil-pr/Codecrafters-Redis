package redis.Commands;

import java.util.Map;

import redis.RedisServiceBase;
import redis.protocol.RespInteger;
import redis.protocol.RespValue;


public class WaitCommand extends RedisCommand {
    int numReplicas;
    long timeoutMillis;

    private static ArgReader ARG_READER = new ArgReader(
        Type.WAIT.name(),
        new String[] { ":string", // command name
                ":int", // number of replicas to wait for
                ":int" // timeout in milliseconds
        });

    public WaitCommand() {
        super(Type.WAIT);
    }

    public WaitCommand(int numReplicas, long timeout) {
        super(Type.WAIT);
        this.numReplicas = numReplicas;
        this.timeoutMillis = timeout;
    }

    /**
     * @return the numReplicas
     */
    public int getNumReplicas() {
        return numReplicas;
    }

    /**
     * @return the timeoutMillis
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    @Override
    public boolean isBlockingCommand() {
        return true;
    }

    @Override
    public byte[] execute(RedisServiceBase service) {
        int count = service.waitForReplicationServers(numReplicas, timeoutMillis);
        return new RespInteger(count).asResponse();
    }

    @Override
    public void setArgs(RespValue[] args) {
        Map<String, RespValue> argsMap = ARG_READER.readArgs(args);
        numReplicas = argsMap.get("1").getValueAsLong().intValue();
        timeoutMillis = argsMap.get("2").getValueAsLong();
    }

    @Override
    public String toString() {
        return "WaitCommand [numReplicas=" + numReplicas + ", timeoutMillis=" + timeoutMillis + "]";
    }

}
