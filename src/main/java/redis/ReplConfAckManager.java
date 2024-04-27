package redis;

import java.io.IOException;
import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import redis.Commands.RedisCommand;
import redis.Commands.ReplConfCommand;

public final class ReplConfAckManager {
    private Map<Object, Set<ClientConnection>> waitFollowerSets = new ConcurrentHashMap<>();
    private Map<Object, Set<ClientConnection>> ackFollowerSets = new ConcurrentHashMap<>();
    private boolean testingDontWaitForAck = true;

    public static ReplConfAckManager INSTANCE = new ReplConfAckManager();

    private ReplConfAckManager() {
    }

    public void setTestingDontWaitForAck(boolean testingDontWaitForAck) {
        this.testingDontWaitForAck = testingDontWaitForAck;
    }

    public void notifyGotAckFromFollower(ClientConnection connection) {
        waitFollowerSets.forEach((lock, connectionSet) -> {
            if (connectionSet.contains(connection)) {
                synchronized (lock) {
                    ackFollowerSets.get(lock).add(connection);
                    lock.notifyAll();
                }
            }
        });
    }

    public int waitForAcksFromFollowerSet(int requestWaitFor, Set<ClientConnection> followerSet,
            Clock clock,
            long timeoutMillis) {
        int result = 0;

        Object lock = new Object();
        waitFollowerSets.put(lock, followerSet);
        Set<ClientConnection> ackSet = new HashSet<>();
        ackFollowerSets.put(lock, ackSet);
        synchronized (lock) {

            followerSet.forEach(this::sendCommand);

            long start = clock.millis();
            long now = start;

            int numToWaitFor = Math.min(requestWaitFor, followerSet.size());
            if (testingDontWaitForAck) {
                return followerSet.size();
            }
            try {
                while ((start + timeoutMillis - now > 0)
                        && ackSet.size() < numToWaitFor) {
                    // suspend this synchronized section and wait until the lock is notified that an
                    // ack has been received
                    lock.wait(start + timeoutMillis - now);
                    System.out.println(String.format(
                            "ReplConfAckManager: lock notified %d acks of %d requested",
                            ackSet.size(),
                            numToWaitFor));
                    now = clock.millis();
                    result = ackSet.size();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(String.format(
                        "ReplConfAckManager: exception while waiting for acks: %s %s",
                        followerSet,
                        this));
            } finally {
                waitFollowerSets.remove(lock);
                ackFollowerSets.remove(lock);
            }
        }
        return result;
    }

    private void sendCommand(ClientConnection connection) {
        ReplConfCommand ack = new ReplConfCommand(ReplConfCommand.Option.GETACK, "*");
        String ackString = new String(ack.asCommand()).toUpperCase();
        System.out.println(String.format("ReplConfAckManager: Sending command '%s' to client '%s'",
                RedisCommand.responseLogString(ackString.getBytes()), connection));
        try {
            connection.writeFlush(ackString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(String.format(
                    "ReplConfAckManager: exception while send replconf command to connection: %s %s",
                    connection,
                    this));
        }
    }

    @Override
    public String toString() {
        return "ReplConfAckManager [waitFollowerSets=" + waitFollowerSets + ", ackFollowerSets="
                + ackFollowerSets + "]";
    }

}
