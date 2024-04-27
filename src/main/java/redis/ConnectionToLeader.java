package redis;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import redis.Commands.PingCommand;
import redis.Commands.PsyncCommand;
import redis.Commands.RedisCommand;
import redis.Commands.ReplConfCommand;
import redis.protocol.RespBulkString;
import redis.protocol.RespValue;
import redis.protocol.RespValueParser;

public class ConnectionToLeader {
    // keep a list of socket connections and continue checking for new connections
    private final FollowerService service;
    private final ClientConnection leaderConnection;
    private final Deque<CommandAndResponseConsumer> commandsToLeader = new ConcurrentLinkedDeque<>();
    private final ExecutorService executor;
    private final RespValueParser valueParser;
    private volatile boolean done = false;
    private long handshakeBytesReceived = 0;
    private RespBulkString fullResyncRdb;

    public ConnectionToLeader(FollowerService service) throws IOException {
        this.service = service;
        executor = Executors.newFixedThreadPool(1); // We need just one thread for sending commands
                                                    // to the leader
        valueParser = new RespValueParser();

        leaderConnection = new ClientConnection(service.getLeaderClientSocket(), valueParser);
        System.out.println(String.format("Connection to leader: %s, isOpened: %s", leaderConnection,
                !leaderConnection.isClosed()));

        // create the thread for sending handshake commands to the leader
        executor.execute(() -> {
            try {
                runHandshakeLoop();
            } catch (InterruptedException e) {
                System.out
                        .println("InterruptedException on send command thread: " + e.getMessage());
                terminate();
            }
        });
    }

    public long getHandshakeBytesReceived() {
        return handshakeBytesReceived;
    }

    public void startHandshake() {
        System.out.println(String.format("Starting handshake with leader"));
        sendCommand(new PingCommand(), (cmd, response) -> {
            ReplConfCommand conf1 = new ReplConfCommand(ReplConfCommand.Option.LISTENING_PORT,
                    String.valueOf(service.getPort()));
            sendCommand(conf1, (conf1Cmd, response2) -> {
                ReplConfCommand conf2 = new ReplConfCommand(ReplConfCommand.Option.CAPA, "psync2");
                sendCommand(conf2, (conf2Cmd, response3) -> {
                    PsyncCommand psync = new PsyncCommand("?", Long.valueOf(-1L));
                    sendCommand(psync, (psyncCmd, response4) -> {
                        if (response4.isSimpleString() && response4.getValueAsString().toUpperCase()
                                .startsWith("FULLRESYNC")) {
                            System.out.println(
                                    String.format("Full resync - looking for rdb response"));
                            return true;
                        }
                        if (!response4.isBulkString()) {
                            throw new RuntimeException(
                                    String.format("Unexpected response: %s", response4));
                        }
                        setFullResyncRdb((RespBulkString) response4);
                        System.out.println(String.format("Handshake completed"));
                        handshakeBytesReceived = leaderConnection.getNumBytesReceived();

                        // after the handshake, allow the ConnectionManager to poll for commands
                        // from the leader and process them in the FollowerService on the main event
                        // loop
                        service.getConnectionManager().addPriorityConnection(leaderConnection);
                        return false;
                    });
                    return false;
                });
                return false;
            });
            return false;
        });
    }

    private void setFullResyncRdb(RespBulkString fullResyncRdb) {
        this.fullResyncRdb = fullResyncRdb;
    }

    public byte[] getFullResyncRdb() {
        return fullResyncRdb.getValue();
    }

    private void sendCommand(RedisCommand command,
            BiFunction<RedisCommand, RespValue, Boolean> responseConsumer) {
        CommandAndResponseConsumer cmd = new CommandAndResponseConsumer(command, responseConsumer);
        // add the command to the queue
        commandsToLeader.offerLast(cmd);
    }

    public void terminate() {
        System.out.println(String.format("Terminate follower invoked. Closing socket to leader %s.",
                service.getLeaderClientSocket()));
        done = true;

        try {
            service.getLeaderClientSocket().close();
        } catch (IOException e) {
            System.out.println("IOException on socket close: " + e.getMessage());
        }

        executor.close();
    }

    public void runHandshakeLoop() throws InterruptedException {
        while (!done) {
            if (leaderConnection.isClosed()) {
                System.out.println(String.format(
                        "Terminating service due to connection is closed by leader during handshake: %s",
                        leaderConnection));
                terminate();
                continue;
            }

            try {
                while (!commandsToLeader.isEmpty()) {
                    CommandAndResponseConsumer cmd = commandsToLeader.pollFirst();

                    System.out.println(String.format("Sending leader command: %s", cmd.command));
                    leaderConnection.writeFlush(cmd.command.asCommand());

                    RespValue response = leaderConnection.readValue();
                    System.out.println(String.format("Received leader response: %s", response));

                    if (cmd.responseConsumer.apply(cmd.command, response)) {
                        try {
                            byte[] rdb = leaderConnection.readRDB();

                            response = new RespBulkString(rdb);
                            System.out.println(String.format("Received leader RDB: %s", response));
                            cmd.responseConsumer.apply(cmd.command, response);
                        } catch (IOException e) {
                            System.out.println(String.format(
                                    "ConnectionToLeader: IOException on readRDB: %s %s",
                                    e.getClass().getSimpleName(), e.getMessage()));
                        }
                    }
                }
                // sleep a bit before the next handshake command
                Thread.sleep(50L);
            } catch (Exception e) {
                System.out.println(String.format("ConnectionToLeader Loop Exception: %s \"%s\"",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }
        System.out.println(String.format(
                "Exiting thread for handshake commands - done: %s", done));
    }

    public boolean isLeaderConnection(ClientConnection conn) {
        return leaderConnection.equals(conn);
    }

    public void executeCommandFromLeader(ClientConnection conn, RedisCommand command)
            throws IOException {
        if (!isLeaderConnection(conn)) {
            System.out.println(String.format(
                    "ConnectionToLeader ERROR: executeCommandFromLeader called with non-leader connection: %s",
                    conn));
            return;
        }

        if (command.isReplicatedCommand()) {
            System.out.println(
                    String.format("Received replicated command from leader: %s", conn));
        } else {
            System.out.println(String.format("Received request from leader: %s", conn));
        }

        boolean writeResponse = shouldSendResponseToConnection(command, conn);

        byte[] response = command.execute(service);
        if (writeResponse) {
            System.out.println(String.format("Follower service sending %s response: %s",
                    command.getType().name(), RedisCommand.responseLogString(response)));
            if (response != null && response.length > 0) {
                conn.writeFlush(response);
            }
        } else {
            System.out.println(String.format("Follower service do not send %s response: %s",
                    command.getType().name(), RedisCommand.responseLogString(response)));
        }
    }

    public boolean shouldSendResponseToConnection(RedisCommand command, ClientConnection conn) {
        if (!leaderConnection.equals(conn)) {
            return true;
        }
        boolean isReplconfGetack = command instanceof ReplConfCommand && ((ReplConfCommand) command)
                .getOptionsMap().containsKey(ReplConfCommand.GETACK_NAME);
        return isReplconfGetack;
    }

    private static class CommandAndResponseConsumer {
        private final RedisCommand command;
        private final BiFunction<RedisCommand, RespValue, Boolean> responseConsumer;

        public CommandAndResponseConsumer(RedisCommand command,
                BiFunction<RedisCommand, RespValue, Boolean> responseConsumer) {
            this.command = command;
            this.responseConsumer = responseConsumer;
        }
    }
}
