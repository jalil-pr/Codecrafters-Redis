package redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import redis.Commands.RedisCommand;
import redis.protocol.RespValue;

public class WaitExecutor {
    private final int requestWaitFor;
    private final int numToWaitFor;
    private final AtomicInteger numAcknowledged;

    private final CountDownLatch latch;
    private final ExecutorService executorService;

    public WaitExecutor(int requestWaitFor, int numFollowers, ExecutorService executorService) {
        this.requestWaitFor = requestWaitFor;
        this.numToWaitFor = Math.min(requestWaitFor, numFollowers);
        this.executorService = executorService;
        this.numAcknowledged = new AtomicInteger(0);
        this.latch = new CountDownLatch(requestWaitFor);
    }

    int wait(Collection<ConnectionToFollower> followers, long timeoutMillis) {
        try {

            long extendedTimeout = timeoutMillis + 100L;

            List<Future<Void>> callFutures = new ArrayList<>();
            for (ConnectionToFollower follower : followers) {
                callFutures.add(executorService.submit(() -> {
                    requestAckFromFollower(follower, extendedTimeout);
                    return null;
                }));
            }

            long before = System.currentTimeMillis();
            System.out.println(String.format(
                    "Time %d: waiting u to %d millis for acks.", before, extendedTimeout));
            asyncSendRequest(() -> {
                try {
                    long waitUntil;
                    if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                        System.out.println(String.format(
                                "Timed out waiting for %d replConfAcks. Received %d acks.",
                                numToWaitFor, numAcknowledged.get()));
                        System.out.println(
                                Integer.toHexString(System.identityHashCode(numAcknowledged)));

                        System.out.println(String.format(
                                "Time %d: sleeping extend for up to %d millis for acks.",
                                System.currentTimeMillis(), extendedTimeout - timeoutMillis));

                        waitUntil = before + extendedTimeout;
                    } else {

                        waitUntil = before + timeoutMillis;
                    }

                    for (; (numAcknowledged.get() < followers.size()
                            && System.currentTimeMillis() < waitUntil);) {
                        System.out.println(String.format(
                                "Time %d: waiting until %d and have received %d replConfAcks.",
                                System.currentTimeMillis(), waitUntil, numAcknowledged.get()));
                        Thread.sleep(100L);
                    }
                } catch (InterruptedException e) {
                    System.out.println(String.format(
                            "Interrupted while waiting for %d replConfAcks. Received %d acks.",
                            requestWaitFor, numAcknowledged.get()));
                }
            }).get();
            long after = System.currentTimeMillis();
            System.out.println(String.format(
                    "Time %d: after extended task wait, elapsed time: %d.",
                    after, after - before));

            // cancel all pending tasks
            int countCancelled = 0;
            for (Future<Void> future : callFutures) {
                if (future.cancel(true)) {
                    countCancelled++;
                }
            }
            System.out.println(
                    String.format("Cancelled %d of %d tasks.", countCancelled, callFutures.size()));
        } catch (Exception e) {
            System.out
                    .println(String.format(
                            "Error while sending %d replConfAcks. Received %d acks.",
                            numToWaitFor, numAcknowledged.get()));
        }
        System.out.println(Integer.toHexString(System.identityHashCode(numAcknowledged)));
        System.out.println(String.format("Returning %d of %d requested acks.",
                numAcknowledged.get(), requestWaitFor));
        return numAcknowledged.get();
    }

    private void requestAckFromFollower(ConnectionToFollower connection, long extendedTimeout) {
        try {
            System.out.println(String.format("Sending replConfAck to %s", connection.toString()));
            System.out.println(String.format("Time %d: before send on %s",
                    System.currentTimeMillis(), connection));
            // if no response received within timeout, then it returns null
            RespValue ackResponse = connection.sendAndWaitForReplConfAck(extendedTimeout);
            if (ackResponse == null) {
                System.out.println(
                        String.format(
                                "Time %d: after send on %s, Timed out waiting, no response. Still waiting for %d replConfAcks.",
                                System.currentTimeMillis(), connection,
                                numToWaitFor - numAcknowledged.get()));
            } else {
                System.out.println(String.format("Time %d: after send on %s, response: %s",
                        System.currentTimeMillis(), connection,
                        RedisCommand.responseLogString(ackResponse.asResponse())));
                System.out.println(Integer.toHexString(System.identityHashCode(numAcknowledged)));
                int prevAck = numAcknowledged.getAndIncrement();
                latch.countDown();
                System.out.println(String.format("Received replConfAck %d (prev %d) from %s",
                        numAcknowledged.get(), prevAck, connection.toString()));
            }
        } catch (Exception e) {
            System.out.println(
                    String.format(
                            "Error sending replConfAck to %s, error: %s %s, cause: %s",
                            connection.toString(), e.getClass().getSimpleName(), e.getMessage(),
                            e.getCause()));
            System.out.println(String.format(
                    "Not counted. Still waiting for %d replConfAcks.",
                    numToWaitFor - numAcknowledged.get()));
        }
    }

    private Future<?> asyncSendRequest(Runnable runnable) {
        return executorService.submit(runnable);
    }

    @Override
    public String toString() {
        return "WaitExecutor [numToWaitFor=" + numToWaitFor + ", numAcknowledged="
                + numAcknowledged.get() + "]";
    }
}
