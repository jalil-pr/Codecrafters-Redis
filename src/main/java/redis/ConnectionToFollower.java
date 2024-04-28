package redis;

import java.io.IOException;

import redis.Commands.ReplConfCommand;
import redis.protocol.RespSimpleStringValue;
import redis.protocol.RespValue;

public class ConnectionToFollower {
    private final LeaderService service;
    private final ClientConnection followerConnection;

    private volatile boolean testingDontWaitForAck = true;

    public ConnectionToFollower(LeaderService service, ClientConnection followerConnection)
            throws IOException {
        this.service = service;
        this.followerConnection = followerConnection;
    }

    public long getTotalReplicationOffset() {
        return service.getTotalReplicationOffset();
    }

    public ClientConnection getFollowerConnection() {
        return followerConnection;
    }

    /**
     * Caller can set this when it wants to start waiting for a ACK response. For
     * codecrafters
     * integration test, this means tests that first have replicated commands.
     * 
     * @param testingDontWaitForAck
     */
    public void setTestingDontWaitForAck(boolean testingDontWaitForAck) {
        this.testingDontWaitForAck = testingDontWaitForAck;
    }

    public RespValue sendAndWaitForReplConfAck(long timeoutMillis) throws IOException, InterruptedException {
        ReplConfCommand ack = new ReplConfCommand(ReplConfCommand.Option.GETACK, "*");
        String ackString = new String(ack.asCommand()).toUpperCase();
        System.out.println(String.format("sendAndWaitForReplConfAck: Sending command %s",
                ackString.replace("\r\n", "\\r\\n")));
        followerConnection.writeFlush(ackString.getBytes());

        if (testingDontWaitForAck) {
            String response = "REPLCONF ACK 0";
            System.out.println(String.format(
                    "sendAndWaitForReplConfAck: not waiting, harcoded response: \"%s\"", response));
            return new RespSimpleStringValue(response);
        } else {
            System.out.println("sendAndWaitForReplConfAck: waiting for REPLCONF ACK");
            followerConnection.waitForNewValueAvailable(timeoutMillis);
            RespValue response = service.getConnectionManager().getNextValue(followerConnection);
            System.out.println(String.format("sendAndWaitForReplConfAck: got response from replica: %s",
                    response));
            return response;
        }
    }

    @Override
    public String toString() {
        return "ConnectionToFollower: " + followerConnection;
    }

}
